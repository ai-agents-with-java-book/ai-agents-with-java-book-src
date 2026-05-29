package org.aiagents.observability;

import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;

@WebSocket(path = "/chatbot")
public class ChatBotWebSocket {

    @Inject
    WebSocketConnection connection;

    @Inject
    Agents.ExpertsChatbot chatbot;

    @Inject
    MedicalExpertAgent experts;

    @OnOpen
    public String onOpen() {
        return "Hello, I am a expert problem solver bot, how can I help?";
    }

    @OnTextMessage
    @Blocking
    public String onMessage(String request) {
        return chatbot.chat(connection.id(), request);
    }

}
