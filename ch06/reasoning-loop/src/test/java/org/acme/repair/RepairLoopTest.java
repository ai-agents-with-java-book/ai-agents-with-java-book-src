package org.acme.repair;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.acme.repair.Repair.*;
import static org.junit.jupiter.api.Assertions.*;

class RepairLoopTest {
    private final Limits limits = new Limits(4, 2, Duration.ofMinutes(1));

    private static Observation rejected(String... passed) {
        return new Observation(Check.REJECTED, Set.of(passed), "Still failing", "test");
    }

    private static Proposal replace(String source) {
        return new Proposal(Action.REPLACE_SOURCE, List.of("Fix the failure"), source, "Revise");
    }

    @Test void verifiesBeforeCallingThePlanner() {
        var passed = new Observation(Check.PASSED, Set.of("all"), "", "test");
        var result = new RepairLoop(s -> { fail("Planner must not run"); return null; },
                (source, remaining) -> passed, limits).run("goal", "initial");
        assertEquals(Status.COMPLETED, result.status());
        assertEquals(0, result.modelCalls());
    }

    @Test void replansUsingTheLatestEvidence() {
        var checks = new ArrayDeque<>(List.of(rejected(), rejected("sum"),
                new Observation(Check.PASSED, Set.of("sum", "average"), "", "test")));
        Planner planner = snapshot -> {
            if (snapshot.history().isEmpty()) return replace("sum fixed");
            assertEquals("sum fixed", snapshot.source());
            assertEquals(Set.of("sum"), snapshot.observation().passed());
            assertEquals(1, snapshot.history().size());
            return replace("all fixed");
        };
        var result = new RepairLoop(planner, (s, d) -> checks.remove(), limits)
                .run("goal", "initial");
        assertEquals(Status.COMPLETED, result.status());
        assertEquals(2, result.modelCalls());
        assertEquals("all fixed", result.bestSource());
    }

    @Test void aClaimOfCompletionCannotOverrideFailedTests() {
        Planner planner = s -> new Proposal(Action.STOP, List.of(), "", "Everything is fixed");
        var result = new RepairLoop(planner, (s, d) -> rejected(), limits).run("goal", "initial");
        assertEquals(Status.BLOCKED, result.status());
        assertEquals(Check.REJECTED, result.bestObservation().check());
    }

    @Test void rejectsMalformedProposalBeforeExecutingIt() {
        var executions = new AtomicInteger();
        Planner planner = s -> new Proposal(null, List.of(), "bad", "bad");
        var result = new RepairLoop(planner, (s, d) -> {
            executions.incrementAndGet(); return rejected();
        }, limits).run("goal", "initial");
        assertEquals(Status.FAILED, result.status());
        assertEquals(1, executions.get());
    }

    @Test void repeatedSourceDoesNotRunAgain() {
        var executions = new AtomicInteger();
        var proposals = new ArrayDeque<>(List.of(replace("changed"), replace("initial")));
        var result = new RepairLoop(s -> proposals.remove(), (s, d) -> {
            executions.incrementAndGet(); return rejected();
        }, limits).run("goal", "initial");
        assertEquals(Status.STALLED, result.status());
        assertEquals(2, executions.get());
    }

    @Test void keepsTheBestCandidateWhenLaterAttemptsRegress() {
        var checks = new ArrayDeque<>(List.of(rejected(), rejected("sum", "max"),
                rejected("sum"), rejected("average")));
        var proposals = new ArrayDeque<>(List.of(replace("best"), replace("worse"), replace("also worse")));
        var result = new RepairLoop(s -> proposals.remove(), (s, d) -> checks.remove(), limits)
                .run("goal", "initial");
        assertEquals(Status.STALLED, result.status());
        assertEquals("best", result.bestSource());
        assertEquals("also worse", result.latest().source());
    }

    @Test void enforcesTheModelCallAllowance() {
        var executions = new AtomicInteger();
        var oneCall = new Limits(1, 3, Duration.ofMinutes(1));
        var result = new RepairLoop(s -> replace("revision"), (s, d) -> {
            executions.incrementAndGet(); return rejected();
        }, oneCall).run("goal", "initial");
        assertEquals(Status.BUDGET_EXHAUSTED, result.status());
        assertEquals(1, result.modelCalls());
        assertEquals(2, executions.get());
    }

    @Test void discardsAProposalReturnedAfterTheDeadline() {
        var time = new AtomicLong();
        var executions = new AtomicInteger();
        Planner slow = s -> {
            time.set(Duration.ofMinutes(2).toNanos()); return replace("too late");
        };
        var result = new RepairLoop(slow, (s, d) -> {
            executions.incrementAndGet(); return rejected();
        }, limits, time::get).run("goal", "initial");
        assertEquals(Status.BUDGET_EXHAUSTED, result.status());
        assertEquals(1, executions.get());
    }

    @Test void unavailableVerificationStopsFurtherPlanning() {
        var checks = new ArrayDeque<>(List.of(rejected(),
                new Observation(Check.UNAVAILABLE, Set.of(), "Maven unavailable", "test")));
        var result = new RepairLoop(s -> replace("revision"), (s, d) -> checks.remove(), limits)
                .run("goal", "initial");
        assertEquals(Status.FAILED, result.status());
        assertEquals(1, result.modelCalls());
    }

    @Test void providerFailureCountsAsAnInvocation() {
        var result = new RepairLoop(s -> { throw new IllegalStateException("Provider unavailable"); },
                (s, d) -> rejected(), limits).run("goal", "initial");
        assertEquals(Status.FAILED, result.status());
        assertEquals(1, result.modelCalls());
    }

    @Test void failedVerificationKeepsSourceAndObservationTogether() {
        var calls = new AtomicInteger();
        var result = new RepairLoop(s -> replace("untested"), (s, d) -> {
            if (calls.incrementAndGet() == 2) throw new IllegalStateException("Cannot run");
            return rejected("sum");
        }, limits).run("goal", "initial");
        assertEquals(Status.FAILED, result.status());
        assertEquals("initial", result.latest().source());
        assertEquals(Set.of("sum"), result.latest().observation().passed());
    }
}
