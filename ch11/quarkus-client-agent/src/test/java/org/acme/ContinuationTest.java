package org.acme;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import dev.langchain4j.agentic.a2a.A2ATaskInterruptedException;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.a2aproject.sdk.spec.TaskState;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContinuationTest {
    private static PendingReplies store() {
        return new PendingReplies(Duration.ofMinutes(10), Clock.systemUTC());
    }

    private static GreetingResource resource(PendingReplies store, HelloWorldAgentHil agent, String subject) {
        var resource = new GreetingResource();
        JsonWebToken token = (JsonWebToken) Proxy.newProxyInstance(JsonWebToken.class.getClassLoader(),
                new Class<?>[] { JsonWebToken.class }, (proxy, method, args) -> switch (method.getName()) {
                    case "getName", "getSubject" -> subject;
                    case "getIssuer" -> "https://issuer.example.test";
                    default -> null;
                });
        resource.identity = QuarkusSecurityIdentity.builder().setPrincipal(token).build();
        resource.pendingReplies = store;
        resource.helloWorldAgentHil = agent;
        return resource;
    }

    @Test
    void interleavedConversationsRetainBothIdsAndTheirOwner() {
        var store = store();
        var sequence = new AtomicInteger();
        HelloWorldAgentHil agent = (context, task, text) -> {
            if (task == null) {
                int id = sequence.incrementAndGet();
                throw new A2ATaskInterruptedException("task" + id, "context" + id,
                        TaskState.TASK_STATE_INPUT_REQUIRED, "Choose a case");
            }
            return context + ":" + task + ":" + text;
        };
        var alice = resource(store, agent, "alice");
        var bob = resource(store, agent, "bob");
        var first = alice.start(new GreetingResource.Input("first"));
        var second = bob.start(new GreetingResource.Input("second"));
        assertNotEquals(first.handle(), second.handle());
        assertEquals(400, assertThrows(WebApplicationException.class,
                () -> alice.resume(first.handle(), new GreetingResource.Input("invalid"))).getResponse().getStatus());
        assertThrows(NotFoundException.class,
                () -> bob.resume(first.handle(), new GreetingResource.Input("upper")));
        assertEquals("context2:task2:lower",
                bob.resume(second.handle(), new GreetingResource.Input("lower")).result());
        assertEquals("context1:task1:upper",
                alice.resume(first.handle(), new GreetingResource.Input("upper")).result());
        assertEquals(409, assertThrows(WebApplicationException.class,
                () -> alice.resume(first.handle(), new GreetingResource.Input("upper"))).getResponse().getStatus());
    }

    @Test
    void onlyOneConcurrentClaimSucceeds() throws Exception {
        var store = store();
        String handle = store.remember("owner", "context", "task");
        Callable<Integer> attempt = () -> {
            try { store.claim(handle, "owner"); return 200; }
            catch (WebApplicationException rejected) { return rejected.getResponse().getStatus(); }
        };
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var results = executor.invokeAll(List.of(attempt, attempt));
            assertEquals(List.of(200, 409), results.stream().map(f -> {
                try { return f.get(); } catch (Exception failure) { throw new AssertionError(failure); }
            }).sorted().toList());
        }
    }

    @Test
    void missingForeignAndExpiredHandlesAreRejected() {
        var now = new AtomicReference<>(Instant.parse("2026-11-12T12:00:00Z"));
        Clock clock = new Clock() {
            public ZoneId getZone() { return ZoneId.of("UTC"); }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return now.get(); }
        };
        var store = new PendingReplies(Duration.ofSeconds(5), clock);
        assertThrows(NotFoundException.class, () -> store.claim("missing", "alice"));
        String handle = store.remember("alice", "context", "task");
        assertThrows(NotFoundException.class, () -> store.claim(handle, "bob"));
        assertThrows(NotFoundException.class, () -> store.claim(handle, null));
        now.set(now.get().plusSeconds(5));
        assertEquals(410, assertThrows(WebApplicationException.class,
                () -> store.claim(handle, "alice")).getResponse().getStatus());
    }

    @Test
    void anotherInputRequestGetsANewHandleForTheSameOwner() {
        var store = store();
        var calls = new AtomicInteger();
        var resource = resource(store, (context, task, text) -> {
            if (calls.incrementAndGet() < 3) {
                throw new A2ATaskInterruptedException("task", "context",
                        TaskState.TASK_STATE_INPUT_REQUIRED, "Choose again");
            }
            return "done";
        }, "alice");
        var first = resource.start(new GreetingResource.Input("hello"));
        var second = resource.resume(first.handle(), new GreetingResource.Input("upper"));
        assertNotEquals(first.handle(), second.handle());
        assertThrows(NotFoundException.class, () -> store.claim(second.handle(), "bob"));
        assertEquals("done", resource.resume(second.handle(), new GreetingResource.Input("lower")).result());
    }

    @Test
    void uncertainOutcomeNeverReopensTheHandle() {
        var store = store();
        var calls = new AtomicInteger();
        var resource = resource(store, (context, task, text) -> {
            calls.incrementAndGet();
            throw new IllegalStateException("connection lost after sending");
        }, "alice");
        String handle = store.remember("https://issuer.example.test\nalice", "context", "task");
        assertEquals(502, assertThrows(WebApplicationException.class,
                () -> resource.resume(handle, new GreetingResource.Input("upper"))).getResponse().getStatus());
        assertEquals(409, assertThrows(WebApplicationException.class,
                () -> resource.resume(handle, new GreetingResource.Input("upper"))).getResponse().getStatus());
        assertEquals(1, calls.get());
    }

    @Test
    void blankSubjectCannotInvokeTheRemoteAgent() {
        var resource = resource(store(), (context, task, text) -> {
            fail("Remote agent must not be invoked"); return null;
        }, " ");
        assertEquals(403, assertThrows(WebApplicationException.class,
                () -> resource.start(new GreetingResource.Input("hello"))).getResponse().getStatus());
    }
}
