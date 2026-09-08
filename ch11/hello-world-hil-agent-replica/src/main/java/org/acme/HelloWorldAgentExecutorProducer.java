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
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.a2aproject.sdk.spec.TextPart;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class HelloWorldAgentExecutorProducer {

    // Metadata key used to carry the original input text across the HIL boundary.
    static final String ORIGINAL_TEXT_KEY = "hil_original_text";

    @Inject
    Logger logger;

    @Produces
    public AgentExecutor agentExecutor() {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {

                Task currentTask = context.getTask();

                // ── Re-invocation after Human-in-the-Loop ────────────────────────────
                if (currentTask != null
                        && currentTask.status().state() == TaskState.TASK_STATE_INPUT_REQUIRED) {

                    String userChoice = context.getUserInput("upper");
                    logger.infof("User chose: %s", userChoice);

                    // Read the original text from the task metadata stored during
                    // the first invocation via the WORKING status update event.
                    String originalText = "";
                    if (currentTask.metadata() != null) {
                        Object stored = currentTask.metadata().get(ORIGINAL_TEXT_KEY);
                        if (stored != null) {
                            originalText = stored.toString();
                        }
                    }
                    logger.infof("Original text to transform: %s", originalText);

                    String result;
                    if ("lower".equalsIgnoreCase(userChoice.trim())) {
                        result = originalText.toLowerCase();
                    } else {
                        result = originalText.toUpperCase();
                    }

                    emitter.addArtifact(List.of(new TextPart(result)));
                    emitter.complete();
                    logger.info("HIL complete — result: " + result);
                    return;
                }

                // ── First invocation: pause and ask the user ──────────────────────────
                String inputText = extractTextFromMessage(context.getMessage());
                logger.infof("Received message from client: %s", inputText);

                // Emit a WORKING status that carries the original text in its metadata.
                // The SDK merges TaskStatusUpdateEvent.metadata() into Task.metadata(),
                // making it available to us on re-invocation via context.getTask().metadata().
                Message step1Msg = emitter.messageBuilder()
                        .parts(List.of(new TextPart("Step 1: message received, waiting for your decision")))
                        .build();

                TaskStatusUpdateEvent workingEvent = TaskStatusUpdateEvent.builder()
                        .taskId(emitter.getTaskId())
                        .contextId(emitter.getContextId())
                        .status(new TaskStatus(TaskState.TASK_STATE_WORKING, step1Msg, null))
                        .metadata(Map.of(ORIGINAL_TEXT_KEY, inputText))
                        .build();
                emitter.emitEvent(workingEvent);
                logger.info("Finished Step 1");

                // Pause execution and ask the user to choose the transformation
                Message inputRequestMsg = emitter.messageBuilder()
                        .parts(List.of(new TextPart(
                                "Do you want to transform the text to 'upper' or 'lower' case? "
                                + "Reply with 'upper' or 'lower'.")))
                        .build();
                emitter.requiresInput(inputRequestMsg);
                logger.info("Waiting for human input...");
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
                    throw new TaskNotCancelableError();
                }

                if (task.status().state() == TaskState.TASK_STATE_COMPLETED) {
                    throw new TaskNotCancelableError();
                }

                emitter.cancel();
            }
        };
    }
}
