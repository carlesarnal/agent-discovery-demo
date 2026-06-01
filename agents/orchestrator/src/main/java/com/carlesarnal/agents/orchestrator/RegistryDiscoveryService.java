package com.carlesarnal.agents.orchestrator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.a2a.client.A2AClient;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;
import io.apicurio.registry.client.RegistryClient;
import io.apicurio.registry.rest.client.models.ArtifactMetaData;
import io.apicurio.registry.rest.client.models.ArtifactSearchResults;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RegistryDiscoveryService {

    private static final Logger LOG = Logger.getLogger(RegistryDiscoveryService.class);

    private final RegistryClient registryClient;
    private final String groupId;
    private final ObjectMapper mapper = new ObjectMapper();

    @Inject
    public RegistryDiscoveryService(
            @ConfigProperty(name = "registry.url", defaultValue = "http://localhost:8080") String registryUrl,
            @ConfigProperty(name = "registry.group-id", defaultValue = "a2a-agents") String groupId) {
        this.registryClient = RegistryClient.create(registryUrl + "/apis/registry/v3");
        this.groupId = groupId;
    }

    public String discoverAndDelegate(String userRequest) {
        try {
            String agentUrl = discoverAgent();
            if (agentUrl == null) {
                return "No agents found in the registry.";
            }

            LOG.infof("Discovered agent at: %s", agentUrl);
            return delegateToAgent(agentUrl, userRequest);
        } catch (Exception e) {
            LOG.error("Orchestration failed", e);
            return "Error: " + e.getMessage();
        }
    }

    private String discoverAgent() throws Exception {
        ArtifactSearchResults results = registryClient
                .groups()
                .byGroupId(groupId)
                .artifacts()
                .get();

        if (results.getArtifacts() == null || results.getArtifacts().isEmpty()) {
            LOG.warn("No artifacts found in registry group: " + groupId);
            return null;
        }

        ArtifactMetaData firstAgent = results.getArtifacts().get(0);
        LOG.infof("Found agent: %s (%s)", firstAgent.getName(), firstAgent.getArtifactId());

        return getAgentUrl(firstAgent.getArtifactId());
    }

    private String getAgentUrl(String artifactId) throws Exception {
        InputStream content = registryClient
                .groups()
                .byGroupId(groupId)
                .artifacts()
                .byArtifactId(artifactId)
                .versions()
                .byVersionExpression("branch=latest")
                .content()
                .get();

        String json = new String(content.readAllBytes(), StandardCharsets.UTF_8);
        JsonNode card = mapper.readTree(json);
        return card.has("url") ? card.get("url").asText() : null;
    }

    private String delegateToAgent(String agentUrl, String message) throws Exception {
        A2AClient client = A2AClient.builder()
                .url(agentUrl)
                .build();

        MessageSendParams params = MessageSendParams.builder()
                .message(MessageSendParams.Message.builder()
                        .role("user")
                        .parts(List.of(new TextPart(message, null)))
                        .messageId(UUID.randomUUID().toString())
                        .build())
                .build();

        Task task = client.sendMessage(params);

        if (task.getArtifacts() != null && !task.getArtifacts().isEmpty()) {
            StringBuilder result = new StringBuilder();
            for (var artifact : task.getArtifacts()) {
                for (Part<?> part : artifact.parts()) {
                    if (part instanceof TextPart textPart) {
                        result.append(textPart.getText());
                    }
                }
            }
            return result.toString();
        }

        return "Agent completed but returned no artifacts.";
    }
}
