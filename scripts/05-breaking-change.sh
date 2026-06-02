#!/bin/bash
# Demonstrate compatibility checking and version evolution
set -euo pipefail

REGISTRY_URL="${REGISTRY_URL:-http://localhost:8080}"

echo "=== Compatibility & Version Evolution Demo ==="
echo ""

# Show all versions
echo "--- Current versions of summarizer-system-prompt ---"
curl -s "$REGISTRY_URL/apis/registry/v3/groups/prompts/artifacts/summarizer-system-prompt/versions" | jq '.versions[] | {version: .version, createdOn: .createdOn}'
echo ""

echo "--- Version 1 variables ---"
curl -s "$REGISTRY_URL/apis/registry/v3/groups/prompts/artifacts/summarizer-system-prompt/versions/1/content" | jq '.variables | keys'

echo ""
echo "--- Version 2 variables (added optional 'tone') ---"
curl -s "$REGISTRY_URL/apis/registry/v3/groups/prompts/artifacts/summarizer-system-prompt/versions/2/content" | jq '.variables | keys'
echo ""

echo "--- Compatibility rule in effect ---"
curl -s "$REGISTRY_URL/apis/registry/v3/groups/prompts/artifacts/summarizer-system-prompt/rules" | jq .
echo ""

echo "--- Attempting a new version that renames 'input_text' to 'content' ---"
HTTP_CODE=$(curl -s -o /tmp/breaking-change-response.json -w "%{http_code}" \
  -X POST "$REGISTRY_URL/apis/registry/v3/groups/prompts/artifacts/summarizer-system-prompt/versions" \
  -H "Content-Type: application/json" \
  -d '{
    "content": {
      "content": "{\"template\":\"Summarize this content in {{max_sentences}} sentences.\\n\\nContent: {{content}}\\n\\nSummary:\",\"variables\":{\"max_sentences\":{\"type\":\"integer\",\"default\":3},\"content\":{\"type\":\"string\",\"required\":true}},\"metadata\":{\"model\":\"llama3.2\",\"temperature\":0.3,\"version\":\"3.0.0\"}}",
      "contentType": "application/json"
    }
  }')

echo "HTTP Status: $HTTP_CODE"
echo ""

if [ "$HTTP_CODE" -ge 400 ]; then
  echo "REJECTED! The registry blocked this change."
  echo ""
  jq . /tmp/breaking-change-response.json 2>/dev/null || cat /tmp/breaking-change-response.json
else
  echo "The version was accepted. JSON-level compatibility checking allows"
  echo "structural changes — both versions are valid JSON objects."
  echo ""
  echo "For stricter enforcement, use JSON Schema-type artifacts with"
  echo "BACKWARD compatibility — the same rules that protect your"
  echo "OpenAPI response schemas and Avro definitions."
fi

echo ""
echo "--- All versions (showing full evolution history) ---"
curl -s "$REGISTRY_URL/apis/registry/v3/groups/prompts/artifacts/summarizer-system-prompt/versions" | jq '.versions[] | {version: .version, createdOn: .createdOn}'
echo ""

echo "=== Version evolution demo complete ==="
