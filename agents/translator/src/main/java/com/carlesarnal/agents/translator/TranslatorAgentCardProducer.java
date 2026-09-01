package com.carlesarnal.agents.translator;

import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class TranslatorAgentCardProducer {

    @Inject
    @ConfigProperty(name = "quarkus.http.port")
    private int httpPort;

    @Inject
    @ConfigProperty(name = "agent.base-url", defaultValue = "http://localhost")
    private String baseUrl;

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        String url = baseUrl + ":" + httpPort;
        return AgentCard.builder()
                .name("Translator Agent")
                .description("Translates text between languages using an LLM")
                .url(url)
                .version("1.0.0")
                .capabilities(
                        AgentCapabilities.builder()
                                .streaming(false)
                                .pushNotifications(false)
                                .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .supportedInterfaces(Collections.singletonList(new AgentInterface("JSONRPC", url)))
                .skills(
                        Collections.singletonList(
                                AgentSkill.builder()
                                        .id("translate-text")
                                        .name("Text Translation")
                                        .description("Translates text from one language to another")
                                        .tags(List.of("translation", "language", "nlp"))
                                        .examples(List.of(
                                                "Translate to French: Hello world",
                                                "Translate this to Spanish: The weather is nice"))
                                        .build()))
                .build();
    }
}
