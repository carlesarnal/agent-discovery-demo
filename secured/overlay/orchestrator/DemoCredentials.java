package io.quarkiverse.langchain4j.sample.registry.orchestrator;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.jboss.logging.Logger;

import io.quarkiverse.langchain4j.agentic.auth.AgentAuthProvider;
import io.quarkiverse.langchain4j.mcp.auth.McpClientAuthProvider;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;

/**
 * Secured mode: the orchestrator's client-credentials token, sent to the A2A agents, to the MCP
 * servers it discovers in the Registry, and to the Registry for the contract lookups. The token is
 * fetched at startup and refreshed in the background, so the getters never block.
 */
@ApplicationScoped
public class DemoCredentials implements AgentAuthProvider, McpClientAuthProvider, ClientHeadersFactory {

    private static final Logger log = Logger.getLogger(DemoCredentials.class);

    @Inject
    OidcClient client;

    @Inject
    Vertx vertx;

    private final AtomicReference<Tokens> tokens = new AtomicReference<>();

    void start(@Observes StartupEvent event) {
        tokens.set(client.getTokens().await().atMost(Duration.ofSeconds(30)));
        vertx.setPeriodic(Duration.ofSeconds(30).toMillis(), id -> refresh());
    }

    private void refresh() {
        Tokens current = tokens.get();
        if (current != null && !current.isAccessTokenWithinRefreshInterval()
                && !current.isAccessTokenExpired()) {
            return;
        }
        client.getTokens().subscribe().with(tokens::set,
                failure -> log.warnf("Could not refresh the orchestrator token: %s", failure.getMessage()));
    }

    private String bearer() {
        return "Bearer " + tokens.get().getAccessToken();
    }

    @Override
    public String getAuthorization(AgentAuthProvider.Input input) {
        return bearer();
    }

    @Override
    public String getAuthorization(McpClientAuthProvider.Input input) {
        return bearer();
    }

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incoming,
            MultivaluedMap<String, String> outgoing) {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("Authorization", bearer());
        return headers;
    }
}
