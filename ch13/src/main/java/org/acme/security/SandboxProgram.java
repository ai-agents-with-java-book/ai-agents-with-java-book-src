package org.acme.security;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

// Run this program only through IsolatedJavaExample; it demonstrates worker restrictions.
public final class SandboxProgram {
    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "calculate" : args[0];
        switch (mode) {
            case "calculate" -> System.out.println("sum=" + (20 + 22));
            case "write" -> {
                Files.writeString(Path.of("/input/unexpected.txt"), "This write must be rejected");
                System.out.println("WRITE_SUCCEEDED");
            }
            case "network" -> {
                try (var socket = new Socket()) {
                    socket.connect(new InetSocketAddress("192.0.2.1", 443), 1500);
                }
            }
            case "secret" -> System.out.println("hostSentinelVisible="
                    + (System.getenv("CH13_HOST_ONLY_SENTINEL") != null));
            case "timeout" -> Thread.sleep(120_000);
            case "output" -> System.out.print("x".repeat(20_000));
            default -> throw new IllegalArgumentException("Unknown mode: " + mode);
        }
    }
}
