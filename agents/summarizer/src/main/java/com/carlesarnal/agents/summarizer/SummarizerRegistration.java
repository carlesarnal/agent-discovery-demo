package com.carlesarnal.agents.summarizer;

import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.a2a.runtime.apicurio.PublishToAgentRegistry;

@PublishToAgentRegistry(
        name = "Summarizer Agent",
        description = "Summarizes long text into concise 2-3 sentence abstracts using an LLM",
        version = "1.0.0",
        skills = {
                @PublishToAgentRegistry.Skill(
                        id = "summarize-text",
                        name = "Text Summarization",
                        description = "Produces a concise summary of input text in 2-3 sentences")
        })
@RegisterAiService
public interface SummarizerRegistration {
    String process(String input);
}
