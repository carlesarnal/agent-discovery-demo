#!/bin/bash
# Cloud Native AI Summit Paris 2026 — "Agent Sprawl Is the New Microservice Sprawl"
# Talk-order walkthrough (25-minute slot). Prompt/model-schema scripts are intentionally left out.
#
# Usage:
#   ./scripts/paris-2026-demo.sh              # uses the ollama container
#   HOST_OLLAMA=1 ./scripts/paris-2026-demo.sh # uses a native Ollama on the host (GPU on macOS)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
pause() { echo ""; read -r -p "Press Enter to continue..." _; echo ""; }

COMPOSE=(docker compose -f "$PROJECT_DIR/docker-compose.yaml")
if [ "${HOST_OLLAMA:-0}" = "1" ]; then
  COMPOSE+=(-f "$PROJECT_DIR/compose.host-ollama.yaml")
else
  "${COMPOSE[@]}" up -d ollama
fi

echo "=== 0. Registry ==="
"${COMPOSE[@]}" up -d apicurio-registry apicurio-registry-ui
"$SCRIPT_DIR/wait-for-registry.sh"
pause

echo "=== 1. Agents start and publish their own Agent Cards / MCP tool ==="
"$SCRIPT_DIR/06-start-agents.sh"
pause

echo "=== 2. Discovery + delegation: A2A agents, then an MCP tool, through one registry ==="
"$SCRIPT_DIR/07-live-agent-demo.sh"
pause

echo "=== 3. Contract protection: a breaking Agent Card change is rejected ==="
"$SCRIPT_DIR/08-breaking-agent-card-change.sh"
pause

echo "=== 4. Lifecycle: deprecate an agent, routing stops, restore it ==="
"$SCRIPT_DIR/09-deprecate-agent.sh"

echo ""
echo "Dashboard: http://localhost:10020   Registry UI: http://localhost:8888"
echo "Clean up:  ./scripts/cleanup.sh"
