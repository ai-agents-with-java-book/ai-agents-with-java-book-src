package org.acme;

public record Answer(String sessionId, String agentMessage, boolean waitingForUser) {
}
