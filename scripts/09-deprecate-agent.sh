#!/bin/bash
# Lifecycle governance: deprecate an agent in the registry and watch the orchestrator stop routing to it.
# Requires the agents to be running (./scripts/06-start-agents.sh).
set -euo pipefail

REGISTRY_URL="${REGISTRY_URL:-http://localhost:8080}"
ORCHESTRATOR_URL="${ORCHESTRATOR_URL:-http://localhost:10020}"
GROUP="a2a-agents"
ARTIFACT="translator-agent"
REQUEST="Translate to French: Open standards improve interoperability"

latest_version() {
  curl -s "$REGISTRY_URL/apis/registry/v3/groups/$GROUP/artifacts/$ARTIFACT/versions/branch=latest" | jq -r '.version'
}

set_state() {
  curl -s -o /dev/null -w "HTTP %{http_code}\n" \
    -X PUT "$REGISTRY_URL/apis/registry/v3/groups/$GROUP/artifacts/$ARTIFACT/versions/$1/state" \
    -H "Content-Type: application/json" -d "{\"state\": \"$2\"}"
}

ask() {
  echo "  > $REQUEST"
  echo -n "  < "
  curl -s -m 300 -X POST "$ORCHESTRATOR_URL/orchestrate" -H "Content-Type: text/plain" -d "$REQUEST"
  echo ""
}

VERSION=$(latest_version)
echo "=== Deprecation Demo: $ARTIFACT (latest version $VERSION) ==="
echo ""

echo "--- 1. Translator is ENABLED: the orchestrator routes to it ---"
ask
echo ""

echo "--- 2. Mark the latest Agent Card version DEPRECATED ---"
set_state "$VERSION" DEPRECATED
curl -s "$REGISTRY_URL/apis/registry/v3/groups/$GROUP/artifacts/$ARTIFACT/versions/$VERSION" | jq -c '{version, state}'
echo ""

echo "--- 3. Same request: the orchestrator no longer offers the deprecated agent ---"
ask
echo "  (The card is still readable in the registry; the orchestrator's discovery excludes it.)"
echo ""

echo "--- 4. Restore: set the version back to ENABLED ---"
set_state "$VERSION" ENABLED
ask
echo ""

echo "Deprecation is a registry state, not a code change: every consumer that honours it"
echo "stops routing new work to the agent at once. Consumers that ignore it keep calling."
echo ""
echo "=== Deprecation demo complete ==="
