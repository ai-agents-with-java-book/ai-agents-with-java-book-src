package org.acme.support;

public final class SummaryUnavailableException extends RuntimeException {
    public SummaryUnavailableException(Throwable cause) {
        super("Summary provider unavailable", cause);
    }
}
