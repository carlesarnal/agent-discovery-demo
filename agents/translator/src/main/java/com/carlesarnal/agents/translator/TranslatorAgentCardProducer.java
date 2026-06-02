package com.carlesarnal.agents.translator;

import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
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
        return new AgentCard.Builder()
                .name("Translator Agent")
                .description("Translates text between languages using an LLM")
                .url(baseUrl + ":" + httpPort)
                .version("1.0.0")
                .capabilities(
                        new AgentCapabilities.Builder()
                                .streaming(false)
                                .pushNotifications(false)
                                .stateTransitionHistory(false)
                                .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(
                        Collections.singletonList(
                                new AgentSkill.Builder()
                                        .id("translate-text")
                                        .name("Text Translation")
                                        .description("Translates text from one language to another")
                                        .tags(List.of("translation", "language", "nlp"))
                                        .examples(List.of(
                                                "Translate to French: Hello world",
                                                "Translate this to Spanish: The weather is nice"))
                                        .build()))
                .protocolVersion("0.3.0")
                .build();
    }
}
