package com.carlesarnal.agents.summarizer;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface SummarizerAgent {

    @SystemMessage("""
            You are a summarization assistant. When given text, produce a concise
            summary in 2-3 sentences. Focus on the key points and main ideas.
            Be direct and factual. Format your response in plain text.
            """)
    String summarize(@UserMessage String text);
}
