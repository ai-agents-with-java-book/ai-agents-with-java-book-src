package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskNotCancelableError;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TextPart;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class HelloWorldAgentExecutorProducer {

    @Inject
    Logger logger;

    @Produces
    public AgentExecutor agentExecutor() {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {

                String response = extractTextFromMessage(context.getMessage());
                logger.infof("Received Message from Client Agent: %s", response);

                // Use status updates (with an optional message) for intermediate progress.
                // emitter.sendMessage() emits a Message event which the SDK treats as a
                // final/terminal event and immediately closes the SSE stream.
                Message step1Msg = emitter.messageBuilder()
                        .parts(List.of(new TextPart("Step 1: Creates Hello Message")))
                        .build();
                emitter.startWork(step1Msg);
                logger.info("Finished Step 1");

                response = response.concat("*");

                Message step2Msg = emitter.messageBuilder()
                        .parts(List.of(new TextPart("Step 2: Process Hello Message")))
                        .build();
                emitter.updateStatus(TaskState.TASK_STATE_WORKING, step2Msg);
                logger.info("Finished Step 2");

                response = response.toUpperCase();

                TextPart responsePart = new TextPart(response);
                List<Part<?>> parts = List.of(responsePart);

                emitter.addArtifact(parts);

                logger.info("Complete");

                emitter.complete();
            }

            private String extractTextFromMessage(Message message) {
                StringBuilder textBuilder = new StringBuilder();
                if (message.parts() != null) {
                    for (Part<?> part : message.parts()) {
                        if (part instanceof TextPart textPart) {
                            textBuilder.append(textPart.text());
                        }
                    }
                }
                return textBuilder.toString();
            }

            @Override
            public void cancel(RequestContext context, AgentEmitter emitter) throws A2AError {
                Task task = context.getTask();

                if (task.status().state() == TaskState.TASK_STATE_CANCELED) {
                    // task already cancelled
                    throw new TaskNotCancelableError();
                }

                if (task.status().state() == TaskState.TASK_STATE_COMPLETED) {
                    // task already completed
                    throw new TaskNotCancelableError();
                }

                // cancel the task
                emitter.cancel();
            }
        };
    }
}
