package org.acme.support;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkiverse.flow.Flow;
import io.serverlessworkflow.api.types.Workflow;
import static io.quarkiverse.flow.dsl.FlowDSL.function;
import static io.quarkiverse.flow.dsl.FlowWorkflowBuilder.workflow;

@ApplicationScoped
public class SummaryFlow extends Flow {
    @Inject
    SupportSummary summary;

    @Override
    public Workflow descriptor() {
        return workflow("support-summary")
            .tasks(function("summarize", summary::summarize, String.class))
            .build();
    }
}
