package org.acme.graph;


import org.acme.Answer;
import org.acme.ai.Assistant;
import org.acme.ai.AssistantResponse;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphDefinition;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.utils.EdgeMappings;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;


public class QAAssistant {

    private final CompiledGraph<QAState> graph;
    private RunnableConfig config;
    private final String threadId;

    public QAAssistant(CompiledGraph<QAState> graph, String id) {
        this.graph = graph;
        this.threadId = id;
    }

    public Answer startConversation(String question) {
        config = RunnableConfig.builder()
            .threadId(threadId)
            .build();
        Optional<QAState> result = this.graph.invoke(Map.of("question", question), config);
        Optional<Answer> chatResponse = result.map(
            state -> new Answer(this.threadId, state.messages(), state.human())
        );
        return chatResponse.orElseThrow();
    }

    public Answer continueConversation(String question) throws Exception {
        var runnableConfig =  RunnableConfig.builder()
            .threadId(threadId)
            .build();

        var updateConfig = graph.updateState(runnableConfig,
            Map.of("question", question),
            null);

        final Optional<QAState> result =
            graph.invoke(GraphInput.resume(), updateConfig);

        Optional<Answer> chatResponse = result.map(
            state -> new Answer(this.threadId, state.messages(), state.human())
        );
        return chatResponse.orElseThrow();

    }

}
