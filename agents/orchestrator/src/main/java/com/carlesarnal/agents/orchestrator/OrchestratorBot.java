package com.carlesarnal.agents.orchestrator;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.mcp.runtime.apicurio.ApicurioRegistryMcpTools;
import io.quarkiverse.langchain4j.ToolBox;
import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService(tools = { ApicurioRegistryMcpTools.class })
@ApplicationScoped
public interface OrchestratorBot {

    @McpToolBox
    @SystemMessage("""
            You are an intelligent orchestrator. You have access to two types of capabilities:

            1. MCP TOOLS: You can search for MCP servers in the registry, connect to them,
               and use their tools. Use searchMcpServers to find available tools, then
               connectMcpServer to connect, then use the tools directly.

            2. A2A AGENTS: The user may also ask you to list available A2A agents.
               When asked about agents, describe what you know from the system.

            For weather questions, search for MCP servers with "weather" and use the weather tool.
            For summarization or translation, tell the user to use the orchestrator dashboard
            which handles A2A agent delegation.

            Be concise in your responses.
            """)
    String chat(@UserMessage String message);
}
