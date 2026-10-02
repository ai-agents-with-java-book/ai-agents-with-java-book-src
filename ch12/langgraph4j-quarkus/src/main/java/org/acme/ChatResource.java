package org.acme;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.acme.ai.Assistant;
import org.acme.graph.QAAssistant;
import org.acme.graph.QAState;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Path("/chat")
public class ChatResource {

    private final Map<String, QAAssistant> sessions = new ConcurrentHashMap<>();

    //private final Assistant assistant;

    private final CompiledGraph<QAState> graph;

    public ChatResource(CompiledGraph<QAState> graph) {
        this.graph = graph;
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Answer ask(Question req) throws Exception {
        // if sessionId is null, agent starts with from scratch
        String sessionId = req.sessionId() != null
            ? req.sessionId()
            : "1";

        QAAssistant qaAssistant;
        if (sessions.containsKey(sessionId)) {
            qaAssistant = sessions.get(sessionId);
        } else {
            qaAssistant = new QAAssistant(graph, sessionId);
            sessions.put(sessionId, qaAssistant);
        }

        if (req.sessionId() == null) {
            return qaAssistant.startConversation(req.message());
        } else {
            return qaAssistant.continueConversation(req.message());
        }
    }
}
