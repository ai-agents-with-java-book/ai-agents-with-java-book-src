package org.acme;

import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.HandleToolArgumentError;
import io.quarkiverse.langchain4j.HandleToolExecutionError;
import dev.langchain4j.service.tool.ToolErrorContext;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;

@RegisterAiService(toolProviderSupplier = DynamicToolProviderSupplier.class)
public interface MyAiService {
    String assist(String request);

    @HandleToolArgumentError
    static String handleArgumentError(Throwable error) {
        return ToolFailures.report(error,
                "Invalid tool arguments. Check the schema.");
    }

    @HandleToolExecutionError
    static ToolErrorHandlerResult handleExecutionError(
            Throwable error, ToolErrorContext context) {
        return ToolErrorHandlerResult.text(ToolFailures.report(error,
                "Tool '" + context.toolExecutionRequest().name()
                        + "' could not complete the request."));
    }
}
