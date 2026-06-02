package com.carlesarnal.agents.orchestrator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.apicurio.registry.client.RegistryClientFactory;
import io.apicurio.registry.client.common.RegistryClientOptions;
import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.models.SearchedArtifact;
import io.apicurio.registry.rest.client.models.ArtifactSearchResults;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@ApplicationScoped
public class RegistryDiscoveryService {

    private static final Logger LOG = Logger.getLogger(RegistryDiscoveryService.class);

    private final RegistryClient registryClient;
    private final String groupId;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Inject
    public RegistryDiscoveryService(
            @ConfigProperty(name = "registry.url", defaultValue = "http://localhost:8080") String registryUrl,
            @ConfigProperty(name = "registry.group-id", defaultValue = "a2a-agents") String groupId) {
        this.registryClient = RegistryClientFactory.create(
                RegistryClientOptions.create(registryUrl + "/apis/registry/v3"));
        this.groupId = groupId;
    }

    public String discoverAndDelegate(String userRequest) {
        try {
            String agentUrl = discoverAgent();
            if (agentUrl == null) {
                return "No agents found in the registry.";
            }

            LOG.infof("Discovered agent at: %s", agentUrl);
            return delegateViaA2A(agentUrl, userRequest);
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

        SearchedArtifact firstAgent = results.getArtifacts().get(0);
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

    private String delegateViaA2A(String agentUrl, String message) throws Exception {
        ObjectNode textPart = mapper.createObjectNode();
        textPart.put("kind", "text");
        textPart.put("text", message);

        ArrayNode parts = mapper.createArrayNode();
        parts.add(textPart);

        ObjectNode msg = mapper.createObjectNode();
        msg.put("role", "user");
        msg.set("parts", parts);
        msg.put("messageId", UUID.randomUUID().toString());
        msg.put("kind", "message");

        ObjectNode params = mapper.createObjectNode();
        params.set("message", msg);

        ObjectNode request = mapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", UUID.randomUUID().toString());
        request.put("method", "message/send");
        request.set("params", params);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(agentUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(request)))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        JsonNode responseJson = mapper.readTree(response.body());
        JsonNode result = responseJson.get("result");
        if (result != null && result.has("artifacts")) {
            JsonNode artifacts = result.get("artifacts");
            StringBuilder sb = new StringBuilder();
            for (JsonNode artifact : artifacts) {
                for (JsonNode part : artifact.get("parts")) {
                    if ("text".equals(part.path("kind").asText())) {
                        sb.append(part.get("text").asText());
                    }
                }
            }
            return sb.toString();
        }

        return "Agent responded: " + response.body();
    }
}
