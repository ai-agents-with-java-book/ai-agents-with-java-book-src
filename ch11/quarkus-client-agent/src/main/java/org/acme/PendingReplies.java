package org.acme;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Single-process handles. A claimed handle is never made pending again. */
@ApplicationScoped
public class PendingReplies {
    enum State { PENDING, IN_FLIGHT, CONSUMED, UNCERTAIN }

    static final class Pending {
        final String owner;
        final String contextId;
        final String taskId;
        final Instant expiresAt;
        final AtomicReference<State> state = new AtomicReference<>(State.PENDING);

        Pending(String owner, String contextId, String taskId, Instant expiresAt) {
            this.owner = owner;
            this.contextId = contextId;
            this.taskId = taskId;
            this.expiresAt = expiresAt;
        }
    }

    private final ConcurrentHashMap<String, Pending> replies = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final Clock clock;

    @Inject
    PendingReplies(@ConfigProperty(name = "hil.continuation-ttl", defaultValue = "PT10M") Duration ttl) {
        this(ttl, Clock.systemUTC());
    }

    PendingReplies(Duration ttl, Clock clock) {
        if (ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException("Lifetime must be positive");
        this.ttl = ttl;
        this.clock = clock;
    }

    String remember(String owner, String contextId, String taskId) {
        if (owner == null || owner.isBlank() || contextId == null || contextId.isBlank()
                || taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("Owner and both continuation IDs are required");
        }
        Instant now = clock.instant();
        replies.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt));
        String handle = UUID.randomUUID().toString();
        replies.put(handle, new Pending(owner, contextId, taskId, now.plus(ttl)));
        return handle;
    }

    Pending claim(String handle, String owner) {
        Pending pending = replies.get(handle);
        if (pending == null || owner == null || !owner.equals(pending.owner)) {
            throw new NotFoundException();
        }
        if (!clock.instant().isBefore(pending.expiresAt)) {
            replies.remove(handle, pending);
            throw new WebApplicationException("Continuation expired", 410);
        }
        if (!pending.state.compareAndSet(State.PENDING, State.IN_FLIGHT)) {
            throw new WebApplicationException("Reply already submitted", 409);
        }
        return pending;
    }
}
