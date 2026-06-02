package com.carlesarnal.agents.summarizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCard;
import io.apicurio.registry.client.RegistryClientFactory;
import io.apicurio.registry.client.common.RegistryClientOptions;
import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.models.CreateArtifact;
import io.apicurio.registry.rest.client.models.CreateVersion;
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
            RegistryClient client = RegistryClientFactory.create(
                    RegistryClientOptions.create(registryUrl + "/apis/registry/v3"));

            ObjectMapper mapper = new ObjectMapper();
            String cardJson = mapper.writeValueAsString(agentCard);
            String artifactId = agentCard.name().toLowerCase().replaceAll("[^a-z0-9]+", "-");

            VersionContent content = new VersionContent();
            content.setContent(cardJson);
            content.setContentType("application/json");

            CreateVersion version = new CreateVersion();
            version.setContent(content);

            CreateArtifact createArtifact = new CreateArtifact();
            createArtifact.setArtifactId(artifactId);
            createArtifact.setArtifactType("JSON");
            createArtifact.setName(agentCard.name());
            createArtifact.setDescription(agentCard.description());
            createArtifact.setFirstVersion(version);

            client.groups()
                    .byGroupId(groupId)
                    .artifacts()
                    .post(createArtifact);

            LOG.infof("Published Agent Card '%s' to registry group '%s'", agentCard.name(), groupId);
        } catch (Exception e) {
            LOG.warn("Could not publish Agent Card to registry", e);
        }
    }
}
