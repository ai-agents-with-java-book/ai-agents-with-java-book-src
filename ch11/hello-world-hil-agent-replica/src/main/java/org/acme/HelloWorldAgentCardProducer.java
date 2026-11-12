package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.a2aproject.sdk.spec.ClientCredentialsOAuthFlow;
import org.a2aproject.sdk.spec.OAuth2SecurityScheme;
import org.a2aproject.sdk.spec.OAuthFlows;
import org.a2aproject.sdk.spec.SecurityRequirement;

@ApplicationScoped
public class HelloWorldAgentCardProducer {

    @ConfigProperty(name = "agent.url")
    String agentUrl;

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String issuer;

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        var flow = new ClientCredentialsOAuthFlow(null, Map.of("openid", "Identity"),
                issuer + "/protocol/openid-connect/token");
        var security = OAuth2SecurityScheme.builder()
                .flows(OAuthFlows.builder().clientCredentials(flow).build()).build();
        return AgentCard.builder()
            .name("Hello World HIL Agent")
            .description("A hello world agent with Human-in-the-Loop: pauses before transforming "
                + "the input text and asks the user to choose upper or lower case.")
            .supportedInterfaces(List.of(
                new AgentInterface(TransportProtocol.JSONRPC.asString(), agentUrl)))
            .version("1.0.0")
            .documentationUrl("http://example.com/docs")
            .capabilities(AgentCapabilities.builder()
                .streaming(true)
                .pushNotifications(false)
                .build())
            .securitySchemes(Map.of("oauth2", security))
            .securityRequirements(List.of(SecurityRequirement.builder()
                .scheme("oauth2", List.of("openid")).build()))
            .defaultInputModes(Collections.singletonList("text"))
            .defaultOutputModes(Collections.singletonList("text"))
            .skills(Collections.singletonList(AgentSkill.builder()
                .id("text_case_transform")
                .name("Text case transformation with human approval")
                .description("Receives text, pauses to ask the user whether to transform it "
                    + "to upper or lower case, then returns the transformed result.")
                .tags(List.of("hello world", "human-in-the-loop", "text"))
                .examples(List.of("hi", "hello world", "tell me a greeting"))
                .build()))
            .build();
    }

}
