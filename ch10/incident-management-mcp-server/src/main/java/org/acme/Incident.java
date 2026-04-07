package org.acme;

public record Incident(
    String id,
    String title,
    Severity severity,
    Status status,
    String assignee
) {}