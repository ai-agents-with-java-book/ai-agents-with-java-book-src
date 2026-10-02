package org.acme;

import io.quarkiverse.flow.Flow;
import io.quarkiverse.flow.dsl.FlowDSL;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowContextData;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

import static io.quarkiverse.flow.dsl.FlowWorkflowBuilder.workflow;

@ApplicationScoped
public class SecretEchoFlow extends Flow {
    @Override
    public Workflow descriptor() {
        return workflow()
            .use(u -> u.secrets("mySecret")) // declare the handle
            .tasks(
                t -> t.set("${ $secret.mySecret.password }"),
                FlowDSL.withContext("map-secret", (String secret, WorkflowContextData ctx) -> Map.of("secret", secret), String.class)
                )
            .build();
    }
}