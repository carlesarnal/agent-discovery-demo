#!/bin/bash
# Start the real A2A agents with Ollama
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

# Build and start the agents
echo "--- Building and starting agents ---"
docker compose -f "$PROJECT_DIR/docker-compose.yaml" up -d --build summarizer orchestrator
echo ""

# Wait for agents to be ready
echo "--- Waiting for Summarizer Agent (port 10010) ---"
until curl -sf http://localhost:10010/q/health > /dev/null 2>&1; do
  sleep 2
done
echo "Summarizer Agent is ready!"
echo ""

echo "--- Waiting for Orchestrator Agent (port 10020) ---"
until curl -sf http://localhost:10020/q/health > /dev/null 2>&1; do
  sleep 2
done
echo "Orchestrator Agent is ready!"
echo ""

# Verify the summarizer published its card to the registry
echo "--- Verifying Agent Card in Registry ---"
curl -s http://localhost:8080/apis/registry/v3/groups/a2a-agents/artifacts | jq '.artifacts[] | {id: .artifactId, name: .name}'
echo ""

echo "=== Agents are running ==="
echo "  Summarizer:   http://localhost:10010"
echo "  Orchestrator: http://localhost:10020"
echo "  Registry:     http://localhost:8080"
