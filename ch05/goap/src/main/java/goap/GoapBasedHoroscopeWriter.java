package goap;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.observability.AgentMonitor;
import dev.langchain4j.agentic.patterns.goap.GoalOrientedPlanner;
import dev.langchain4j.model.chat.ChatModel;

import java.util.Map;

public class GoapBasedHoroscopeWriter {

    private static final ChatModel CHAT_MODEL = ChatModelProvider.createChatModel();
    
    public static void main(String[] args) {
        AgentMonitor monitor = new AgentMonitor();

        HoroscopeGenerator horoscopeGenerator = AgenticServices.agentBuilder(HoroscopeGenerator.class)
                .chatModel(CHAT_MODEL)
                .outputKey("horoscope")
                .build();

        PersonExtractor personExtractor = AgenticServices.agentBuilder(PersonExtractor.class)
                .chatModel(CHAT_MODEL)
                .outputKey("person")
                .build();

        SignExtractor signExtractor = AgenticServices.agentBuilder(SignExtractor.class)
                .chatModel(CHAT_MODEL)
                .outputKey("sign")
                .build();

        Writer writer = AgenticServices.agentBuilder(Writer.class)
                .chatModel(CHAT_MODEL)
                .outputKey("writeup")
                .build();

        StoryFinder storyFinder = AgenticServices.agentBuilder(StoryFinder.class)
                .chatModel(CHAT_MODEL)
                .tools(new WebSearchTool())
                .outputKey("story")
                .build();

        UntypedAgent horoscopeAgent = AgenticServices.plannerBuilder()
                .subAgents(horoscopeGenerator, personExtractor, signExtractor, writer, storyFinder)
                .outputKey("writeup")
                .planner(GoalOrientedPlanner::new)
                .listener(monitor)
                .build();

        Map<String, Object> input = Map.of("prompt", "My name is Mario and my zodiac sign is pisces");
        System.out.println(horoscopeAgent.invoke(input));

        System.out.println(monitor.successfulExecutions().get(0));
    }
}
