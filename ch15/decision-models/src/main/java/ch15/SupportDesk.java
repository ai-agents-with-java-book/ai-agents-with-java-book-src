package ch15;

import dev.langchain4j.model.decision.request.DecisionRequestParameters;
import dev.langchain4j.model.decision.response.YesNoAnswer;
import dev.langchain4j.service.V;
import dev.langchain4j.service.decision.Choice;
import dev.langchain4j.service.decision.Decide;
import dev.langchain4j.service.decision.DecisionResult;
import dev.langchain4j.service.decision.Scale;

import java.util.concurrent.CompletableFuture;

public interface SupportDesk {

    @Decide("""
            Is this message spam?
            Yes: promotion of a product or website, phishing, scams,
            links unrelated to our product.
            No: a genuine question, complaint or request, even if it is rude.""")
    boolean isSpam(String message);

    @Decide("Is this message spam?")
    YesNoAnswer spamProbability(String message);

    @Decide("Which team should handle this ticket?")
    Team route(String ticket);

    @Decide("Which team should handle this ticket?")
    Choice<Team> routeWithProbabilities(String ticket);

    @Decide("How frustrated is the customer?")
    Scale<Frustration> frustration(String ticket);

    record Triage(
            @Decide("Which team should handle this ticket?") Team team,
            @Decide("Does this ticket need attention today, given the urgency rules?")
            boolean urgent,
            @Decide("Does the customer ask for money back?") YesNoAnswer refund) {}

    Triage triage(String ticket, String urgencyRules);

    @Decide("Which team should handle this ticket?")
    DecisionResult<Team> routeWithMetadata(String ticket);

    @Decide("Which team should handle this ticket?")
    CompletableFuture<Team> routeAsync(String ticket);

    @Decide("Which team should handle this ticket?")
    Team route(@V("ticket") String ticket, DecisionRequestParameters parameters);
}
