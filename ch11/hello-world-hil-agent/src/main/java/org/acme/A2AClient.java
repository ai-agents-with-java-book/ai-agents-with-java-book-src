package org.acme;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.a2aproject.sdk.A2A;
import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.TextPart;

public class A2AClient {
    private static final Duration NETWORK_TIMEOUT = Duration.ofSeconds(30);

    static void main(String[] args) throws Exception {
        var card = A2A.getAgentCard("http://localhost:7777");
        var session = new HilSession();
        try (Client client = Client.builder(card)
                .clientConfig(new ClientConfig.Builder().setStreaming(true).build())
                .addConsumers(List.of(session))
                .streamingErrorHandler(session::fail)
                .withTransport(JSONRPCTransport.class,
                        new JSONRPCTransportConfigBuilder())
                .build()) {
            client.sendMessage(A2A.toUserMessage("tell me a greeting"));
            HilSession.Prompt prompt = session.awaitPrompt(NETWORK_TIMEOUT);
            if (prompt != null) {
                String choice = session.readChoice(
                        new InputStreamReader(System.in, StandardCharsets.UTF_8));
                if (choice == null) return;
                if (session.claimReply()) {
                    client.sendMessage(Message.builder()
                            .role(Message.Role.ROLE_USER)
                            .parts(List.of(new TextPart(choice)))
                            .taskId(prompt.taskId()).contextId(prompt.contextId())
                            .build());
                }
            }
            session.awaitCompletion(NETWORK_TIMEOUT);
        }
    }
}
