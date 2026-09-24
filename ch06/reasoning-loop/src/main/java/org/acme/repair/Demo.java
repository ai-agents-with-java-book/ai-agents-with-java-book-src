package org.acme.repair;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import static org.acme.repair.Repair.*;

public final class Demo {
    public static final String GOAL = """
            Preserve package org.acme and class Calculator with public methods
            int sum(int[] values), double average(int[] values), int max(int[] values).
            Sum returns the total, including zero for an empty array.
            Average preserves fractional results and rejects an empty array with
            IllegalArgumentException. Maximum works for all-negative arrays and
            rejects an empty array with IllegalArgumentException.
            Inputs are non-null; integer overflow is outside this exercise.
            """;

    public static Proposal scripted(Snapshot snapshot) {
        String source = snapshot.source();
        String next;
        String step;
        if (source.contains("i <= values.length")) {
            next = source.replace("i <= values.length", "i < values.length");
            step = "Stop the sum loop before the array length";
        } else if (source.contains("return sum(values) / values.length;")) {
            next = source.replace("return sum(values) / values.length;",
                    "return (double) sum(values) / values.length;");
            step = "Convert the sum to double before division";
        } else {
            next = source.replace("int max = 0;", "int max = values[0];");
            step = "Initialize the maximum from the first value";
        }
        return new Proposal(Action.REPLACE_SOURCE, List.of(step), next, step);
    }

    public static void main(String[] args) throws Exception {
        Path fixture = Path.of("fixture");
        String source = Files.readString(fixture.resolve(MavenVerifier.SOURCE));
        var verifier = new MavenVerifier(fixture, System.getProperty("repair.maven", "mvn"));
        var limits = new Limits(4, 2, Duration.ofMinutes(3));
        Result result = new RepairLoop(Demo::scripted, verifier, limits).run(GOAL, source);
        for (Attempt attempt : result.latest().history()) {
            System.out.printf("Attempt %d: %d/6 passed%n", attempt.number(),
                    attempt.observation().passed().size());
        }
        System.out.println(result.status() + ": " + result.reason());
        System.out.println("Evidence: " + result.bestObservation().evidenceDirectory());
        if (result.status() != Status.COMPLETED) {
            throw new IllegalStateException(result.reason());
        }
    }
}
