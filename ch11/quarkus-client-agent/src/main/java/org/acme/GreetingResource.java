package org.acme;


import dev.langchain4j.agentic.a2a.A2ATaskInterruptedException;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.AgenticSystemSuspendedException;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.a2aproject.sdk.spec.TaskState;

@Path("/greeting")
public class GreetingResource {

    @Inject
    HelloWorldAgentSec helloWorldAgent;

    @GET
    public String hello() {

        System.out.println(helloWorldAgent.greeting("hi"));

        return "hello";

    }

    @Inject
    HelloWorldAgentHil helloWorldAgentHil;

    private String contextId;
    private String taskId;


    @Path("/hil")
    @GET
    public String hil() {
        try {
            String output = helloWorldAgentHil.greeting(null, null, "Hi");
            return output;
        } catch(A2ATaskInterruptedException e) {
            if (e.state() == TaskState.TASK_STATE_INPUT_REQUIRED) {
                contextId = e.contextId();
                taskId = e.taskId();
                return e.getMessage();
            }

            throw e;
        }
    }

    @Path("/continue")
    @GET
    public String continueHello() {
        return helloWorldAgentHil.greeting(contextId, taskId, "upper");
    }
}
