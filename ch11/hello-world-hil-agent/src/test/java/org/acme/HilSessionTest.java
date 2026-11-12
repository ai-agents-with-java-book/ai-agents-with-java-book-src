package org.acme;

import java.io.Reader;
import java.io.EOFException;
import java.io.StringReader;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.a2aproject.sdk.A2A;
import org.a2aproject.sdk.client.MessageEvent;
import org.a2aproject.sdk.client.TaskEvent;
import org.a2aproject.sdk.client.TaskUpdateEvent;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(3)
class HilSessionTest {
    private static final Duration WAIT = Duration.ofMillis(100);

    private static Task task(TaskState state) {
        return Task.builder().id("task").contextId("context")
                .status(new TaskStatus(state)).build();
    }

    @Test
    void capturesBothIdsAndSendsAtMostOneReply() throws Exception {
        var session = new HilSession();
        Task task = task(TaskState.TASK_STATE_INPUT_REQUIRED);
        session.accept(new TaskUpdateEvent(task, TaskStatusUpdateEvent.builder()
                .taskId(task.id()).contextId(task.contextId()).status(task.status()).build()), null);
        assertEquals(new HilSession.Prompt("task", "context"), session.awaitPrompt(WAIT));
        assertEquals("upper", session.readChoice(new StringReader("invalid\nupper\n")));
        assertTrue(session.claimReply());
        session.accept(new TaskEvent(task), null);
        assertFalse(session.claimReply());
        session.accept(new TaskEvent(task(TaskState.TASK_STATE_COMPLETED)), null);
        session.awaitCompletion(WAIT);
        session.accept(new TaskEvent(task), null);
        assertFalse(session.claimReply());
    }

    @Test
    void earlyTaskOrDirectMessageCompletionNeedsNoHumanReply() throws Exception {
        for (var event : new org.a2aproject.sdk.client.ClientEvent[] {
                new TaskEvent(task(TaskState.TASK_STATE_COMPLETED)),
                new MessageEvent(A2A.toAgentMessage("done")) }) {
            var session = new HilSession();
            session.accept(event, null);
            assertNull(session.awaitPrompt(WAIT));
            session.awaitCompletion(WAIT);
            assertFalse(session.claimReply());
        }
    }

    @Test
    void failedAndCanceledTasksReleaseBothWaits() {
        for (TaskState state : new TaskState[] {
                TaskState.TASK_STATE_FAILED, TaskState.TASK_STATE_CANCELED,
                TaskState.TASK_STATE_REJECTED }) {
            var session = new HilSession();
            session.accept(new TaskEvent(task(state)), null);
            assertThrows(IllegalStateException.class, () -> session.awaitPrompt(WAIT));
            assertThrows(IllegalStateException.class, () -> session.awaitCompletion(WAIT));
            assertFalse(session.claimReply());
        }
    }

    @Test
    void streamFailureBeforeInputAndWhileReadingReleasesTheCaller() throws Exception {
        var before = new HilSession();
        before.fail(new IllegalStateException("connection failed"));
        assertThrows(IllegalStateException.class, () -> before.awaitPrompt(WAIT));
        var session = new HilSession();
        session.accept(new TaskEvent(task(TaskState.TASK_STATE_INPUT_REQUIRED)), null);
        var reading = new CountDownLatch(1);
        var released = new CompletableFuture<Throwable>();
        Reader blockedInput = new Reader() {
            public int read(char[] chars, int offset, int length) {
                reading.countDown();
                try { new CountDownLatch(1).await(); }
                catch (InterruptedException expected) { Thread.currentThread().interrupt(); }
                return -1;
            }
            public void close() {}
        };
        Thread.ofVirtual().start(() -> {
            try { session.readChoice(blockedInput); released.complete(null); }
            catch (Exception failure) { released.complete(failure); }
        });
        assertTrue(reading.await(1, TimeUnit.SECONDS));
        session.fail(new IllegalStateException("stream lost"));
        assertInstanceOf(IllegalStateException.class, released.get(1, TimeUnit.SECONDS));
        assertFalse(session.claimReply());
        assertThrows(IllegalStateException.class, () -> session.awaitCompletion(WAIT));
    }

    @Test
    void terminalOutcomeAfterPromptPreventsReply() throws Exception {
        for (TaskState state : new TaskState[] {
                TaskState.TASK_STATE_COMPLETED, TaskState.TASK_STATE_FAILED,
                TaskState.TASK_STATE_CANCELED }) {
            var session = new HilSession();
            session.accept(new TaskEvent(task(TaskState.TASK_STATE_INPUT_REQUIRED)), null);
            session.awaitPrompt(WAIT);
            session.accept(new TaskEvent(task(state)), null);
            assertFalse(session.claimReply());
        }
    }

    @Test
    void bothNetworkWaitsHaveDeadlines() throws Exception {
        var initial = new HilSession();
        assertThrows(TimeoutException.class, () -> initial.awaitPrompt(Duration.ofMillis(1)));
        var finalWait = new HilSession();
        finalWait.accept(new TaskEvent(task(TaskState.TASK_STATE_INPUT_REQUIRED)), null);
        finalWait.awaitPrompt(WAIT);
        assertTrue(finalWait.claimReply());
        assertThrows(TimeoutException.class, () -> finalWait.awaitCompletion(Duration.ofMillis(1)));
        assertFalse(finalWait.claimReply());
    }

    @Test
    void interruptionRestoresTheFlag() {
        var session = new HilSession();
        Thread.currentThread().interrupt();
        try {
            assertThrows(InterruptedException.class, () -> session.awaitPrompt(WAIT));
            assertTrue(Thread.currentThread().isInterrupted());
            assertFalse(session.claimReply());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void endOfInputStopsWithoutRetry() throws Exception {
        var session = new HilSession();
        session.accept(new TaskEvent(task(TaskState.TASK_STATE_INPUT_REQUIRED)), null);
        assertNull(session.readChoice(new StringReader("")));
        assertFalse(session.claimReply());
        var truncatedStream = new HilSession();
        truncatedStream.accept(new TaskEvent(task(TaskState.TASK_STATE_INPUT_REQUIRED)), null);
        truncatedStream.fail(new EOFException("Truncated stream"));
        assertThrows(EOFException.class, () -> truncatedStream.readChoice(new StringReader("")));
    }
}
