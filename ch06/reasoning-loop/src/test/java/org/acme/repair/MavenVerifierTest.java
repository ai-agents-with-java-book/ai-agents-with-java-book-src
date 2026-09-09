package org.acme.repair;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.acme.repair.Repair.*;
import static org.junit.jupiter.api.Assertions.*;

class MavenVerifierTest {
    @TempDir Path directory;

    @Test void repairsTheRealFixtureInThreeIterations() throws Exception {
        Path fixture = Path.of("fixture");
        String initial = Files.readString(fixture.resolve(MavenVerifier.SOURCE));
        var verifier = new MavenVerifier(fixture, System.getProperty("repair.maven", "mvn"));
        var result = new RepairLoop(Demo::scripted, verifier,
                new Limits(4, 2, Duration.ofMinutes(3))).run(Demo.GOAL, initial);
        assertEquals(Status.COMPLETED, result.status(), result.latest().observation().toString());
        assertEquals(3, result.modelCalls());
        assertEquals(MavenVerifier.EXPECTED, result.bestObservation().passed());
        assertEquals(4, result.latest().history().get(0).observation().passed().size());
        assertEquals(5, result.latest().history().get(1).observation().passed().size());
        assertEquals(initial, Files.readString(fixture.resolve(MavenVerifier.SOURCE)));
        var directories = result.latest().history().stream()
                .map(a -> a.observation().evidenceDirectory()).distinct().count();
        assertEquals(3, directories);
    }

    @Test void compilationFailureBecomesRepairFeedback() throws Exception {
        var verifier = new MavenVerifier(Path.of("fixture"), "mvn");
        var observation = verifier.verify("package org.acme; public class Calculator { broken }",
                Duration.ofSeconds(10));
        assertEquals(Check.REJECTED, observation.check());
        assertTrue(observation.feedback().contains("error"));
        assertEquals(Set.of(), observation.passed());
    }

    @Test void expiredAllowanceDoesNotStartCompilation() throws Exception {
        var verifier = new MavenVerifier(Path.of("fixture"), "mvn");
        var observation = verifier.verify("invalid", Duration.ZERO);
        assertEquals(Check.UNAVAILABLE, observation.check());
        assertFalse(Files.exists(Path.of(observation.evidenceDirectory(), "compile.log")));
    }

    @Test void missingTestCannotProduceSuccess() throws Exception {
        assertEquals(Check.UNAVAILABLE, report("", 0).check());
    }

    @Test void skippedTestCannotProduceSuccess() throws Exception {
        String cases = cases().replace("name=\"fractionalAverage\"/>",
                "name=\"fractionalAverage\"><skipped/></testcase>");
        assertEquals(Check.REJECTED, report(cases, 0).check());
    }

    @Test void duplicateTestCannotProduceSuccess() throws Exception {
        assertEquals(Check.UNAVAILABLE, report(cases() + testCase("fractionalAverage"), 0).check());
    }

    @Test void successfulTestsCannotHideAFailedBuild() throws Exception {
        assertEquals(Check.UNAVAILABLE, report(cases(), 1).check());
    }

    private Observation report(String cases, int exit) throws Exception {
        Path report = directory.resolve("report.xml");
        Files.writeString(report, "<testsuite>" + cases + "</testsuite>");
        return MavenVerifier.readReport(report, exit, directory);
    }

    private static String cases() {
        return MavenVerifier.EXPECTED.stream().sorted().map(MavenVerifierTest::testCase)
                .reduce("", String::concat);
    }

    private static String testCase(String name) {
        return "<testcase classname=\"org.acme.CalculatorTest\" name=\"" + name + "\"/>";
    }
}
