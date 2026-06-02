#!/bin/bash
# Demonstrate compatibility checking with PROMPT_TEMPLATE versioning
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

echo "--- Attempting a BREAKING change: remove required 'input_text' variable ---"
HTTP_CODE=$(curl -s -o /tmp/breaking-change-response.json -w "%{http_code}" \
  -X POST "$REGISTRY_URL/apis/registry/v3/groups/prompts/artifacts/summarizer-system-prompt/versions" \
  -H "Content-Type: application/json" \
  -d '{
    "content": {
      "content": "{\"templateId\":\"summarizer-system-prompt\",\"name\":\"Summarizer System Prompt\",\"version\":\"3.0.0\",\"description\":\"Breaking change version\",\"templateFormat\":\"mustache\",\"template\":\"Summarize this content in {{max_sentences}} sentences.\\n\\nContent: {{content}}\\n\\nSummary:\",\"variables\":{\"max_sentences\":{\"type\":\"integer\",\"default\":3},\"content\":{\"type\":\"string\",\"required\":true}}}",
      "contentType": "application/json"
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
  echo "The BACKWARD compatibility rule prevents removing required variables"
  echo "from PROMPT_TEMPLATE artifacts — same rule as OpenAPI schema evolution."
else
  echo "Change was accepted — inspecting the result."
  jq . /tmp/breaking-change-response.json 2>/dev/null || cat /tmp/breaking-change-response.json
fi

echo ""
echo "=== Version evolution demo complete ==="
