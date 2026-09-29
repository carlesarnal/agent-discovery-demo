#!/bin/bash
# Start all agents (A2A + MCP) via Docker Compose
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# HOST_OLLAMA=1 uses a native Ollama on the host (e.g. GPU on macOS) instead of the ollama container
COMPOSE=(docker compose -f "$PROJECT_DIR/docker-compose.yaml")
if [ "${HOST_OLLAMA:-0}" = "1" ]; then
  COMPOSE+=(-f "$PROJECT_DIR/compose.host-ollama.yaml")
fi

echo "=== Starting Agents ==="
echo ""

# Pull both Ollama models
echo "--- Pulling Ollama models ---"
for model in qwen2.5:1.5b qwen2.5:7b; do
  echo "  Pulling $model..."
  curl -sf http://localhost:11434/api/pull -d "{\"name\": \"$model\"}" | while read -r line; do
    status=$(echo "$line" | jq -r '.status // empty' 2>/dev/null)
    [ "$status" = "success" ] && echo "    $model ready"
  done
done
echo ""

# Pre-warm the models
echo "--- Pre-warming Ollama ---"
curl -sf http://localhost:11434/api/generate -d '{"model":"qwen2.5:1.5b","prompt":"hello","stream":false}' > /dev/null
curl -sf http://localhost:11434/api/generate -d '{"model":"qwen2.5:7b","prompt":"hello","stream":false}' > /dev/null
echo "Models loaded"
echo ""

# Start all agents via docker compose
echo "--- Starting agents (building if needed) ---"
"${COMPOSE[@]}" up -d --build summarizer translator mcp-weather orchestrator
echo ""

# Wait for agents to be ready
echo "--- Waiting for agents to start ---"
# The MCP server has no root page (404 on "/"), so probe its /mcp endpoint instead.
for url in http://localhost:10010/ http://localhost:10030/ http://localhost:10040/mcp http://localhost:10020/; do
  waited=0
  until curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null | grep -qE "200|400|405"; do
    sleep 2; waited=$((waited + 2))
    if [ "$waited" -ge 300 ]; then
      echo "  Timed out waiting for $url" >&2
      exit 1
    fi
  done
  echo "  $url ready"
done
echo ""

# Verify artifacts in the registry
echo "--- Artifacts in Registry ---"
curl -s http://localhost:8080/apis/registry/v3/search/artifacts | jq '.artifacts[] | {id: .artifactId, name: .name, type: .artifactType, group: .groupId}'
echo ""

echo "=== All agents running ==="
echo "  Summarizer:    http://localhost:10010  (A2A)"
echo "  Translator:    http://localhost:10030  (A2A)"
echo "  MCP Weather:   http://localhost:10040  (MCP)"
echo "  Orchestrator:  http://localhost:10020  (A2A + MCP)"
echo "  Dashboard:     http://localhost:10020"
echo "  Registry API:  http://localhost:8080"
echo "  Registry UI:   http://localhost:8888"
