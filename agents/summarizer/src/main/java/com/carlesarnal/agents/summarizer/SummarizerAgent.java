package com.carlesarnal.agents.summarizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@ApplicationScoped
public class SummarizerAgent {

    private static final Logger LOG = Logger.getLogger(SummarizerAgent.class);

    @Inject
    ChatModel chatModel;

    @Inject
    @ConfigProperty(name = "registry.url", defaultValue = "http://localhost:8080")
    String registryUrl;

    @Inject
    @ConfigProperty(name = "registry.prompt.group", defaultValue = "prompts")
    String promptGroup;

    @Inject
    @ConfigProperty(name = "registry.prompt.artifact", defaultValue = "summarizer-system-prompt")
    String promptArtifact;

    @Inject
    @ConfigProperty(name = "registry.prompt.version", defaultValue = "branch=latest")
    String promptVersion;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public String summarize(String text) {
        String prompt = renderPromptFromRegistry(text);
        LOG.infof("Using prompt from registry: %s", prompt.substring(0, Math.min(80, prompt.length())) + "...");
        return chatModel.chat(prompt);
    }

    private String renderPromptFromRegistry(String inputText) {
        try {
            String renderUrl = String.format("%s/apis/registry/v3/groups/%s/artifacts/%s/versions/%s/render",
                    registryUrl, promptGroup, promptArtifact, promptVersion);

            String body = mapper.writeValueAsString(
                    mapper.createObjectNode()
                            .set("variables", mapper.createObjectNode()
                                    .put("max_sentences", 3)
                                    .put("input_text", inputText)
                                    .put("tone", "professional")));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(renderUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode result = mapper.readTree(response.body());

            if (result.has("rendered")) {
                return result.get("rendered").asText();
            }

            LOG.warnf("Registry render returned no 'rendered' field: %s", response.body());
        } catch (Exception e) {
            LOG.warn("Failed to render prompt from registry, using fallback", e);
        }

        return "You are a summarization assistant. Summarize the following text in 3 sentences.\n\nText: "
                + inputText + "\n\nSummary:";
    }
}
