package org.acme.repair;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public final class Repair {
    private Repair() { }

    public enum Action { REPLACE_SOURCE, STOP }
    public enum Check { PASSED, REJECTED, UNAVAILABLE }
    public enum Status { COMPLETED, BLOCKED, STALLED, BUDGET_EXHAUSTED, FAILED }

    public record Proposal(Action action, List<String> plan,
                           String replacementSource, String reason) {
        public boolean valid() {
            return action != null && plan != null && plan.size() <= 6
                    && plan.stream().allMatch(s -> s != null
                        && !s.isBlank() && s.length() <= 500)
                    && reason != null && !reason.isBlank() && reason.length() <= 2000
                    && replacementSource != null && replacementSource.length() <= 16000
                    && (action == Action.STOP || (!plan.isEmpty()
                        && !replacementSource.isBlank()));
        }
    }

    public record Observation(Check check, Set<String> passed,
                              String feedback, String evidenceDirectory) {
        public Observation {
            passed = Set.copyOf(passed);
        }
    }

    public record Attempt(int number, Proposal proposal, Observation observation) { }

    public record Snapshot(String goal, String source, Observation observation,
                           List<Attempt> history) {
        public Snapshot {
            history = List.copyOf(history);
        }
    }

    public record Limits(int modelCalls, int nonImprovingAttempts, Duration duration) {
        public Limits {
            if (modelCalls < 1 || nonImprovingAttempts < 1
                    || duration == null || duration.isNegative() || duration.isZero()) {
                throw new IllegalArgumentException("Limits must be positive");
            }
        }
    }

    public record Result(Status status, String reason, int modelCalls,
                         String bestSource, Observation bestObservation,
                         Snapshot latest) { }

    @FunctionalInterface
    public interface Planner {
        Proposal propose(Snapshot snapshot);
    }

    @FunctionalInterface
    public interface Verifier {
        Observation verify(String source, Duration remaining) throws Exception;
    }
}
