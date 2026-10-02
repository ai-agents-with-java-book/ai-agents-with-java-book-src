package org.acme.ai;

import org.acme.Sentiment;

public record SentimentAnalysis(String reason, Sentiment result) {
}
