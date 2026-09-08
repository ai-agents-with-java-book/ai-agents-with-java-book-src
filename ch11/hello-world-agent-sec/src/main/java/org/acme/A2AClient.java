package org.acme;

import org.a2aproject.sdk.A2A;
import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.ClientEvent;
import org.a2aproject.sdk.client.MessageEvent;
import org.a2aproject.sdk.client.TaskEvent;
import org.a2aproject.sdk.client.TaskUpdateEvent;
import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.spi.interceptors.auth.AuthInterceptor;
import org.a2aproject.sdk.client.transport.spi.interceptors.auth.CredentialService;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.Message;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class A2AClient {

    static void main(String args[]) throws InterruptedException {
        AgentCard agentCard = fetchAgentCard();
        final CompletableFuture<String> messageResponse
            = new CompletableFuture<>();
        Client client = createClient(agentCard, messageResponse);

        Message message = A2A.toUserMessage("tell me a greeting");

        client.sendMessage(message);
        try {
            System.out.println(messageResponse.get());
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

    }

    public static Client createClient(
        final AgentCard agentCard,
        final CompletableFuture<String> messageResponse) {

        // Create consumers for handling client events
        List<BiConsumer<ClientEvent, AgentCard>> consumers =
            EventHandlerUtil.createEventConsumers(messageResponse);

        // Create error handler for streaming errors
        Consumer<Throwable> streamingErrorHandler =
            EventHandlerUtil.createStreamingErrorHandler(messageResponse);

        // Create credential service for OAuth2 authentication
        CredentialService credentialService
            = new KeycloakOauthCredentialService();

        // Create shared auth interceptor for all transports
        AuthInterceptor authInterceptor = new AuthInterceptor(credentialService);

        // Create the A2A client with the specified transport
        try {
            var builder =
                Client.builder(agentCard)
                    .addConsumers(consumers)
                    .streamingErrorHandler(streamingErrorHandler);

            // Configure only the specified transport

                    builder.withTransport(
                        JSONRPCTransport.class,
                        new JSONRPCTransportConfigBuilder()
                            .addInterceptor(authInterceptor) // auth config
                            .build());


            return builder.clientConfig(new ClientConfig.Builder().build()).build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create A2A client", e);
        }
    }

    private static AgentCard fetchAgentCard() {
        System.out.println("Fetching agent card...");
        AgentCard agentCard = A2A.getAgentCard("http://localhost:7777");
        System.out.println("✓ Agent: " + agentCard.name());
        System.out.println("✓ Description: " + agentCard.description());


        System.out.println();
        return agentCard;
    }
}
