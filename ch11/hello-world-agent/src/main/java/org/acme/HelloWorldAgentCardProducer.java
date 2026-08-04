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

@ApplicationScoped
public class HelloWorldAgentCardProducer {

    @ConfigProperty(name = "agent.url")
    String agentUrl;

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
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
