package org.acme;

import org.acme.ai.SentimentAnalysis;

public record TicketResponse(String draft, SentimentAnalysis sentiment) {
}
