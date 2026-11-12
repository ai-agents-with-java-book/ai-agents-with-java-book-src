package org.acme.support;

import java.util.Objects;

public final class SupportDrafts {
    private final SupportSummary summary;

    public SupportDrafts(SupportSummary summary) {
        this.summary = Objects.requireNonNull(summary);
    }

    public String prepareSummary(String request) {
        if (request == null || request.isBlank()) {
            throw new IllegalArgumentException("Support request is required");
        }
        try {
            return summary.summarize(request);
        } catch (RuntimeException failure) {
            throw new SummaryUnavailableException(failure);
        }
    }
}
