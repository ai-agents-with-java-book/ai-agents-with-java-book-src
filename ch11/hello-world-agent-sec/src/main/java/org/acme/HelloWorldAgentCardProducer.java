package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.a2aproject.sdk.spec.ClientCredentialsOAuthFlow;
import org.a2aproject.sdk.spec.OAuth2SecurityScheme;
import org.a2aproject.sdk.spec.OAuthFlows;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.eclipse.microprofile.config.inject.ConfigProperty;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.a2aproject.sdk.spec.SecurityRequirement;

@ApplicationScoped
public class HelloWorldAgentCardProducer {

    @ConfigProperty(name = "agent.url")
    String agentUrl;

    @ConfigProperty(name = "quarkus.keycloak.devservices.port")
    private int keycloakPort;

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        ClientCredentialsOAuthFlow clientCredentialsOAuthFlow = new ClientCredentialsOAuthFlow(
            null,
            Map.of("openid", "openid", "profile", "profile"),
            "http://localhost:" + keycloakPort + "/realms/quarkus/protocol/openid-connect/token");
        OAuth2SecurityScheme securityScheme = OAuth2SecurityScheme.builder()
            .flows(OAuthFlows.builder().clientCredentials(clientCredentialsOAuthFlow).build())
            .build();

        return AgentCard.builder()
            .name("Hello World Agent")
            .description("Just a hello world agent")
            .supportedInterfaces(List.of(
                new AgentInterface(TransportProtocol.JSONRPC.asString(), agentUrl)))
            .version("1.0.0")
            .documentationUrl("http://example.com/docs")
            .capabilities(AgentCapabilities.builder()
                .streaming(true)
                .pushNotifications(false)
                .build())
            .defaultInputModes(Collections.singletonList("text"))
            .defaultOutputModes(Collections.singletonList("text"))
            .securitySchemes(Map.of("oauth2", securityScheme))
            .securityRequirements(List.of(
                SecurityRequirement.builder()
                    .scheme("oauth2", List.of("openid", "profile"))
                    .build()))
            .skills(Collections.singletonList(AgentSkill.builder()
                .id("hello_world")
                .name("Returns hello world")
                .description("just returns hello world")
                .tags(Collections.singletonList("hello world"))
                .examples(List.of("hi", "hello world"))
                .build()))
            .build();
    }

}
