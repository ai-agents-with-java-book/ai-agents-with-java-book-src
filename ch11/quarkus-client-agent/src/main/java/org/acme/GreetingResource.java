package org.acme;

import dev.langchain4j.agentic.a2a.A2ATaskInterruptedException;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.a2aproject.sdk.spec.TaskState;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/greeting")
public class GreetingResource {
    @Inject HelloWorldAgentSec helloWorldAgent;
    @Inject HelloWorldAgentHil helloWorldAgentHil;
    @Inject SecurityIdentity identity;
    @Inject PendingReplies pendingReplies;

    public record Input(String text) {}
    public record Reply(String handle, String prompt, String result) {}

    @GET
    public String hello() {
        System.out.println(helloWorldAgent.greeting("hi"));
        return "hello";
    }

    @POST
    @Path("/hil")
    @Authenticated
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Reply start(Input input) {
        return invoke(owner(), null, null, text(input));
    }

    @POST
    @Path("/hil/{handle}")
    @Authenticated
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Reply resume(@PathParam("handle") String handle, Input input) {
        String owner = owner();
        String choice = choice(input);
        PendingReplies.Pending pending = pendingReplies.claim(handle, owner);
        try {
            Reply reply = invoke(owner, pending.contextId, pending.taskId, choice);
            pending.state.set(PendingReplies.State.CONSUMED);
            return reply;
        } catch (RuntimeException failure) {
            pending.state.set(PendingReplies.State.UNCERTAIN);
            throw new WebApplicationException(
                    "Remote outcome unknown; do not retry this handle", 502);
        }
    }

    private Reply invoke(String owner, String contextId, String taskId, String text) {
        try {
            return new Reply(null, null, helloWorldAgentHil.greeting(contextId, taskId, text));
        } catch (A2ATaskInterruptedException pause) {
            if (pause.state() != TaskState.TASK_STATE_INPUT_REQUIRED) {
                throw new WebApplicationException("Remote task did not complete", 502);
            }
            String handle = pendingReplies.remember(owner, pause.contextId(), pause.taskId());
            return new Reply(handle, pause.reason(), null);
        }
    }

    private String owner() {
        if (identity.isAnonymous()
                || !(identity.getPrincipal() instanceof JsonWebToken token)
                || token.getIssuer() == null || token.getIssuer().isBlank()
                || token.getSubject() == null || token.getSubject().isBlank()) {
            throw new ForbiddenException();
        }
        return token.getIssuer() + "\n" + token.getSubject();
    }

    private static String text(Input input) {
        if (input == null || input.text() == null || input.text().isBlank()) {
            throw new BadRequestException("Text is required");
        }
        return input.text().trim();
    }

    private static String choice(Input input) {
        String choice = text(input);
        if (!choice.equalsIgnoreCase("upper") && !choice.equalsIgnoreCase("lower")) {
            throw new BadRequestException("Choose upper or lower");
        }
        return choice;
    }
}
