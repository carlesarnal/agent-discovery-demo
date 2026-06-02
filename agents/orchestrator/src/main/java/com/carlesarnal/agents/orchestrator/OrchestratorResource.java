package com.carlesarnal.agents.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestStreamElementType;
import io.smallrye.mutiny.Multi;

import java.util.concurrent.LinkedBlockingQueue;

@Path("/orchestrate")
public class OrchestratorResource {

    @Inject
    RegistryDiscoveryService discoveryService;

    private final ObjectMapper mapper = new ObjectMapper();

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String orchestrate(String request) {
        return discoveryService.discoverAndDelegate(request);
    }

    @POST
    @Path("/stream")
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
}
