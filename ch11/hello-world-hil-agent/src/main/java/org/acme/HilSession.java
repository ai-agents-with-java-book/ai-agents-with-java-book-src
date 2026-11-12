package org.acme;

import java.io.BufferedReader;
import java.io.Reader;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import org.a2aproject.sdk.client.ClientEvent;
import org.a2aproject.sdk.client.MessageEvent;
import org.a2aproject.sdk.client.TaskEvent;
import org.a2aproject.sdk.client.TaskUpdateEvent;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TextPart;

/** Coordinates one human reply with the stream's terminal outcome. */
public final class HilSession implements BiConsumer<ClientEvent, AgentCard> {
    public record Prompt(String taskId, String contextId) {}

    private final CompletableFuture<Prompt> input = new CompletableFuture<>();
    private final CompletableFuture<Void> completion = new CompletableFuture<>();
    private boolean terminal;
    private boolean replyClaimed;

    @Override
    public synchronized void accept(ClientEvent event, AgentCard card) {
        if (terminal) return;
        switch (event) {
            case MessageEvent message -> {
                print(message.getMessage().parts());
                finish(null);
            }
            case TaskEvent task -> observe(task.getTask());
            case TaskUpdateEvent update -> observe(update.getTask());
        }
    }

    private void observe(org.a2aproject.sdk.spec.Task task) {
        if (task == null || task.status() == null || task.status().state() == null) {
            fail(new IllegalStateException("Missing task status"));
            return;
        }
        TaskStatus status = task.status();
        if (status.message() != null) print(status.message().parts());
        if (status.state() == TaskState.TASK_STATE_INPUT_REQUIRED) {
            if (task.id() == null || task.id().isBlank()
                    || task.contextId() == null || task.contextId().isBlank()) {
                fail(new IllegalStateException("Missing continuation IDs"));
            } else if (!replyClaimed) {
                input.complete(new Prompt(task.id(), task.contextId()));
            }
        } else if (status.state().isFinal()) {
            if (status.state() == TaskState.TASK_STATE_COMPLETED) {
                if (task.artifacts() != null) {
                    task.artifacts().forEach(artifact -> print(artifact.parts()));
                }
                finish(null);
            } else {
                fail(new IllegalStateException("Task ended: " + status.state()));
            }
        }
    }

    public synchronized void fail(Throwable failure) {
        finish(failure == null ? new IllegalStateException("Stream failed") : failure);
    }

    private void finish(Throwable failure) {
        if (terminal) return;
        terminal = true;
        if (failure == null) {
            completion.complete(null);
            input.complete(null);
        } else {
            completion.completeExceptionally(failure);
            input.completeExceptionally(failure);
        }
    }

    public Prompt awaitPrompt(Duration timeout) throws Exception {
        return await(input, timeout);
    }

    /** A terminal stream outcome also releases a pending keyboard wait. */
    public String readChoice(Reader reader) throws Exception {
        CompletableFuture<String> answer = new CompletableFuture<>();
        Thread keyboard = Thread.ofVirtual().start(() -> {
            try {
                BufferedReader lines = new BufferedReader(reader);
                while (!completion.isDone()) {
                    System.out.print("Enter your choice ('upper' or 'lower'): ");
                    String line = lines.readLine();
                    if (line == null) {
                        synchronized (HilSession.this) { finish(null); }
                        return;
                    }
                    String choice = line.trim();
                    if (choice.equalsIgnoreCase("upper") || choice.equalsIgnoreCase("lower")) {
                        answer.complete(choice);
                        return;
                    }
                }
            } catch (Exception failure) {
                answer.completeExceptionally(failure);
            }
        });
        try {
            return (String) await(CompletableFuture.anyOf(answer, completion), null);
        } finally {
            // Leave System.in open. A blocked console read is a daemon task.
            keyboard.interrupt();
        }
    }

    /** Linearizes one reply against terminal callbacks already received. */
    public synchronized boolean claimReply() {
        if (terminal || replyClaimed || !input.isDone()) return false;
        replyClaimed = true;
        return true;
    }

    public void awaitCompletion(Duration timeout) throws Exception {
        await(completion, timeout);
    }

    private <T> T await(CompletableFuture<T> future, Duration timeout) throws Exception {
        try {
            return timeout == null ? future.get()
                    : future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException failure) {
            fail(failure);
            Thread.currentThread().interrupt();
            throw failure;
        } catch (TimeoutException failure) {
            fail(failure);
            throw failure;
        } catch (ExecutionException failure) {
            fail(failure.getCause());
            if (failure.getCause() instanceof Exception cause) throw cause;
            throw failure;
        }
    }

    private static void print(List<Part<?>> parts) {
        if (parts != null) {
            parts.stream().filter(TextPart.class::isInstance)
                    .map(TextPart.class::cast).map(TextPart::text).forEach(System.out::println);
        }
    }
}
