#!/bin/bash
# Demonstrate live A2A agent discovery + MCP tool discovery via the orchestrator
set -euo pipefail

ORCHESTRATOR_URL="${ORCHESTRATOR_URL:-http://localhost:10020}"

echo "=== Live Demo: A2A + MCP ==="
echo ""

echo ">>> A2A Flow: Summarization"
echo ""
curl -s -X POST "$ORCHESTRATOR_URL/orchestrate" \
  -H "Content-Type: text/plain" \
  -d "Summarize the benefits of open standards for AI governance"
echo ""
echo ""

echo ">>> A2A Flow: Translation"
echo ""
curl -s -X POST "$ORCHESTRATOR_URL/orchestrate" \
  -H "Content-Type: text/plain" \
  -d "Translate to French: Open standards improve interoperability"
echo ""
echo ""

echo ">>> MCP Flow: Weather (search → connect → callMcpTool)"
echo ""
curl -s -X POST "$ORCHESTRATOR_URL/chat" \
  -H "Content-Type: text/plain" \
  -d "What is the weather in Amsterdam?"
echo ""
echo ""

echo "=== Demo complete ==="
echo "  Dashboard: $ORCHESTRATOR_URL"
echo "  Registry:  http://localhost:8888"
