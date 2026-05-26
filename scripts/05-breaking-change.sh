#!/bin/bash
# Demonstrate that breaking changes are rejected by compatibility rules
set -euo pipefail

REGISTRY_URL="${REGISTRY_URL:-http://localhost:8080}"

echo "=== Breaking Change Demo ==="
echo ""
echo "Attempting to register a breaking change to the summarizer-system-prompt..."
echo "This version REMOVES the required 'input_text' variable — a backward-incompatible change."
echo ""

HTTP_CODE=$(curl -s -o /tmp/breaking-change-response.json -w "%{http_code}" \
  -X POST "$REGISTRY_URL/apis/registry/v3/groups/prompts/artifacts/summarizer-system-prompt/versions" \
  -H "Content-Type: application/json" \
  -d '{
    "template": "Summarize this content in {{max_sentences}} sentences.\n\nContent: {{content}}\n\nSummary:",
    "variables": {
      "max_sentences": {"type": "integer", "default": 3},
      "content": {"type": "string", "required": true}
    },
    "metadata": {
      "model": "llama3.2",
      "temperature": 0.3,
      "version": "3.0.0"
    }
  }')

echo "HTTP Status: $HTTP_CODE"
echo ""

if [ "$HTTP_CODE" -ge 400 ]; then
  echo "REJECTED! The registry blocked this breaking change."
  echo ""
  echo "Error details:"
  jq . /tmp/breaking-change-response.json 2>/dev/null || cat /tmp/breaking-change-response.json
  echo ""
  echo "The BACKWARD compatibility rule prevents removing or renaming required variables."
  echo "This protects downstream agents that depend on 'input_text' existing in the template."
else
  echo "WARNING: Change was accepted (this may happen if compatibility checking is JSON-level)."
  echo "Response:"
  jq . /tmp/breaking-change-response.json
fi

echo ""
echo "=== Breaking change demo complete ==="
