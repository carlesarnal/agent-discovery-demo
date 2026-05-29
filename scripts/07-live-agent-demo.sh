#!/bin/bash
# Demonstrate live agent discovery and delegation via the orchestrator
set -euo pipefail

ORCHESTRATOR_URL="${ORCHESTRATOR_URL:-http://localhost:10020}"

echo "=== Live Agent Demo ==="
echo ""
echo "The orchestrator will:"
echo "  1. Search the registry for agents that can handle the task"
echo "  2. Discover the Summarizer Agent's capabilities"
echo "  3. Delegate the task via the A2A Protocol"
echo "  4. Return the result"
echo ""
echo "--- Sending request to orchestrator ---"
echo ""
echo "Request: \"Summarize the key benefits of using open standards like OpenAPI"
echo "          and A2A for governing AI agents in enterprise architectures\""
echo ""

RESPONSE=$(curl -s -X POST "$ORCHESTRATOR_URL/orchestrate" \
  -H "Content-Type: text/plain" \
  -d "Summarize the key benefits of using open standards like OpenAPI and A2A for governing AI agents in enterprise architectures")

echo "--- Response ---"
echo ""
echo "$RESPONSE"
echo ""
echo "=== Live agent demo complete ==="
