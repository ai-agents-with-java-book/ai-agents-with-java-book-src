package org.aiagents.observability;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.ActivationCondition;
import dev.langchain4j.agentic.declarative.ChatMemoryProviderSupplier;
import dev.langchain4j.agentic.declarative.ConditionalAgent;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.MonitoredAgent;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

public class Agents {
    public interface MedicalExpert {

        @UserMessage("""
            You are a medical expert.
            Analyze the following user request under a medical point of view and provide the best possible answer.
            The user request is {request}.
            """)
        @Agent(description = "A medical expert", outputKey = "response")
        String medical(@MemoryId String memoryId, String request);

        @ChatMemoryProviderSupplier
        static ChatMemory chatMemory(Object memoryId) {
            return MessageWindowChatMemory.withMaxMessages(10);
        }
    }

    public interface LegalExpert {

        @UserMessage("""
            You are a legal expert.
            Analyze the following user request under a legal point of view and provide the best possible answer.
            The user request is {request}.
            """)
        @Agent(description = "A legal expert", outputKey = "response", summarizedContext = {"medical", "technical"})
        String legal(@MemoryId String memoryId, String request);

        @ChatMemoryProviderSupplier
        static ChatMemory chatMemory(Object memoryId) {
            return MessageWindowChatMemory.withMaxMessages(10);
        }
    }

    public interface TechnicalExpert {

        @UserMessage("""
            You are a technical expert.
            Analyze the following user request under a technical point of view and provide the best possible answer.
            The user request is {request}.
            """)
        @Agent(description = "A technical expert", outputKey = "response")
        String technical(@MemoryId String memoryId, String request);

        @ChatMemoryProviderSupplier
        static ChatMemory chatMemory(Object memoryId) {
            return MessageWindowChatMemory.withMaxMessages(10);
        }
    }

    public enum RequestCategory {
        LEGAL, MEDICAL, TECHNICAL, UNKNOWN
    }

    public interface ClassifierAgent {

        @UserMessage("""
            Analyze the following user request and categorize it as 'legal', 'medical' or 'technical'.
            In case the request doesn't belong to any of those categories categorize it as 'unknown'.
            Reply with only one of those words and nothing else.
            The user request is: '{request}'.
            """)
        @Agent(description = "Categorize a user request", outputKey = "category")
        RequestCategory classify(String request);
    }

    public interface ExpertsRouter {

        @ConditionalAgent(outputKey = "response",
                subAgents = { MedicalExpert.class, TechnicalExpert.class, LegalExpert.class })
        String askExpert(String request, RequestCategory category);

        @ActivationCondition(value = MedicalExpert.class, description = "medical")
        static boolean activateMedical(RequestCategory category) {
            return category == RequestCategory.MEDICAL;
        }

        @ActivationCondition(value = TechnicalExpert.class, description = "technical")
        static boolean activateTechnical(RequestCategory category) {
            return category == RequestCategory.TECHNICAL;
        }

        @ActivationCondition(value = LegalExpert.class, description = "legal")
        static boolean activateLegal(RequestCategory category) {
            return category == RequestCategory.LEGAL;
        }
    }

    public interface ExpertsChatbot extends MonitoredAgent {

        @SequenceAgent(outputKey = "response",
                subAgents = { ClassifierAgent.class, ExpertsRouter.class })
        String chat(@MemoryId String memoryId, String request);
    }

    public static class NoOpAgent {
        @Agent(outputKey = "response")
        public static String noop(String response) {
            return response;
        }
    }

    public interface ExpertsChatbot2 extends MonitoredAgent {
        @SequenceAgent(outputKey = "response",
                subAgents = { ClassifierAgent.class, ExpertsRouter.class, NoOpAgent.class })
        String chat2(@MemoryId String memoryId, String request);
    }
}
