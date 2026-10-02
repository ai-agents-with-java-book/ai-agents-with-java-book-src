package org.acme.graph;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class QAState extends AgentState {

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
        "question", Channels.appender(ArrayList::new),
        "human", Channels.base(() -> false),
        "messages", Channels.base(() -> "")
    );

    public QAState(Map<String, Object> data) {
        super(data);
    }

    public List<String> question() {
        return this.<List<String>>value("question")
            .orElse(Collections.emptyList());
    }

    public Boolean human() {
        return this.<Boolean>value("human").orElse(true);
    }

    public String messages() {
        return this.<String>value("messages").orElse("");
    }
}
