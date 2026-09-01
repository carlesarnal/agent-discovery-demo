package com.carlesarnal.agents.orchestrator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import io.apicurio.registry.client.RegistryClientFactory;
import io.apicurio.registry.client.common.RegistryClientOptions;
import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.models.SearchedArtifact;
import io.apicurio.registry.rest.client.models.ArtifactSearchResults;
import io.quarkiverse.langchain4j.a2a.runtime.apicurio.ApicurioAgentsRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Discovers A2A agents published in Apicurio Registry (via {@code @PublishToAgentRegistry})
 * and delegates a user request to the best matching one.
 *
 * <p>Discovery first goes through {@link ApicurioAgentsRegistry} — the
 * {@code AgentsRegistry} SPI implementation provided by the
 * {@code quarkus-langchain4j-a2a-apicurio-registry} extension. Because that SPI only exposes
 * {@code name}/{@code description} (no skills), the raw {@link RegistryClient} is additionally
 * used to read each Agent Card's {@code skills} so the LLM can match on capability, not just name.
 *
 * <p>Delegation to the chosen agent is done with {@link AgenticServices#a2aBuilder(String)} —
 * the A2A client from {@code langchain4j-agentic-a2a} — instead of a hand-rolled JSON-RPC call.
 */
@ApplicationScoped
public class RegistryDiscoveryService {

    private static final Logger LOG = Logger.getLogger(RegistryDiscoveryService.class);

    // Words too generic to be useful as a skill-search keyword.
    private static final Set<String> STOPWORDS = Set.of(
            "the", "a", "an", "to", "of", "for", "in", "on", "and", "or", "please",
            "this", "that", "is", "are", "with", "from", "into", "what", "how",
            "can", "you", "me", "it", "its", "as", "at", "by");

    // Naive stemming: compare word prefixes so "translate"/"translation"/"translator"
    // and "summarize"/"summarization" match each other without a real NLP dependency.
    private static final int STEM_PREFIX_LEN = 6;

    private final ApicurioAgentsRegistry agentsRegistry;
    private final RegistryClient registryClient;
    private final String groupId;
    private final ObjectMapper mapper = new ObjectMapper();

    private final AgentSelectorAiService agentSelector;

    @Inject
    public RegistryDiscoveryService(
            @ConfigProperty(name = "registry.url", defaultValue = "http://localhost:8080") String registryUrl,
            @ConfigProperty(name = "registry.group-id", defaultValue = "a2a-agents") String groupId,
            ApicurioAgentsRegistry agentsRegistry,
            AgentSelectorAiService agentSelector) {
        this.agentsRegistry = agentsRegistry;
        this.agentSelector = agentSelector;
        this.registryClient = RegistryClientFactory.create(
                RegistryClientOptions.create(registryUrl + "/apis/registry/v3"));
        this.groupId = groupId;
    }

    public record StepEvent(String step, String status, String detail, String payload) {
        public StepEvent(String step, String status, String detail) {
            this(step, status, detail, null);
        }
    }

    private record AgentCandidate(String name, String artifactId, String url, String cardJson, String skills) {}

    public String discoverAndDelegate(String userRequest) {
        return discoverAndDelegate(userRequest, e -> {});
    }

    public String discoverAndDelegate(String userRequest, Consumer<StepEvent> onStep) {
        try {
            onStep.accept(new StepEvent("search", "running",
                    "Discovering agents via ApicurioAgentsRegistry (AgentsRegistry SPI)..."));

            // Preferred path: the AgentsRegistry SPI bean provided by
            // quarkus-langchain4j-a2a-apicurio-registry. Falls back to a direct registry
            // search below to enrich candidates with skills for LLM matching.
            Map<String, ?> registeredAgents = agentsRegistry.allAgents();
            LOG.infof("ApicurioAgentsRegistry reports %d agent(s) via the AgentsRegistry SPI",
                    registeredAgents.size());

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

            // Read all Agent Cards (skills aren't exposed by the AgentsRegistry SPI, so we read
            // the artifact content directly via the Apicurio Registry SDK for LLM matching)
            onStep.accept(new StepEvent("inspect", "running", "Reading Agent Cards from registry..."));

            List<AgentCandidate> candidates = new ArrayList<>();
            StringBuilder llmPrompt = new StringBuilder();
            llmPrompt.append("User request: ").append(userRequest).append("\n\nAvailable agents:\n");

            for (SearchedArtifact artifact : results.getArtifacts()) {
                InputStream content = registryClient.groups().byGroupId(groupId)
                        .artifacts().byArtifactId(artifact.getArtifactId())
                        .versions().byVersionExpression("branch=latest").content().get();
                String cardJson = new String(content.readAllBytes(), StandardCharsets.UTF_8);
                JsonNode card = mapper.readTree(cardJson);

                String url = card.has("url") ? card.get("url").asText() : null;
                String name = card.has("name") ? card.get("name").asText() : artifact.getArtifactId();

                List<String> skillNames = new ArrayList<>();
                if (card.has("skills")) {
                    for (JsonNode s : card.get("skills")) {
                        skillNames.add(s.get("name").asText());
                    }
                }
                String skills = String.join(", ", skillNames);

                candidates.add(new AgentCandidate(name, artifact.getArtifactId(), url, cardJson, skills));
            }

            String allCardsJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                    candidates.stream().map(c -> {
                        ObjectNode n = mapper.createObjectNode();
                        n.put("artifactId", c.artifactId);
                        n.put("name", c.name);
                        n.put("skills", c.skills);
                        return n;
                    }).toList());
            onStep.accept(new StepEvent("inspect", "done",
                    "Read " + candidates.size() + " Agent Cards: " +
                    candidates.stream().map(c -> c.name).reduce((a, b) -> a + ", " + b).orElse(""),
                    allCardsJson));

            // Search by skill. Apicurio Registry's search API only indexes flat artifact
            // metadata (name/description/labels), not the nested `skills` array inside an
            // AGENT_CARD's content — so registry-side search can't filter on capability.
            // We narrow the candidate set client-side instead, matching request keywords
            // (stemmed to a short prefix) against each card's skill names/descriptions.
            onStep.accept(new StepEvent("skills", "running",
                    "Searching " + candidates.size() + " Agent Card(s) by skill for a match..."));
            List<AgentCandidate> matchedBySkill = searchCandidatesBySkill(candidates, userRequest);
            String skillsPayload = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                    matchedBySkill.stream().map(c -> {
                        ObjectNode n = mapper.createObjectNode();
                        n.put("artifactId", c.artifactId);
                        n.put("name", c.name);
                        n.put("skills", c.skills);
                        return n;
                    }).toList());
            boolean skillMatchFound = matchedBySkill.size() < candidates.size();
            onStep.accept(new StepEvent("skills", "done",
                    skillMatchFound
                            ? "Skill search narrowed " + candidates.size() + " agent(s) down to " + matchedBySkill.size()
                                    + ": " + matchedBySkill.stream().map(c -> c.name).reduce((a, b) -> a + ", " + b).orElse("")
                            : "No skill keyword matched — keeping all " + candidates.size() + " agent(s) as candidates",
                    skillsPayload));

            for (AgentCandidate c : matchedBySkill) {
                llmPrompt.append("- artifactId: ").append(c.artifactId)
                        .append(", name: ").append(c.name)
                        .append(", skills: ").append(c.skills).append("\n");
            }

            // Use LLM to select the best agent
            onStep.accept(new StepEvent("match", "running", "Asking LLM to select the best agent for this request..."));
            String selectedId = agentSelector.selectAgent(llmPrompt.toString()).trim();
            LOG.infof("LLM selected agent: '%s'", selectedId);

            AgentCandidate chosen = matchedBySkill.stream()
                    .filter(c -> selectedId.contains(c.artifactId))
                    .findFirst()
                    .orElse(matchedBySkill.get(0));

            ObjectNode matchPayload = mapper.createObjectNode();
            matchPayload.put("llmInput", llmPrompt.toString());
            matchPayload.put("llmOutput", selectedId);
            ArrayNode candidatesArray = mapper.createArrayNode();
            for (AgentCandidate c : matchedBySkill) {
                ObjectNode n = mapper.createObjectNode();
                n.put("agent", c.name);
                n.put("artifactId", c.artifactId);
                n.put("skills", c.skills);
                n.put("selected", c == chosen);
                candidatesArray.add(n);
            }
            matchPayload.set("candidates", candidatesArray);

            onStep.accept(new StepEvent("match", "done",
                    "LLM selected: " + chosen.name + " (artifactId: " + chosen.artifactId + ") | Skills: " + chosen.skills,
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(matchPayload)));

            if (chosen.url == null) {
                onStep.accept(new StepEvent("delegate", "error", "Selected agent has no URL"));
                return "Agent Card has no URL.";
            }

            onStep.accept(new StepEvent("delegate", "running",
                    "Delegating to " + chosen.name + " via the A2A client (langchain4j-agentic-a2a) at " + chosen.url + "..."));
            String[] a2aPayloads = new String[2];
            String response = delegateViaA2A(chosen.url, chosen.name, userRequest, a2aPayloads);
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

    /**
     * Narrows the candidate agents down to those whose skills (or name) look relevant to the
     * user's request, using simple stemmed-keyword matching. Apicurio Registry's search API can
     * filter by artifact name/description (see {@code searchMcpServers} on the MCP side), but it
     * doesn't index the nested {@code skills} array inside an AGENT_CARD's content, so this
     * capability-based "search" has to happen client-side once the cards have been read.
     *
     * <p>Falls back to returning every candidate if no keyword matches anything, so the LLM
     * router always has at least the full candidate set to choose from.
     */
    private static List<AgentCandidate> searchCandidatesBySkill(List<AgentCandidate> candidates, String userRequest) {
        List<String> stems = Arrays.stream(userRequest.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(w -> w.length() >= 4 && !STOPWORDS.contains(w))
                .map(w -> w.substring(0, Math.min(STEM_PREFIX_LEN, w.length())))
                .distinct()
                .toList();

        if (stems.isEmpty()) {
            return candidates;
        }

        List<AgentCandidate> matches = candidates.stream()
                .filter(c -> {
                    String haystack = (c.name() + " " + c.skills()).toLowerCase(Locale.ROOT);
                    return stems.stream().anyMatch(haystack::contains);
                })
                .toList();

        return matches.isEmpty() ? candidates : matches;
    }

    /**
     * Delegates to a remote agent using the A2A client built by
     * {@code langchain4j-agentic-a2a}'s {@link AgenticServices#a2aBuilder(String)} — the same
     * mechanism {@link ApicurioAgentsRegistry} uses internally to turn a discovered Agent Card
     * into a callable agent — instead of constructing the A2A JSON-RPC request by hand.
     */
    private String delegateViaA2A(String agentUrl, String agentName, String message, String[] payloads) {
        if (payloads != null) {
            payloads[0] = mapper.createObjectNode().put("input", message).toString();
        }

        UntypedAgent agent = AgenticServices.a2aBuilder(agentUrl)
                .inputKeys("input")
                .outputKey(agentName)
                .build();

        Object result = agent.invoke(Map.of("input", message));
        String response = result != null ? result.toString() : "";

        if (payloads != null) {
            payloads[1] = mapper.createObjectNode().put("response", response).toString();
        }
        return response;
    }
}
