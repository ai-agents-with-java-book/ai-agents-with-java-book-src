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
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.Message;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class A2AClient {

    static void main(String args[]) throws InterruptedException {
        AgentCard agentCard = fetchAgentCard();
        Client client = createClient(agentCard);

        Message message = A2A.toUserMessage("tell me a greeting");

        client.sendMessage(message);
        System.out.println(Thread.currentThread().getName());
        Thread.sleep(20000);

    }

    private static Client createClient(AgentCard agentCard) {
        System.out.println("Creating streaming client for subscription...");

        List<BiConsumer<ClientEvent, AgentCard>> consumers = List.of(
            (event, card) -> {
                System.out.println(Thread.currentThread().getName());
                switch (event) {
                    case MessageEvent messageEvent -> System.out.println("Message: " + messageEvent);
                    case TaskEvent taskEvent -> System.out.println("Task: " + taskEvent);
                    case TaskUpdateEvent updateEvent -> System.out.println("Task Update: " + updateEvent.getUpdateEvent());
                }
            }
        );

        // Create error handler for streaming errors
        Consumer<Throwable> streamingErrorHandler = (error) -> {
            if(error == null || error instanceof java.util.concurrent.CancellationException) {
                return;
            }
            System.err.println("Streaming error occurred: " + error.getMessage());
            error.printStackTrace();

        };

        ClientConfig streamingConfig = new ClientConfig.Builder()
            .setStreaming(true)
            .build();

        Client streamingClient = Client.builder(agentCard)
            .clientConfig(streamingConfig)
            .addConsumers(consumers)
            .streamingErrorHandler(streamingErrorHandler)
            .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
            .build();

        System.out.println("✓ Clients created");
        System.out.println();

        return streamingClient;
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
