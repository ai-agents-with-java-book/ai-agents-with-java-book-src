package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.function.Supplier;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;

@ApplicationScoped
public class DynamicToolProviderSupplier
        implements Supplier<ToolProvider> {
    private final ToolProvider provider;

    public DynamicToolProviderSupplier() throws NoSuchMethodException {
        Clock clock = new Clock();
        var specification = ToolSpecifications
                .toolSpecificationsFrom(Clock.class).getFirst();
        var method = Clock.class.getMethod("getNow", String.class);
        var executor = DefaultToolExecutor.builder()
                .object(clock)
                .originalMethod(method)
                .methodToInvoke(method)
                .wrapToolArgumentsExceptions(true)
                .propagateToolExecutionExceptions(true)
                .build();
        provider = request -> request.userMessage().singleText()
                .contains("current")
                ? ToolProviderResult.builder()
                        .add(specification, executor).build()
                : ToolProviderResult.builder().build();
    }

    @Override
    public ToolProvider get() {
        return provider;
    }
}
