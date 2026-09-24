package org.acme.repair;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongSupplier;
import static org.acme.repair.Repair.*;

public final class RepairLoop {
    private final Planner planner;
    private final Verifier verifier;
    private final Limits limits;
    private final LongSupplier nanoTime;

    public RepairLoop(Planner planner, Verifier verifier, Limits limits) {
        this(planner, verifier, limits, System::nanoTime);
    }

    public RepairLoop(Planner planner, Verifier verifier, Limits limits,
                      LongSupplier nanoTime) {
        this.planner = planner;
        this.verifier = verifier;
        this.limits = limits;
        this.nanoTime = nanoTime;
    }

    public Result run(String goal, String initialSource) {
        long started = nanoTime.getAsLong();
        String source = initialSource;
        String bestSource = source;
        Observation observed = new Observation(Check.UNAVAILABLE, Set.of(),
                "Verification has not run", "");
        Observation best = observed;
        List<Attempt> history = new ArrayList<>();
        Set<String> seenSources = new HashSet<>(Set.of(source));
        int calls = 0;
        int nonImproving = 0;
        try {
            observed = verifier.verify(source, remaining(started));
            best = observed;
            while (true) {
                var snapshot = new Snapshot(goal, source, observed, history);
                if (observed.check() == Check.PASSED) {
                    return result(Status.COMPLETED, "Acceptance checks passed",
                            calls, source, observed, snapshot);
                }
                if (observed.check() == Check.UNAVAILABLE) {
                    return result(Status.FAILED, "No reliable verification result",
                            calls, bestSource, best, snapshot);
                }
                if (expired(started) || calls >= limits.modelCalls()) {
                    return result(Status.BUDGET_EXHAUSTED, "Execution budget exhausted",
                            calls, bestSource, best, snapshot);
                }
                calls++; // Failed invocations consume the allowance too.
                Proposal proposal = planner.propose(snapshot);
                if (expired(started)) {
                    return result(Status.BUDGET_EXHAUSTED, "Planner returned too late",
                            calls, bestSource, best, snapshot);
                }
                if (proposal == null || !proposal.valid()) {
                    return result(Status.FAILED, "Planner returned an invalid proposal",
                            calls, bestSource, best, snapshot);
                }
                if (proposal.action() == Action.STOP) {
                    return result(Status.BLOCKED, proposal.reason(),
                            calls, bestSource, best, snapshot);
                }
                if (!seenSources.add(proposal.replacementSource())) {
                    return result(Status.STALLED, "Planner repeated an earlier source",
                            calls, bestSource, best, snapshot);
                }
                Observation next = verifier.verify(proposal.replacementSource(), remaining(started));
                source = proposal.replacementSource();
                observed = next;
                history.add(new Attempt(calls, proposal, observed));
                if (observed.check() != Check.UNAVAILABLE
                        && observed.passed().size() > best.passed().size()) {
                    bestSource = source;
                    best = observed;
                    nonImproving = 0;
                } else {
                    nonImproving++;
                }
                // A passed or unavailable check is handled at the top of the loop.
                if (observed.check() == Check.REJECTED
                        && nonImproving >= limits.nonImprovingAttempts()) {
                    return result(Status.STALLED, "No improvement in acceptance checks",
                            calls, bestSource, best,
                            new Snapshot(goal, source, observed, history));
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return result(Status.FAILED, "Execution interrupted", calls,
                    bestSource, best, new Snapshot(goal, source, observed, history));
        } catch (Exception failure) {
            return result(Status.FAILED, "Execution failed: "
                    + failure.getClass().getSimpleName(), calls,
                    bestSource, best, new Snapshot(goal, source, observed, history));
        }
    }

    private Duration remaining(long started) {
        long nanos = limits.duration().toNanos() - (nanoTime.getAsLong() - started);
        return Duration.ofNanos(Math.max(0, nanos));
    }

    private boolean expired(long started) {
        return remaining(started).isZero();
    }

    private static Result result(Status status, String reason, int calls,
                                 String source, Observation best, Snapshot latest) {
        return new Result(status, reason, calls, source, best, latest);
    }
}
