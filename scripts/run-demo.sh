#!/bin/bash
# Run the full agent discovery demo end-to-end
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REGISTRY_URL="${REGISTRY_URL:-http://localhost:8080}"

echo "============================================"
echo " Agent Discovery Demo with Apicurio Registry"
echo " API Days Amsterdam 2026"
echo "============================================"
echo ""

# Wait for registry
bash "$SCRIPT_DIR/wait-for-registry.sh"
echo ""

# Run all steps
echo ">>> Step 1: Register A2A Agent Cards"
echo ""
bash "$SCRIPT_DIR/01-register-agents.sh"
echo ""
echo "--------------------------------------------"
echo ""

echo ">>> Step 2: Register Prompt Templates"
echo ""
bash "$SCRIPT_DIR/02-register-prompts.sh"
echo ""
echo "--------------------------------------------"
echo ""

echo ">>> Step 3: Register Model Schemas"
echo ""
bash "$SCRIPT_DIR/03-register-model-schemas.sh"
echo ""
echo "--------------------------------------------"
echo ""

echo ">>> Step 4: Discover Agents"
echo ""
bash "$SCRIPT_DIR/04-discover-agents.sh"
echo ""
echo "--------------------------------------------"
echo ""

echo ">>> Step 5: Breaking Change Protection"
echo ""
bash "$SCRIPT_DIR/05-breaking-change.sh"
echo ""

echo "============================================"
echo " Demo complete!"
echo " Registry UI: $REGISTRY_URL"
echo "============================================"
