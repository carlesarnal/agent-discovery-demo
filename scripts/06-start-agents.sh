#!/bin/bash
# Start the real A2A agents via Docker Compose
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "=== Starting Real A2A Agents ==="
echo ""

# Pull the Ollama model first
echo "--- Pulling qwen2.5:1.5b model (if not cached) ---"
curl -sf http://localhost:11434/api/pull -d '{"name": "qwen2.5:1.5b"}' | while read -r line; do
  status=$(echo "$line" | jq -r '.status // empty' 2>/dev/null)
  [ -n "$status" ] && echo "  $status"
done
echo ""

# Pre-warm the model
echo "--- Pre-warming Ollama ---"
curl -sf http://localhost:11434/api/generate -d '{"model":"qwen2.5:1.5b","prompt":"hello","stream":false}' > /dev/null
echo "Model loaded"
echo ""

# Start the agents via docker compose
echo "--- Starting agents (building if needed) ---"
docker compose -f "$PROJECT_DIR/docker-compose.yaml" up -d --build summarizer translator orchestrator
echo ""

# Wait for agents to be ready
echo "--- Waiting for agents to start ---"
for port in 10010 10030 10020; do
  until curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/" 2>/dev/null | grep -qE "200|405"; do
    sleep 2
  done
  echo "  Port $port ready"
done
echo ""

# Verify Agent Cards in the registry
echo "--- Agent Cards in Registry ---"
curl -s http://localhost:8080/apis/registry/v3/groups/a2a-agents/artifacts | jq '.artifacts[] | {id: .artifactId, name: .name}'
echo ""

echo "--- A2A Well-Known Endpoint ---"
count=$(curl -s http://localhost:8080/.well-known/agents | jq '.count')
echo "$count agents discoverable"
echo ""

echo "=== All agents running ==="
echo "  Summarizer:    http://localhost:10010"
echo "  Translator:    http://localhost:10030"
echo "  Orchestrator:  http://localhost:10020"
echo "  Dashboard:     http://localhost:10020"
echo "  Registry API:  http://localhost:8080"
echo "  Registry UI:   http://localhost:8888"
