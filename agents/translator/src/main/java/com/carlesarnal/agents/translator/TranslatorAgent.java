package com.carlesarnal.agents.translator;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface TranslatorAgent {

    @SystemMessage("""
            You are a translation assistant. When given text with a target language,
            translate it accurately. Preserve the meaning, tone, and formatting.
            Return only the translated text, nothing else.
            """)
    String translate(@UserMessage String text);
}
