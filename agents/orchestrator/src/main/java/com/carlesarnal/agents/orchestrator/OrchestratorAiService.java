package com.carlesarnal.agents.orchestrator;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface OrchestratorAiService {

    @SystemMessage("""
            You are an orchestrator agent. Your job is to fulfill user requests by
            discovering and delegating to specialized A2A agents registered in a
            registry.

            When you receive a request:
            1. Use the searchA2AAgents tool to find agents that can handle the task
            2. Use getAgentCardDetails to inspect the best matching agent
            3. Use delegateToA2AAgent to send the task to that agent
            4. Return the result from the delegated agent to the user

            Always search for agents first. Never try to answer directly — delegate
            to the most appropriate agent.
            """)
    String orchestrate(@UserMessage String request);
}
