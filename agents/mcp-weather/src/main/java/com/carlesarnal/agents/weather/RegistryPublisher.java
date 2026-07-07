package com.carlesarnal.agents.weather;

import io.apicurio.registry.client.RegistryClientFactory;
import io.apicurio.registry.client.common.RegistryClientOptions;
import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.models.CreateArtifact;
import io.apicurio.registry.rest.client.models.CreateVersion;
import io.apicurio.registry.rest.client.models.Labels;
import io.apicurio.registry.rest.client.models.VersionContent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RegistryPublisher {

    private static final Logger LOG = Logger.getLogger(RegistryPublisher.class);

    @Inject
    @ConfigProperty(name = "registry.url", defaultValue = "http://localhost:8080")
    String registryUrl;

    @Inject
    @ConfigProperty(name = "registry.group-id", defaultValue = "mcp-servers")
    String groupId;

    @Inject
    @ConfigProperty(name = "mcp.server.url", defaultValue = "http://localhost:10040")
    String mcpServerUrl;

    void onStart(@Observes StartupEvent ev) {
        try {
            RegistryClient client = RegistryClientFactory.create(
                    RegistryClientOptions.create(registryUrl + "/apis/registry/v3"));

            String toolJson = """
                    {
                      "name": "getWeather",
                      "description": "Get current weather for a city in Europe. Returns temperature, conditions, and wind.",
                      "inputSchema": {
                        "type": "object",
                        "properties": {
                          "city": {
                            "type": "string",
                            "description": "City name (e.g., Amsterdam, London, Paris)"
                          }
                        },
                        "required": ["city"]
                      }
                    }
                    """;

            VersionContent content = new VersionContent();
            content.setContent(toolJson);
            content.setContentType("application/json");

            Labels labels = new Labels();
            labels.getAdditionalData().put("mcp-server-url", mcpServerUrl + "/mcp/sse");
            labels.getAdditionalData().put("mcp-transport-type", "sse");

            CreateVersion version = new CreateVersion();
            version.setContent(content);

            CreateArtifact createArtifact = new CreateArtifact();
            createArtifact.setArtifactId("weather-mcp-server");
            createArtifact.setArtifactType("MCP_TOOL");
            createArtifact.setName("Weather MCP Server");
            createArtifact.setDescription("Provides current weather data for European cities via MCP");
            createArtifact.setFirstVersion(version);

            client.groups()
                    .byGroupId(groupId)
                    .artifacts()
                    .post(createArtifact);

            // Set labels on the version
            client.groups()
                    .byGroupId(groupId)
                    .artifacts()
                    .byArtifactId("weather-mcp-server")
                    .versions()
                    .byVersionExpression("branch=latest")
                    .put(labels);

            LOG.infof("Published MCP_TOOL 'Weather MCP Server' to registry group '%s' with URL %s", groupId, mcpServerUrl);
        } catch (Exception e) {
            LOG.warn("Could not publish MCP tool to registry", e);
        }
    }
}
