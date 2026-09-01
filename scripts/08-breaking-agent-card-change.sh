#!/bin/bash
# Demonstrate that breaking changes to A2A Agent Cards are REJECTED too —
# the same compatibility-rule engine used for prompt templates (and OpenAPI,
# Avro, ...) applies to AGENT_CARD content, checking the skills list itself.
set -euo pipefail

REGISTRY_URL="${REGISTRY_URL:-http://localhost:8080}"
GROUP="a2a-agents"
ARTIFACT="summarizer-agent"

echo "=== Breaking Change Demo: Agent Cards ==="
echo ""
echo "This requires the Summarizer agent to already be running and self-registered"
echo "(./scripts/06-start-agents.sh)."
echo ""

echo "--- Enable BACKWARD compatibility on $ARTIFACT ---"
curl -s -o /dev/null -w "HTTP %{http_code}\n" \
  -X POST "$REGISTRY_URL/apis/registry/v3/groups/$GROUP/artifacts/$ARTIFACT/rules" \
  -H "Content-Type: application/json" \
  -d '{"ruleType": "COMPATIBILITY", "config": "BACKWARD"}'
echo ""

echo "--- Current Agent Card (published live by the Summarizer agent) ---"
CURRENT=$(curl -s "$REGISTRY_URL/apis/registry/v3/groups/$GROUP/artifacts/$ARTIFACT/versions/branch=latest/content")
echo "$CURRENT" | jq '{name, skills: [.skills[].name]}'
echo ""

echo "--- Attempt: Remove the 'Text Summarization' skill ---"
BROKEN=$(echo "$CURRENT" | jq -c '.skills = []')
HTTP_CODE=$(curl -s -o /tmp/agent-card-breaking.json -w "%{http_code}" \
  -X POST "$REGISTRY_URL/apis/registry/v3/groups/$GROUP/artifacts/$ARTIFACT/versions" \
  -H "Content-Type: application/json" \
  --data-binary "$(jq -n --arg c "$BROKEN" '{content: {content: $c, contentType: "application/json"}}')")

echo "HTTP Status: $HTTP_CODE"
if [ "$HTTP_CODE" -ge 400 ]; then
  echo "REJECTED!"
  jq -r '.detail' /tmp/agent-card-breaking.json 2>/dev/null
fi
echo ""

echo "--- Compatible change: add a new skill alongside the existing one(s) ---"
COMPATIBLE=$(echo "$CURRENT" | jq -c '.skills += [{"id":"summarize-bullets","name":"Bullet-Point Summarization","description":"Summarizes text as bullet points","tags":[],"examples":[]}]')
HTTP_CODE=$(curl -s -o /tmp/agent-card-compatible.json -w "%{http_code}" \
  -X POST "$REGISTRY_URL/apis/registry/v3/groups/$GROUP/artifacts/$ARTIFACT/versions" \
  -H "Content-Type: application/json" \
  --data-binary "$(jq -n --arg c "$COMPATIBLE" '{content: {content: $c, contentType: "application/json"}}')")

echo "HTTP Status: $HTTP_CODE"
if [ "$HTTP_CODE" -lt 300 ]; then
  echo "ACCEPTED — new version registered (adding a skill is backward compatible)."
fi
echo ""

echo "Removing a skill an orchestrator may already depend on is rejected BEFORE production —"
echo "the same rule engine that protects OpenAPI schemas and prompt templates now protects"
echo "Agent Cards too."
echo ""
echo "=== Agent Card breaking change demo complete ==="
