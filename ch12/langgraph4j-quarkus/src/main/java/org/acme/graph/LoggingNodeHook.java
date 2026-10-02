package org.acme.graph;

import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.hook.NodeHook;
import org.bsc.langgraph4j.state.AgentState;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public record LoggingNodeHook<QAState extends AgentState>(Logger log)
    implements NodeHook.WrapCall<QAState> {

    @Override
    public CompletableFuture<Map<String, Object>> applyWrap(
        String nodeId, QAState state,
        RunnableConfig config,
        AsyncNodeActionWithConfig<QAState> action) {

        log.infof("node action for node '%s' start with state:%s", nodeId, state);
        return action.apply(state, config)
            .whenComplete( ( result, exception ) -> {
                log.infof("node action for node %s end with request update: %s", nodeId, result);
            });
    }
}
