package com.carlesarnal.agents.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import io.apicurio.registry.client.RegistryClientFactory;
import io.apicurio.registry.client.common.RegistryClientOptions;
import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.models.Labels;
import io.apicurio.registry.rest.client.models.SearchedArtifact;
import io.apicurio.registry.rest.client.models.ArtifactSearchResults;
import io.apicurio.registry.rest.client.models.VersionMetaData;
import io.apicurio.registry.rest.client.models.VersionState;
import io.quarkiverse.langchain4j.a2a.runtime.apicurio.ApicurioAgentsRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

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
 * used, reading the {@code a2a-agent-url}/{@code a2a-agent-skills} <em>labels</em> that
 * {@code A2AAgentCardPublisher} already attaches to every {@code AGENT_CARD} artifact — these
 * come back directly on the search response, so no per-artifact content fetch is needed just to
 * list candidates and their skills.
 *
 * <p>Delegation to the chosen agent is done with {@link AgenticServices#a2aBuilder(String)} —
 * the A2A client from {@code langchain4j-agentic-a2a} — instead of a hand-rolled JSON-RPC call.
 */
@ApplicationScoped
public class RegistryDiscoveryService {

    private static final Logger LOG = Logger.getLogger(RegistryDiscoveryService.class);

    private static final String LABEL_AGENT_URL = "a2a-agent-url";
    private static final String LABEL_AGENT_SKILLS = "a2a-agent-skills";

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

    private record AgentCandidate(String name, String artifactId, String url, String skills) {}



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

            // Build candidates directly from the search results' labels — a2a-agent-url and
            // a2a-agent-skills are already attached to every AGENT_CARD artifact by
            // A2AAgentCardPublisher and returned inline by the search API, so there's no need
            // to fetch each artifact's full content just to list candidates.
            onStep.accept(new StepEvent("inspect", "running",
                    "Reading a2a-agent-url/a2a-agent-skills labels from the search results..."));

            List<AgentCandidate> candidates = new ArrayList<>();
            List<String> skipped = new ArrayList<>();
            List<String> deprecated = new ArrayList<>();
            List<AgentCandidate> deprecatedCandidates = new ArrayList<>();
            for (SearchedArtifact artifact : results.getArtifacts()) {
                Labels labels = artifact.getLabels();
                Map<String, Object> labelData = labels != null ? labels.getAdditionalData() : null;
                String url = labelData != null && labelData.get(LABEL_AGENT_URL) != null
                        ? labelData.get(LABEL_AGENT_URL).toString() : null;
                if (url == null) {
                    skipped.add(artifact.getArtifactId());
                    continue;
                }
                String skills = labelData.get(LABEL_AGENT_SKILLS) != null
                        ? labelData.get(LABEL_AGENT_SKILLS).toString() : "";
                String name = artifact.getName() != null ? artifact.getName() : artifact.getArtifactId();
                // Lifecycle governance: an agent whose latest Agent Card version is DEPRECATED
                // is no longer offered to new requests, even though its card is still readable.
                if (isLatestVersionDeprecated(artifact.getArtifactId())) {
                    deprecated.add(name);
                    deprecatedCandidates.add(new AgentCandidate(name, artifact.getArtifactId(), url, skills));
                    continue;
                }
                candidates.add(new AgentCandidate(name, artifact.getArtifactId(), url, skills));
            }

            String allCardsJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                    candidates.stream().map(c -> {
                        ObjectNode n = mapper.createObjectNode();
                        n.put("artifactId", c.artifactId);
                        n.put("name", c.name);
                        n.put("skills", c.skills);
                        return n;
                    }).toList());
            String inspectDetail = "Read " + candidates.size() + " Agent Cards from labels: " +
                    candidates.stream().map(c -> c.name).reduce((a, b) -> a + ", " + b).orElse("");
            if (!skipped.isEmpty()) {
                inspectDetail += " (skipped " + skipped.size() + " without a2a-agent-url label: "
                        + String.join(", ", skipped) + ")";
            }
            if (!deprecated.isEmpty()) {
                inspectDetail += " (excluded " + deprecated.size() + " DEPRECATED: "
                        + String.join(", ", deprecated) + ")";
            }
            onStep.accept(new StepEvent("inspect", "done", inspectDetail, allCardsJson));

            // If the only agents offering the requested skill are DEPRECATED, say so explicitly
            // instead of widening the search to unrelated agents.
            List<AgentCandidate> deprecatedMatches = strictSkillMatches(deprecatedCandidates, userRequest);
            if (!deprecatedMatches.isEmpty() && strictSkillMatches(candidates, userRequest).isEmpty()) {
                String names = deprecatedMatches.stream().map(AgentCandidate::name).reduce((a, b) -> a + ", " + b).orElse("");
                String msg = "No active agent offers this capability. Matching agent(s) are DEPRECATED in the registry: " + names;
                onStep.accept(new StepEvent("skills", "error", msg));
                return msg;
            }

            if (candidates.isEmpty()) {
                onStep.accept(new StepEvent("search", "error", "No usable (non-deprecated) agents in the registry"));
                return "No usable agents found in the registry.";
            }

            StringBuilder llmPrompt = new StringBuilder();
            llmPrompt.append("User request: ").append(userRequest).append("\n\nAvailable agents:\n");

            // Search by skill. Apicurio Registry's search API can filter by artifact
            // name/description/labels (an exact-match label query would work here too — see
            // searchMcpServers on the MCP side), but a2a-agent-skills is a single flattened
            // string per agent, so free-text requests are still matched client-side: request
            // keywords (stemmed to a short prefix) against each card's skills label.
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
                    .orElse(null);
            if (chosen == null) {
                String msg = "No suitable agent found for this request (LLM selected: '" + selectedId + "').";
                onStep.accept(new StepEvent("match", "error", msg));
                return msg;
            }

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
     * Returns true when the latest version of an Agent Card is in the DEPRECATED state.
     * Registry errors are treated as "not deprecated" so a lookup failure never hides an agent.
     */
    private boolean isLatestVersionDeprecated(String artifactId) {
        try {
            VersionMetaData latest = registryClient.groups().byGroupId(groupId).artifacts()
                    .byArtifactId(artifactId).versions().byVersionExpression("branch=latest").get();
            return latest != null && latest.getState() == VersionState.DEPRECATED;
        } catch (Exception e) {
            LOG.warnf("Could not read version state for %s: %s", artifactId, e.getMessage());
            return false;
        }
    }

    /** Request keywords, stemmed to a short prefix, with stopwords removed. */
    private static List<String> requestStems(String userRequest) {
        return Arrays.stream(userRequest.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(w -> w.length() >= 4 && !STOPWORDS.contains(w))
                .map(w -> w.substring(0, Math.min(STEM_PREFIX_LEN, w.length())))
                .distinct()
                .toList();
    }

    /** Candidates whose name or skills contain a request keyword; empty if none match. */
    private static List<AgentCandidate> strictSkillMatches(List<AgentCandidate> candidates, String userRequest) {
        List<String> stems = requestStems(userRequest);
        return candidates.stream()
                .filter(c -> {
                    String haystack = (c.name() + " " + c.skills()).toLowerCase(Locale.ROOT);
                    return stems.stream().anyMatch(haystack::contains);
                })
                .toList();
    }

    /**
     * Narrows the candidate agents down to those whose skills (or name) look relevant to the
     * user's request, using simple stemmed-keyword matching. Apicurio Registry's search API
     * doesn't index the nested {@code skills} array inside an AGENT_CARD's content, so this
     * capability-based "search" happens client-side once the cards have been read.
     *
     * <p>Falls back to every candidate if no keyword matches, so general requests still reach
     * the LLM router; the router may then answer that no agent is suitable.
     */
    private static List<AgentCandidate> searchCandidatesBySkill(List<AgentCandidate> candidates, String userRequest) {
        List<AgentCandidate> matches = strictSkillMatches(candidates, userRequest);
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
