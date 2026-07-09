#!/bin/bash
# Start all agents (A2A + MCP) via Docker Compose
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

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
docker compose -f "$PROJECT_DIR/docker-compose.yaml" up -d --build summarizer translator mcp-weather orchestrator
echo ""

# Wait for agents to be ready
echo "--- Waiting for agents to start ---"
for port in 10010 10030 10040 10020; do
  until curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/" 2>/dev/null | grep -qE "200|400|405"; do
    sleep 2
  done
  echo "  Port $port ready"
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
