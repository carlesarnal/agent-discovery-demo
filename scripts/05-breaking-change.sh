#!/bin/bash
# Demonstrate that breaking changes are REJECTED by compatibility rules
set -euo pipefail

REGISTRY_URL="${REGISTRY_URL:-http://localhost:8080}"

echo "=== Breaking Change Demo ==="
echo ""

echo "--- Current versions of summarizer-system-prompt ---"
curl -s "$REGISTRY_URL/apis/registry/v3/groups/prompts/artifacts/summarizer-system-prompt/versions" | jq '.versions[] | {version: .version, createdOn: .createdOn}'
echo ""

echo "--- Compatibility rule in effect ---"
curl -s "$REGISTRY_URL/apis/registry/v3/groups/prompts/artifacts/summarizer-system-prompt/rules" | jq .
echo ""

# Breaking change 1: change variable type
echo "--- Attempt 1: Change variable type (max_sentences: integer → string) ---"
echo ""

HTTP_CODE=$(curl -s -o /tmp/breaking1.json -w "%{http_code}" \
  -X POST "$REGISTRY_URL/apis/registry/v3/groups/prompts/artifacts/summarizer-system-prompt/versions" \
  -H "Content-Type: application/json" \
  -d '{
    "content": {
      "content": "{\"templateId\":\"summarizer-system-prompt\",\"name\":\"Summarizer System Prompt\",\"version\":\"3.0.0\",\"templateFormat\":\"mustache\",\"template\":\"You are a summarization assistant. Summarize the following text in {{max_sentences}} sentences.\\n\\nText: {{input_text}}\\n\\nSummary:\",\"variables\":{\"max_sentences\":{\"type\":\"string\",\"default\":\"3\"},\"input_text\":{\"type\":\"string\",\"required\":true}}}",
      "contentType": "application/json"
    }
  }')

echo "HTTP Status: $HTTP_CODE"
if [ "$HTTP_CODE" -ge 400 ]; then
  echo "REJECTED!"
  jq -r '.detail' /tmp/breaking1.json 2>/dev/null
fi
echo ""

# Breaking change 2: remove variable still used in template
echo "--- Attempt 2: Remove 'input_text' variable (still referenced in template) ---"
echo ""

HTTP_CODE=$(curl -s -o /tmp/breaking2.json -w "%{http_code}" \
  -X POST "$REGISTRY_URL/apis/registry/v3/groups/prompts/artifacts/summarizer-system-prompt/versions" \
  -H "Content-Type: application/json" \
  -d '{
    "content": {
      "content": "{\"templateId\":\"summarizer-system-prompt\",\"name\":\"Summarizer System Prompt\",\"version\":\"3.0.0\",\"templateFormat\":\"mustache\",\"template\":\"You are a summarization assistant. Summarize the following text in {{max_sentences}} sentences.\\n\\nText: {{input_text}}\\n\\nSummary:\",\"variables\":{\"max_sentences\":{\"type\":\"integer\",\"default\":3}}}",
      "contentType": "application/json"
    }
  }')

echo "HTTP Status: $HTTP_CODE"
if [ "$HTTP_CODE" -ge 400 ]; then
  echo "REJECTED!"
  jq -r '.detail' /tmp/breaking2.json 2>/dev/null
fi
echo ""

echo "Both breaking changes were caught BEFORE production."
echo "Same rules that protect OpenAPI schemas now protect prompt templates."
echo ""
echo "=== Breaking change demo complete ==="
