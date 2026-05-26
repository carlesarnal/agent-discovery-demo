#!/bin/bash
# Demonstrate agent discovery via the registry API
set -euo pipefail

REGISTRY_URL="${REGISTRY_URL:-http://localhost:8080}"

echo "=== Agent Discovery Demo ==="
echo ""

echo "--- List all registered agents ---"
curl -s "$REGISTRY_URL/apis/registry/v3/groups/ai-agents/artifacts" | jq '.artifacts[] | {id: .artifactId, name: .name}'
echo ""

echo "--- Discover Summarizer Agent capabilities ---"
curl -s "$REGISTRY_URL/apis/registry/v3/groups/ai-agents/artifacts/summarizer-agent/versions/1/content" | jq '{name, description, url, skills: [.skills[].name], capabilities}'
echo ""

echo "--- Search agents by name ---"
curl -s "$REGISTRY_URL/apis/registry/v3/search/artifacts?groupId=ai-agents&name=Translator" | jq '.artifacts[] | {id: .artifactId, name: .name, description: .description}'
echo ""

echo "--- List all prompt template versions ---"
curl -s "$REGISTRY_URL/apis/registry/v3/groups/prompts/artifacts/summarizer-system-prompt/versions" | jq '.versions[] | {version: .version, createdOn: .createdOn}'
echo ""

echo "--- Compare prompt v1 vs v2 ---"
echo "Version 1 variables:"
curl -s "$REGISTRY_URL/apis/registry/v3/groups/prompts/artifacts/summarizer-system-prompt/versions/1/content" | jq '.variables | keys'
echo ""
echo "Version 2 variables:"
curl -s "$REGISTRY_URL/apis/registry/v3/groups/prompts/artifacts/summarizer-system-prompt/versions/2/content" | jq '.variables | keys'
echo ""

echo "--- Retrieve model metadata ---"
curl -s "$REGISTRY_URL/apis/registry/v3/groups/model-schemas/artifacts/customer-churn-predictor/versions/1/content" | jq '{name, version, framework, metrics}'
echo ""

echo "=== Discovery complete ==="
