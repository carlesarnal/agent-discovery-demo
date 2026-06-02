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

# Pre-warm the model
echo "--- Pre-warming Ollama ---"
curl -sf http://localhost:11434/api/generate -d '{"model":"qwen2.5:1.5b","prompt":"hello","stream":false}' > /dev/null
echo "Model loaded"
echo ""

# Build agents
echo "--- Building agents ---"
cd "$PROJECT_DIR/agents/summarizer" && mvn package -DskipTests -q
cd "$PROJECT_DIR/agents/translator" && mvn package -DskipTests -q
cd "$PROJECT_DIR/agents/orchestrator" && mvn package -DskipTests -q
echo "All agents built"
echo ""

# Kill any existing agent processes
for port in 10010 10020 10030; do
  lsof -i :$port -t 2>/dev/null | xargs kill -9 2>/dev/null || true
done
sleep 1

# Start all three agents
echo "--- Starting Summarizer Agent (port 10010) ---"
cd "$PROJECT_DIR/agents/summarizer"
java -jar target/summarizer-agent-1.0-SNAPSHOT-runner.jar > /tmp/summarizer.log 2>&1 &
sleep 8
grep -q "started in" /tmp/summarizer.log && echo "Summarizer Agent is ready!" || echo "WARNING: Summarizer may not be ready — check /tmp/summarizer.log"
echo ""

echo "--- Starting Translator Agent (port 10030) ---"
cd "$PROJECT_DIR/agents/translator"
java -jar target/translator-agent-1.0-SNAPSHOT-runner.jar > /tmp/translator.log 2>&1 &
sleep 8
grep -q "started in" /tmp/translator.log && echo "Translator Agent is ready!" || echo "WARNING: Translator may not be ready — check /tmp/translator.log"
echo ""

echo "--- Starting Orchestrator Agent (port 10020) ---"
cd "$PROJECT_DIR/agents/orchestrator"
java -jar target/orchestrator-agent-1.0-SNAPSHOT-runner.jar > /tmp/orchestrator.log 2>&1 &
sleep 6
grep -q "started in" /tmp/orchestrator.log && echo "Orchestrator Agent is ready!" || echo "WARNING: Orchestrator may not be ready — check /tmp/orchestrator.log"
echo ""

# Verify Agent Cards in the registry
echo "--- Agent Cards in Registry ---"
curl -s http://localhost:8080/apis/registry/v3/groups/a2a-agents/artifacts | jq '.artifacts[] | {id: .artifactId, name: .name}'
echo ""

echo "--- A2A Well-Known Endpoint ---"
curl -s http://localhost:8080/.well-known/agents | jq '.count'
echo " agents discoverable"
echo ""

echo "=== All agents running ==="
echo "  Summarizer:    http://localhost:10010"
echo "  Translator:    http://localhost:10030"
echo "  Orchestrator:  http://localhost:10020"
echo "  Dashboard:     http://localhost:10020"
echo "  Registry API:  http://localhost:8080"
echo "  Registry UI:   http://localhost:8888"
