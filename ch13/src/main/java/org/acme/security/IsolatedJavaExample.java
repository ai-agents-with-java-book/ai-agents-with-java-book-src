package org.acme.security;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class IsolatedJavaExample {
    private static final int MAX_SOURCE_BYTES = 64 * 1024;
    private static final int MAX_OUTPUT_BYTES = 8 * 1024;
    private static final Duration CONTROL_TIMEOUT = Duration.ofSeconds(20);

    private record CommandResult(int exitCode, boolean timedOut, String output, boolean truncated) { }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException("Pass a Java source file and optionally a program argument");
        }
        String runtime = System.getProperty("sandbox.runtime", "docker");
        if (!Set.of("docker", "podman").contains(runtime)) {
            throw new IllegalArgumentException("sandbox.runtime must be docker or podman");
        }
        String image = System.getProperty("sandbox.image", "");
        if (!image.matches("[a-zA-Z0-9./:_-]+@sha256:[a-f0-9]{64}")) {
            throw new IllegalArgumentException("Set sandbox.image to a pre-pulled JDK image@sha256:digest");
        }
        int seconds = Integer.getInteger("sandbox.seconds", 10);
        if (seconds < 1 || seconds > 60) {
            throw new IllegalArgumentException("sandbox.seconds must be between 1 and 60");
        }
        byte[] source;
        try (var input = Files.newInputStream(Path.of(args[0]))) {
            source = input.readNBytes(MAX_SOURCE_BYTES + 1);
        }
        if (source.length > MAX_SOURCE_BYTES) {
            throw new IllegalArgumentException("Source exceeds 64 KiB");
        }
        String sourceHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source));
        Path inputDirectory = Files.createTempDirectory("ch13-source-");
        String container = "ch13-java-" + UUID.randomUUID();
        Path stagedSource = inputDirectory.resolve("Program.java");
        try {
            Files.write(stagedSource, source);
            // The unprivileged worker must be able to read the one mounted input directory.
            Files.setPosixFilePermissions(inputDirectory, PosixFilePermissions.fromString("r-xr-xr-x"));
            Files.setPosixFilePermissions(stagedSource, PosixFilePermissions.fromString("r--r--r--"));
            if (inputDirectory.toString().contains(",")) {
                throw new IllegalArgumentException("Temporary path cannot contain a comma");
            }
            var create = new ArrayList<>(List.of(runtime, "create", "--name", container,
                    "--pull=never", "--network=none", "--read-only", "--user=65534:65534",
                    "--cap-drop=ALL", "--security-opt=no-new-privileges",
                    "--cpus=1", "--memory=512m", "--memory-swap=512m", "--pids-limit=64",
                    "--shm-size=16m", "--log-driver=none",
                    "--tmpfs=/tmp:rw,noexec,nosuid,nodev,size=64m,mode=1777",
                    "--mount=type=bind,src=" + inputDirectory + ",dst=/input,readonly",
                    "--workdir=/tmp", "--entrypoint=java", image,
                    "-Xmx128m", "-XX:+UseSerialGC", "-XX:ActiveProcessorCount=1",
                    "-Djava.io.tmpdir=/tmp", "-Duser.home=/tmp", "--source", "21", "/input/Program.java"));
            if (args.length == 2) {
                create.add(args[1]);
            }
            CommandResult created;
            try {
                created = run(create, CONTROL_TIMEOUT);
            } catch (Exception creationFailure) {
                try {
                    CommandResult removed = run(List.of(runtime, "rm", "--force", container), CONTROL_TIMEOUT);
                    if (removed.timedOut() || removed.exitCode() != 0) {
                        creationFailure.addSuppressed(new IOException("Cleanup unconfirmed for " + container));
                    }
                } catch (Exception cleanupFailure) {
                    creationFailure.addSuppressed(cleanupFailure);
                }
                throw creationFailure;
            }
            if (created.timedOut() || created.exitCode() != 0) {
                // A lost CLI response can leave a container behind. Try the known name.
                CommandResult removed = run(List.of(runtime, "rm", "--force", container), CONTROL_TIMEOUT);
                String cleanup = !removed.timedOut() && removed.exitCode() == 0
                        ? "Container removed" : "Check runtime for container " + container;
                throw new IOException("Container creation unavailable: " + printable(created.output())
                        + ". " + cleanup);
            }
            try {
                CommandResult result = run(List.of(runtime, "start", "--attach", container),
                        Duration.ofSeconds(seconds));
                System.out.println("sourceSha256=" + sourceHash);
                System.out.println("status=" + (result.timedOut() ? "TIMED_OUT"
                        : result.exitCode() == 0 ? "EXITED_ZERO" : "NONZERO_EXIT_OR_RUNTIME_ERROR"));
                System.out.println("exitCode=" + result.exitCode() + " outputTruncated=" + result.truncated());
                System.out.println("workerOutput=" + printable(result.output()));
            } finally {
                // Killing the attached CLI is not enough: remove the daemon-managed container.
                CommandResult removed = run(List.of(runtime, "rm", "--force", container), CONTROL_TIMEOUT);
                if (removed.timedOut() || removed.exitCode() != 0) {
                    throw new IOException("Cleanup unconfirmed for " + container + ": "
                            + printable(removed.output()));
                }
                System.out.println("containerRemoved=" + container);
            }
        } finally {
            Files.setPosixFilePermissions(inputDirectory, PosixFilePermissions.fromString("rwx------"));
            Files.deleteIfExists(stagedSource);
            Files.deleteIfExists(inputDirectory);
        }
    }

    private static CommandResult run(List<String> command, Duration timeout) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        process.getOutputStream().close();
        var captured = new ByteArrayOutputStream();
        boolean[] truncated = {false};
        IOException[] readFailure = {null};
        Thread reader = Thread.ofVirtual().start(() -> {
            try (var input = process.getInputStream()) {
                byte[] buffer = new byte[1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    synchronized (captured) {
                        int retained = Math.min(count, MAX_OUTPUT_BYTES - captured.size());
                        captured.write(buffer, 0, retained);
                        if (retained < count) {
                            truncated[0] = true;
                        }
                    }
                }
            } catch (IOException failure) {
                readFailure[0] = failure;
            }
        });
        boolean finished;
        try {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        }
        boolean drained = reader.join(Duration.ofSeconds(2));
        if (!drained) {
            process.getInputStream().close();
            reader.join(Duration.ofSeconds(2));
        }
        if (finished && (!drained || readFailure[0] != null)) {
            throw new IOException("Could not read complete runtime output", readFailure[0]);
        }
        synchronized (captured) {
            return new CommandResult(finished ? process.exitValue() : -1, !finished,
                    captured.toString(StandardCharsets.UTF_8), truncated[0]);
        }
    }

    // Keep untrusted terminal control characters out of the example's own output.
    private static String printable(String text) {
        StringBuilder result = new StringBuilder();
        text.codePoints().forEach(cp -> {
            if (Character.isISOControl(cp) || Character.getType(cp) == Character.FORMAT) {
                result.append(String.format("\\u%04x", cp));
            } else {
                result.appendCodePoint(cp);
            }
        });
        return result.toString();
    }
}
