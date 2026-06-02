package com.carlesarnal.agents.orchestrator;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface AgentSelectorAiService {

    @SystemMessage("""
            You are an agent router. Given a user request and a list of available agents
            with their skills, respond with ONLY the artifactId of the best matching agent.

            Do not explain your choice. Do not add any other text.
            Respond with exactly one artifactId, nothing else.
            """)
    String selectAgent(@UserMessage String prompt);
}
