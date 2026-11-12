package org.acme;

import java.util.UUID;

public final class ToolFailures {
    private static final System.Logger LOG =
            System.getLogger(ToolFailures.class.getName());

    private ToolFailures() {
    }

    public static String report(Throwable error, String explanation) {
        String reference = UUID.randomUUID().toString();
        LOG.log(System.Logger.Level.WARNING,
                "Tool call failed; reference " + reference, error);
        return explanation + " Reference: " + reference;
    }
}
