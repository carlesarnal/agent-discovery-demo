package com.carlesarnal.agents.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkiverse.langchain4j.a2a.runtime.apicurio.ApicurioAgentsRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestStreamElementType;
import io.smallrye.mutiny.Multi;

import java.util.Map;
import java.util.stream.Collectors;

@Path("/")
public class OrchestratorResource {

    @Inject
    ApicurioAgentsRegistry agentsRegistry;

    @Inject
    RegistryDiscoveryService discoveryService;

    @Inject
    OrchestratorBot bot;

    private final ObjectMapper mapper = new ObjectMapper();

    @GET
    @Path("/agents")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> listAgents() {
        return agentsRegistry.allAgents().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().description()));
    }

    @POST
    @Path("/orchestrate")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String orchestrate(String request) {
        return discoveryService.discoverAndDelegate(request);
    }

    @POST
    @Path("/orchestrate/stream")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<String> orchestrateStream(String request) {
        return Multi.createFrom().emitter(emitter -> {
            Thread.startVirtualThread(() -> {
                try {
                    discoveryService.discoverAndDelegate(request, event -> {
                        try {
                            emitter.emit(mapper.writeValueAsString(event));
                        } catch (Exception e) {
                            emitter.fail(e);
                        }
                    });
                    emitter.complete();
                } catch (Exception e) {
                    emitter.fail(e);
                }
            });
        });
    }

    @POST
    @Path("/chat")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String chat(String message) {
        return bot.chat(message);
    }
}
