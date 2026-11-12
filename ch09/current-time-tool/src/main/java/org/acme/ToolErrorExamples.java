package org.acme;

import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;

public final class ToolErrorExamples {
    public interface Assistant {
        String assist(String request);
    }

    public static Assistant unknownTool(ChatModel chatModel) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .tools(new Clock())
                .hallucinatedToolNameStrategy(request ->
                        ToolExecutionResultMessage.from(request,
                                "Unknown tool. Use an offered tool."))
                .build();
    }

    public static Assistant invalidArguments(ChatModel chatModel) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .tools(new Clock())
                .toolArgumentsErrorHandler((error, context) ->
                        ToolErrorHandlerResult.text(ToolFailures.report(
                                error, "Invalid tool arguments. Check the schema.")))
                .build();
    }

    public static Assistant executionFailure(ChatModel chatModel) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .tools(new Clock())
                .toolExecutionErrorHandler((error, context) ->
                        ToolErrorHandlerResult.text(ToolFailures.report(
                                error, "The tool could not complete the request.")))
                .build();
    }
}
