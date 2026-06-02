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
            onStep.accept(new StepEvent("search", "done",
                    "Found " + agentNames.size() + " agents: " + String.join(", ", agentNames), agentsJson));

            // Read all Agent Cards and pick the best match
            onStep.accept(new StepEvent("inspect", "running", "Reading Agent Cards and matching skills to request..."));

            record AgentCandidate(String name, String artifactId, String url, String cardJson, int score, String skills) {}

            List<AgentCandidate> candidates = new ArrayList<>();
            for (SearchedArtifact artifact : results.getArtifacts()) {
                InputStream content = registryClient.groups().byGroupId(groupId)
                        .artifacts().byArtifactId(artifact.getArtifactId())
                        .versions().byVersionExpression("branch=latest").content().get();
                String cardJson = new String(content.readAllBytes(), StandardCharsets.UTF_8);
                JsonNode card = mapper.readTree(cardJson);

                String url = card.has("url") ? card.get("url").asText() : null;
                String name = card.has("name") ? card.get("name").asText() : artifact.getArtifactId();

                List<String> skillNames = new ArrayList<>();
                List<String> skillDescriptions = new ArrayList<>();
                if (card.has("skills")) {
                    for (JsonNode s : card.get("skills")) {
                        skillNames.add(s.get("name").asText());
                        if (s.has("description")) skillDescriptions.add(s.get("description").asText());
                        if (s.has("tags")) {
                            for (JsonNode tag : s.get("tags")) skillDescriptions.add(tag.asText());
                        }
                    }
                }
                String description = card.has("description") ? card.get("description").asText() : "";

                int score = computeMatchScore(userRequest, name, description, skillNames, skillDescriptions);
                candidates.add(new AgentCandidate(name, artifact.getArtifactId(), url, cardJson, score,
                        String.join(", ", skillNames)));
            }

            candidates.sort((a, b) -> Integer.compare(b.score, a.score));
            AgentCandidate chosen = candidates.get(0);

            StringBuilder matchDetail = new StringBuilder();
            for (AgentCandidate c : candidates) {
                String marker = c == chosen ? " >>> SELECTED" : "";
                matchDetail.append(String.format("%s (score: %d, skills: %s)%s\n", c.name, c.score, c.skills, marker));
            }

            onStep.accept(new StepEvent("inspect", "done",
                    "Selected: " + chosen.name + " (score: " + chosen.score + ") | Skills: " + chosen.skills,
                    chosen.cardJson));

            onStep.accept(new StepEvent("match", "done", matchDetail.toString().trim(),
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                            candidates.stream().map(c -> {
                                ObjectNode n = mapper.createObjectNode();
                                n.put("agent", c.name);
                                n.put("score", c.score);
                                n.put("skills", c.skills);
                                n.put("selected", c == chosen);
                                return n;
                            }).toList())));

            if (chosen.url == null) {
                onStep.accept(new StepEvent("delegate", "error", "Selected agent has no URL"));
                return "Agent Card has no URL.";
            }

            onStep.accept(new StepEvent("delegate", "running",
                    "Delegating to " + chosen.name + " via A2A Protocol at " + chosen.url + "..."));
            String[] a2aPayloads = new String[2];
            String response = delegateViaA2A(chosen.url, userRequest, a2aPayloads);
            String delegatePayload = mapper.createObjectNode()
                    .put("request", a2aPayloads[0])
                    .put("response", a2aPayloads[1])
                    .toString();
            onStep.accept(new StepEvent("delegate", "done",
                    "Response received from " + chosen.name, delegatePayload));

            onStep.accept(new StepEvent("result", "done", response));
            return response;
        } catch (Exception e) {
            LOG.error("Orchestration failed", e);
            onStep.accept(new StepEvent("error", "error", e.getMessage()));
            return "Error: " + e.getMessage();
        }
    }

    private int computeMatchScore(String request, String agentName, String description,
                                  List<String> skillNames, List<String> skillDescriptions) {
        String lower = request.toLowerCase();
        int score = 0;

        for (String skill : skillNames) {
            for (String word : skill.toLowerCase().split("\\s+")) {
                if (word.length() > 3 && lower.contains(word)) score += 10;
            }
        }
        for (String desc : skillDescriptions) {
            for (String word : desc.toLowerCase().split("\\s+")) {
                if (word.length() > 3 && lower.contains(word)) score += 5;
            }
        }
        for (String word : description.toLowerCase().split("\\s+")) {
            if (word.length() > 3 && lower.contains(word)) score += 3;
        }
        for (String word : agentName.toLowerCase().split("\\s+")) {
            if (word.length() > 3 && lower.contains(word)) score += 8;
        }

        return score;
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
