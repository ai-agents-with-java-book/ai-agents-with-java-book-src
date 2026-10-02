package org.acme.graph;

import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.hook.NodeHook;
import org.bsc.langgraph4j.state.AgentState;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GuardrailNodeHook
    implements NodeHook.BeforeCall<QAState> {

    @Override
    public CompletableFuture<Map<String, Object>> applyBefore(String nodeId,
                                                              QAState state, RunnableConfig config) {

        List<String> questionList = state.question();

        String questions = questionList.stream()
                                .collect(Collectors.joining());

        if(questions.length() > 50) {
            throw new IllegalArgumentException("Questions too long");
        }

        return CompletableFuture.completedFuture(Map.of());
    }
}
