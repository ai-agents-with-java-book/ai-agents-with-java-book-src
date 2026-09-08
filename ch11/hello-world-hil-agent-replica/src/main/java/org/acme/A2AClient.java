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
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.a2aproject.sdk.spec.TextPart;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class A2AClient {

    static void main(String[] args) throws Exception {
        AgentCard agentCard = fetchAgentCard();

        // ── Step 1: send the initial message ─────────────────────────────────
        Message message = A2A.toUserMessage("tell me a greeting");
        System.out.println("Sending initial message...");

        // We need to capture the taskId + contextId from the input_required event
        // so we can re-send to the same task.
        AtomicReference<String> pendingTaskId = new AtomicReference<>();
        AtomicReference<String> pendingContextId = new AtomicReference<>();
        CountDownLatch inputRequiredLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(1);

        List<BiConsumer<ClientEvent, AgentCard>> consumers = List.of(
            (event, card) -> {
                System.out.println("[event] " + Thread.currentThread().getName());
                switch (event) {
                    case MessageEvent messageEvent -> {
                        System.out.println("Message: " + messageEvent);
                    }
                    case TaskEvent taskEvent -> {
                        System.out.println("Task: " + taskEvent);
                    }
                    case TaskUpdateEvent updateEvent -> {
                        System.out.println("Task Update: " + updateEvent.getUpdateEvent());

                        if (updateEvent.getUpdateEvent() instanceof TaskStatusUpdateEvent statusUpdate) {
                            TaskState state = statusUpdate.status().state();

                            if (state == TaskState.TASK_STATE_INPUT_REQUIRED) {
                                // Extract the agent's prompt from the status message
                                Message agentPrompt = statusUpdate.status().message();
                                if (agentPrompt != null && agentPrompt.parts() != null) {
                                    agentPrompt.parts().stream()
                                        .filter(p -> p instanceof TextPart)
                                        .map(p -> ((TextPart) p).text())
                                        .forEach(t -> System.out.println("\n[Agent asks]: " + t));
                                }
                                // Capture task/context IDs for the follow-up message
                                pendingTaskId.set(statusUpdate.taskId());
                                pendingContextId.set(statusUpdate.contextId());
                                inputRequiredLatch.countDown();

                            } else if (state.isFinal()) {
                                doneLatch.countDown();
                            }
                        }
                    }
                }
            }
        );

        Consumer<Throwable> errorHandler = error -> {
            if (error == null || error instanceof java.util.concurrent.CancellationException) {
                return;
            }
            System.err.println("Streaming error: " + error.getMessage());
            error.printStackTrace();
        };

        ClientConfig streamingConfig = new ClientConfig.Builder()
            .setStreaming(true)
            .build();

        Client streamingClient = Client.builder(agentCard)
            .clientConfig(streamingConfig)
            .addConsumers(consumers)
            .streamingErrorHandler(errorHandler)
            .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
            .build();

        streamingClient.sendMessage(message);

        // ── Step 2: wait for input_required, read user choice from stdin ──────
        System.out.println("Waiting for agent to request input...");
        inputRequiredLatch.await();

        String userChoice;
        try (Scanner scanner = new Scanner(System.in)) {
            do {
                System.out.print("Enter your choice ('upper' or 'lower'): ");
                userChoice = scanner.nextLine().trim();
            } while (!userChoice.equalsIgnoreCase("upper") && !userChoice.equalsIgnoreCase("lower"));
        }

        // ── Step 3: re-send the choice to the same task ───────────────────────
        String taskId = pendingTaskId.get();
        String contextId = pendingContextId.get();
        System.out.println("Sending choice '" + userChoice + "' to task " + taskId);

        Message replyMessage = Message.builder()
            .role(Message.Role.ROLE_USER)
            .parts(List.of(new TextPart(userChoice)))
            .taskId(taskId)
            .contextId(contextId)
            .build();

        streamingClient.sendMessage(replyMessage);

        // ── Step 4: wait for the final result ────────────────────────────────
        doneLatch.await();
        System.out.println("Done.");
        streamingClient.close();
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
