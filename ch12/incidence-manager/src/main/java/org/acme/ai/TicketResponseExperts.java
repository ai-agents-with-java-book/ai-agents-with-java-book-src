package org.acme.ai;


import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.A2AClientAgent;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.ParallelAgent;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import org.acme.TicketResponse;


public class TicketResponseExperts {

    public interface AutoDraftFeedbackAgent {

        @ParallelAgent(
            outputKey = "final",
            subAgents = { ReviewSentimentAgent.class, GenerateFeedbackAgent.class })
        TicketResponse generateFeedback(@MemoryId String memoryId, String request);

        @Output
        static TicketResponse createResponse(SentimentAnalysis sentiment, String response) {
            return new TicketResponse(response, sentiment);
        }

    }

    public interface GenerateFeedbackAgent {
        @SequenceAgent(description = "Draft, criticize, and review the response for a customer support ticket for galaxy airline agency",
            outputKey = "response", subAgents = {
            NoAgent.class, CriticAgent.class, CriticEditorAgent.class })
        String write(@MemoryId String memoryId, String request);
    }

    @SystemMessage("""
         You are a meticulous reviewer of responses for a customer support for a galaxy trip agency.
          Your job is to evaluate the provided draft based on tone, clarity, factuality, and compliance.

          Compliance Rules:
          - NO promises of returns.
          - MUST disclose risks.
          - NO personal data.
          - NO hype or guarantees.

          Evaluation Rules:
          - If the draft violates ANY compliance rules or the tone is poor, set the verdict to REVISE and list the reasons.
          - If the draft is excellent and compliant, set the verdict to APPROVE.

          CRITICAL OUTPUT INSTRUCTIONS:
          You MUST return a fully populated JSON object. Do not omit any fields.
          Your JSON must contain exactly these four keys:
          1. "verdict": either "APPROVE" or "REVISE"
          2. "reasons": an array of strings explaining your verdict
          3. "suggestions": an array of strings with ideas for improvement
          4. "scores": a nested object containing integer scores (0-100) for "clarity", "tone", "compliance", "factuality", and "overall".
        """)
    public interface CriticAgent {

        @Agent(outputKey = "review")
        @UserMessage("""
            Please review the following response for customer ticket draft:
            
                        {draft}
            """)
        CriticReview critique(String draft);
    }

    @SystemMessage("""
        You are a strict compliance and logic editor for a response to a ticket support from a galaxy trip airline.
        Your job is to revise the provided draft based ONLY on the automated critic's feedback.

        Rules:
        - Prioritize fixing compliance issues (e.g., removing promises of returns, adding risk disclosures).
        - Apply the critic's suggestions precisely.
        - Maintain the exact structure with the response.
        """)
    public interface CriticEditorAgent {

        @UserMessage("""
            Please rewrite the following draft based on the critic's feedback.
            
            The output should be only the message without any mention if this is a draft or an AI response
            
            --- ORIGINAL DRAFT ---
            {draft}
            --- CRITIC FEEDBACK ---
            Reasons for revision: {review.reasons}
            Suggestions for improvement: {review.suggestions}
            """)
        @Agent(outputKey = "response")
        String edit(String draft,
                             CriticReview review);

    }

    public class NoAgent {
        @Agent(description = "Agent that does nothing", outputKey = "draft")
        public static String no(String request) {
            return new String("Thank you very much for your kind words");
        }
    }

    public interface ResponseGeneratorAgent {

        @A2AClientAgent(a2aServerUrl = "http://localhost:7777", outputKey = "draft")
        String generateTicketResponse(String request);
    }

    @RegisterAiService
    public interface ReviewSentimentAgent {

        @Agent(description = "Classify the text", outputKey = "sentiment")
        @SystemMessage("""
            You classify customer interactions for a customer support workflow.
            Classify with only one label from this list:
            POSITIVE, NEUTRAL, NEGATIVE

            Also you need to return the reason why you categorized in this category.
            
            Return must be a JSON document, you can see an example of that:
                        {
                            "reason": "Reason of the classification",
                            "result": "POSITIVE" 
                        } 
            """)
        @UserMessage("""
            The feedback from user is: {request}
        """)
        SentimentAnalysis classify(String request);
    }
}

