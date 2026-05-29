package com.carlesarnal.agents.summarizer;

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
public class SummarizerAgentCardProducer {

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
                .name("Summarizer Agent")
                .description("Summarizes long text into concise 2-3 sentence abstracts using an LLM")
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
                                        .id("summarize-text")
                                        .name("Text Summarization")
                                        .description("Produces a concise summary of input text in 2-3 sentences")
                                        .tags(List.of("summarization", "text", "nlp"))
                                        .examples(List.of(
                                                "Summarize this article about Kubernetes",
                                                "Give me a brief summary of the following text"))
                                        .build()))
                .protocolVersion("0.3.0")
                .build();
    }
}
