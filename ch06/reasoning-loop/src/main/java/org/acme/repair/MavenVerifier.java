package org.acme.repair;

import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import static org.acme.repair.Repair.*;

public final class MavenVerifier implements Verifier {
    public static final String SOURCE = "src/main/java/org/acme/Calculator.java";
    public static final String TEST = "src/test/java/org/acme/CalculatorTest.java";
    public static final Set<String> EXPECTED = Set.of("sumOfPositiveValues",
            "sumOfEmptyArray", "fractionalAverage", "averageRejectsEmptyArray",
            "maximumOfNegativeValues", "maximumRejectsEmptyArray");
    private final Path fixture;
    private final String maven;

    public MavenVerifier(Path fixture, String maven) {
        this.fixture = fixture;
        this.maven = maven;
    }

    @Override
    public Observation verify(String source, Duration remaining) throws Exception {
        long started = System.nanoTime();
        Path work = Files.createTempDirectory("calculator-check-");
        for (String name : List.of("pom.xml", TEST)) {
            Path destination = work.resolve(name);
            Files.createDirectories(destination.getParent());
            Files.copy(fixture.resolve(name), destination);
        }
        Path candidate = work.resolve(SOURCE);
        Files.createDirectories(candidate.getParent());
        Files.writeString(candidate, source);
        Path classes = Files.createDirectories(work.resolve("compile-check"));
        Path compileLog = work.resolve("compile.log");
        String javac = Path.of(System.getProperty("java.home"), "bin", "javac").toString();
        int compiled = execute(List.of(javac, "--release", "21", "-proc:none",
                "-d", classes.toString(), candidate.toString()), work, compileLog,
                left(remaining, started));
        if (compiled == -1) {
            return observation(Check.UNAVAILABLE, "Compilation timed out", work);
        }
        if (compiled != 0) {
            return observation(Check.REJECTED, tail(compileLog), work);
        }
        Path log = work.resolve("maven.log");
        int exit = execute(List.of(maven, "-o", "-B", "-q", "test"), work, log,
                left(remaining, started));
        if (exit == -1) {
            return observation(Check.UNAVAILABLE, "Acceptance test process timed out", work);
        }
        Path report = work.resolve("target/surefire-reports/TEST-org.acme.CalculatorTest.xml");
        if (!Files.isRegularFile(report)) {
            return observation(Check.UNAVAILABLE, "No acceptance report\n" + tail(log), work);
        }
        return readReport(report, exit, work);
    }

    static Observation readReport(Path report, int exit, Path work) throws Exception {
        if (Files.size(report) > 1_000_000) {
            return observation(Check.UNAVAILABLE, "Acceptance report is too large", work);
        }
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var document = factory.newDocumentBuilder().parse(report.toFile());
        var cases = document.getElementsByTagName("testcase");
        Set<String> names = new HashSet<>();
        Set<String> passed = new HashSet<>();
        var feedback = new StringBuilder();
        for (int i = 0; i < cases.getLength(); i++) {
            var test = (Element) cases.item(i);
            String name = test.getAttribute("name");
            if (!"org.acme.CalculatorTest".equals(test.getAttribute("classname"))
                    || !names.add(name) || !EXPECTED.contains(name)) {
                return observation(Check.UNAVAILABLE, "Unexpected acceptance test identity", work);
            }
            boolean failed = false;
            for (String tag : List.of("failure", "error", "skipped")) {
                var problems = test.getElementsByTagName(tag);
                if (problems.getLength() > 0) {
                    failed = true;
                    var problem = (Element) problems.item(0);
                    String detail = problem.getAttribute("message");
                    feedback.append(name).append(": ").append(tag).append(" ")
                            .append(detail, 0, Math.min(700, detail.length())).append('\n');
                }
            }
            if (!failed) {
                passed.add(name);
            }
        }
        if (!names.equals(EXPECTED)) {
            return observation(Check.UNAVAILABLE, "Acceptance report is incomplete", work);
        }
        if (passed.equals(EXPECTED) && exit != 0) {
            return observation(Check.UNAVAILABLE, "Tests passed but Maven failed", work);
        }
        Check check = exit == 0 && passed.equals(EXPECTED) ? Check.PASSED : Check.REJECTED;
        return new Observation(check, passed, feedback.toString(), work.toString());
    }

    private static int execute(List<String> command, Path work, Path log,
                               Duration timeout) throws Exception {
        long millis = Math.min(timeout.toMillis(), 60_000);
        if (millis <= 0) {
            return -1;
        }
        Process process = new ProcessBuilder(command).directory(work.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            return process.waitFor(millis, TimeUnit.MILLISECONDS) ? process.exitValue() : -1;
        } finally {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private static Duration left(Duration allowance, long started) {
        return Duration.ofNanos(Math.max(0, allowance.toNanos()
                - (System.nanoTime() - started)));
    }

    private static String tail(Path log) throws Exception {
        try (var file = new RandomAccessFile(log.toFile(), "r")) {
            int length = (int) Math.min(6000, file.length());
            file.seek(file.length() - length);
            byte[] bytes = new byte[length];
            file.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static Observation observation(Check check, String feedback, Path work) {
        return new Observation(check, Set.of(), feedback, work.toString());
    }
}
