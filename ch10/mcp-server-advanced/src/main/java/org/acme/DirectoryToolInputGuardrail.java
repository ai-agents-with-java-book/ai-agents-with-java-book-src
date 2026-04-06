package org.acme;


import io.quarkiverse.mcp.server.ToolCallException;
import io.quarkiverse.mcp.server.ToolInputGuardrail;

import java.nio.file.Path;
import java.nio.file.Paths;

public class DirectoryToolInputGuardrail implements ToolInputGuardrail {

    @Override
    public void apply(ToolInputContext context) {
        String directory = context.getArguments().getString("directory");
        validate(directory);
    }


    public void validate(String userInput) {

        // 1. Basic sanitization checks
        if (userInput.contains("\0")) {
            throw new ToolCallException("Invalid path: null byte detected");
        }

        // 2. Prevent absolute paths
        Path inputPath = Paths.get(userInput);
        if (inputPath.isAbsolute()) {
            throw new ToolCallException("Absolute paths are not allowed");
        }

        // 3. Normalize path (removes .. and .)
        Path normalized = inputPath.normalize();

        // 4. Prevent traversal outside base (early check)
        if (normalized.startsWith("..")) {
            throw new ToolCallException("Path traversal attempt detected");
        }

    }
}
