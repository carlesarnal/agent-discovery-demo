package com.carlesarnal.agents.summarizer;

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
        String url = baseUrl + ":" + httpPort;
        return AgentCard.builder()
                .name("Summarizer Agent")
                .description("Summarizes long text into concise 2-3 sentence abstracts using an LLM")
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
                                        .id("summarize-text")
                                        .name("Text Summarization")
                                        .description("Produces a concise summary of input text in 2-3 sentences")
                                        .tags(List.of("summarization", "text", "nlp"))
                                        .examples(List.of(
                                                "Summarize this article about Kubernetes",
                                                "Give me a brief summary of the following text"))
                                        .build()))
                .build();
    }
}
