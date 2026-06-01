package com.carlesarnal.agents.orchestrator;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/orchestrate")
public class OrchestratorResource {

    @Inject
    RegistryDiscoveryService discoveryService;

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String orchestrate(String request) {
        return discoveryService.discoverAndDelegate(request);
    }
}
