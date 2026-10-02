package org.acme;


import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.acme.ai.TicketResponseExperts;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class TicketResponseExpertsTest {

    @Inject
    TicketResponseExperts.ResponseGeneratorAgent responseGeneratorAgent;

    @Test
    public void shouldConnectToRemoteAgent() {
        System.out.println(responseGeneratorAgent.generateTicketResponse("Thank you very much for the flight it was great and no incidences."));
    }

    @Inject
    TicketResponseExperts.ReviewSentimentAgent sentimentAgent;

    @Test
    public void analyzeText() {
        System.out.println(sentimentAgent.classify("The experience was great, the trip was on time, so all good."));
    }

    @Inject
    TicketResponseExperts.AutoDraftFeedbackAgent autoDraftFeedbackAgent;

    @Test
    public void shouldAutoDraftAFeedback() {
        TicketResponse result = autoDraftFeedbackAgent.generateFeedback("1", "My seat was broken during the flight.");

        System.out.println(result);
    }


}
