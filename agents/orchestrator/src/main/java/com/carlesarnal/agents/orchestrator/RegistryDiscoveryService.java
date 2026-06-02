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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

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

    public record StepEvent(String step, String status, String detail, String payload) {
        public StepEvent(String step, String status, String detail) {
            this(step, status, detail, null);
        }
    }

    public String discoverAndDelegate(String userRequest) {
        return discoverAndDelegate(userRequest, e -> {});
    }

    public String discoverAndDelegate(String userRequest, Consumer<StepEvent> onStep) {
        try {
            onStep.accept(new StepEvent("search", "running", "Querying registry for agents in group '" + groupId + "'..."));
            ArtifactSearchResults results = registryClient.groups().byGroupId(groupId).artifacts().get();

            if (results.getArtifacts() == null || results.getArtifacts().isEmpty()) {
                onStep.accept(new StepEvent("search", "error", "No agents found in registry"));
                return "No agents found in the registry.";
            }

            List<String> agentNames = new ArrayList<>();
            for (SearchedArtifact a : results.getArtifacts()) {
                agentNames.add(a.getName() != null ? a.getName() : a.getArtifactId());
            }
            String agentsJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                    results.getArtifacts().stream().map(a -> {
                        ObjectNode n = mapper.createObjectNode();
                        n.put("artifactId", a.getArtifactId());
                        n.put("name", a.getName());
                        n.put("type", a.getArtifactType());
                        n.put("groupId", a.getGroupId());
                        return n;
                    }).toList());
            onStep.accept(new StepEvent("search", "done", "Found " + agentNames.size() + " agents: " + String.join(", ", agentNames), agentsJson));

            SearchedArtifact chosen = results.getArtifacts().get(0);
            String chosenName = chosen.getName() != null ? chosen.getName() : chosen.getArtifactId();

            onStep.accept(new StepEvent("inspect", "running", "Reading Agent Card for '" + chosenName + "'..."));
            InputStream content = registryClient.groups().byGroupId(groupId)
                    .artifacts().byArtifactId(chosen.getArtifactId())
                    .versions().byVersionExpression("branch=latest").content().get();
            String cardJson = new String(content.readAllBytes(), StandardCharsets.UTF_8);
            JsonNode card = mapper.readTree(cardJson);

            String agentUrl = card.has("url") ? card.get("url").asText() : null;
            String skills = "";
            if (card.has("skills")) {
                List<String> skillNames = new ArrayList<>();
                for (JsonNode s : card.get("skills")) {
                    skillNames.add(s.get("name").asText());
                }
                skills = String.join(", ", skillNames);
            }
            onStep.accept(new StepEvent("inspect", "done", "Agent: " + chosenName + " | Skills: " + skills + " | URL: " + agentUrl, cardJson));

            if (agentUrl == null) {
                onStep.accept(new StepEvent("delegate", "error", "Agent Card has no URL"));
                return "Agent Card has no URL.";
            }

            onStep.accept(new StepEvent("delegate", "running", "Sending task via A2A Protocol to " + agentUrl + "..."));
            String[] a2aPayloads = new String[2];
            String response = delegateViaA2A(agentUrl, userRequest, a2aPayloads);
            String delegatePayload = mapper.createObjectNode()
                    .put("request", a2aPayloads[0])
                    .put("response", a2aPayloads[1])
                    .toString();
            onStep.accept(new StepEvent("delegate", "done", "Response received from " + chosenName, delegatePayload));

            onStep.accept(new StepEvent("result", "done", response));
            return response;
        } catch (Exception e) {
            LOG.error("Orchestration failed", e);
            onStep.accept(new StepEvent("error", "error", e.getMessage()));
            return "Error: " + e.getMessage();
        }
    }

    private String delegateViaA2A(String agentUrl, String message, String[] payloads) throws Exception {
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

        String requestBody = mapper.writeValueAsString(request);
        if (payloads != null) payloads[0] = requestBody;

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(agentUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (payloads != null) payloads[1] = response.body();

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
