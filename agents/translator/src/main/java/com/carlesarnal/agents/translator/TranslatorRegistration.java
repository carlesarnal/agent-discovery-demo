package com.carlesarnal.agents.translator;

import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.a2a.runtime.apicurio.PublishToAgentRegistry;

@PublishToAgentRegistry(
        name = "Translator Agent",
        description = "Translates text between languages using an LLM",
        version = "1.0.0",
        skills = {
                @PublishToAgentRegistry.Skill(
                        id = "translate-text",
                        name = "Text Translation",
                        description = "Translates text from one language to another")
        })
@RegisterAiService
public interface TranslatorRegistration {
    String process(String input);
}
