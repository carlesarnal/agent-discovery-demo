package com.carlesarnal.agents.summarizer;

import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCard;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@ApplicationScoped
public class RegistryPublisher {

    private static final Logger LOG = Logger.getLogger(RegistryPublisher.class);

    @Inject
    @PublicAgentCard
    AgentCard agentCard;

    @Inject
    @ConfigProperty(name = "registry.url", defaultValue = "http://localhost:8080")
    String registryUrl;

    @Inject
    @ConfigProperty(name = "registry.group-id", defaultValue = "a2a-agents")
    String groupId;

    void onStart(@Observes StartupEvent ev) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String cardJson = mapper.writeValueAsString(agentCard);
            String artifactId = agentCard.name().toLowerCase().replaceAll("[^a-z0-9]+", "-");

            String url = registryUrl + "/apis/registry/v3/groups/" + groupId + "/artifacts";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("X-Registry-ArtifactId", artifactId)
                    .header("X-Registry-ArtifactType", "JSON")
                    .header("X-Registry-Name", agentCard.name())
                    .header("X-Registry-Description", agentCard.description())
                    .POST(HttpRequest.BodyPublishers.ofString(cardJson))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                LOG.infof("Published Agent Card '%s' to registry group '%s'", agentCard.name(), groupId);
            } else {
                LOG.warnf("Failed to publish Agent Card: HTTP %d - %s", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            LOG.warn("Could not publish Agent Card to registry (will retry when registry is available)", e);
        }
    }
}
