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
            You are an intelligent orchestrator with access to MCP tools.

            When the user asks a question:
            1. Search for relevant MCP servers using searchMcpServers
            2. Connect to the found server using connectMcpServer with the groupId
               and artifactId from the search results (e.g. groupId="mcp-servers",
               artifactId="weather-mcp-server")
            3. Use the connected tools to answer the question

            Always use the tools. Be concise.
            """)
    String chat(@UserMessage String message);
}
