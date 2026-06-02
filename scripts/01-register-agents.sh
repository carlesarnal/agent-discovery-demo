#!/bin/bash
# Register A2A Agent Cards in Apicurio Registry
set -euo pipefail

REGISTRY_URL="${REGISTRY_URL:-http://localhost:8080}"
GROUP="ai-agents"

echo "=== Registering A2A Agent Cards ==="

# Summarizer Agent
echo "--- Registering Summarizer Agent ---"
curl -s -X POST "$REGISTRY_URL/apis/registry/v3/groups/$GROUP/artifacts" \
  -H "Content-Type: application/json" \
  -d '{
    "artifactId": "summarizer-agent",
    "artifactType": "JSON",
    "name": "Summarizer Agent",
    "description": "Summarizes long documents into concise abstracts",
    "firstVersion": {
      "content": {
        "content": "{\"name\":\"Summarizer Agent\",\"description\":\"Summarizes long documents into concise abstracts\",\"url\":\"https://agents.internal/summarizer\",\"version\":\"1.0.0\",\"capabilities\":{\"streaming\":true,\"pushNotifications\":false},\"skills\":[{\"id\":\"summarize-text\",\"name\":\"Text Summarization\",\"description\":\"Produces a concise summary of input text\",\"inputModes\":[\"text\"],\"outputModes\":[\"text\"]},{\"id\":\"summarize-pdf\",\"name\":\"PDF Summarization\",\"description\":\"Extracts and summarizes PDF content\",\"inputModes\":[\"application/pdf\"],\"outputModes\":[\"text\"]}],\"defaultInputModes\":[\"text\"],\"defaultOutputModes\":[\"text\"]}",
        "contentType": "application/json"
      }
    }
  }' | jq .artifact
echo ""

# Translator Agent
echo "--- Registering Translator Agent ---"
curl -s -X POST "$REGISTRY_URL/apis/registry/v3/groups/$GROUP/artifacts" \
  -H "Content-Type: application/json" \
  -d '{
    "artifactId": "translator-agent",
    "artifactType": "JSON",
    "name": "Translator Agent",
    "description": "Translates text between languages using LLM-powered translation",
    "firstVersion": {
      "content": {
        "content": "{\"name\":\"Translator Agent\",\"description\":\"Translates text between languages using LLM-powered translation\",\"url\":\"https://agents.internal/translator\",\"version\":\"1.0.0\",\"capabilities\":{\"streaming\":true,\"pushNotifications\":false},\"skills\":[{\"id\":\"translate-text\",\"name\":\"Text Translation\",\"description\":\"Translates text from one language to another\",\"inputModes\":[\"text\"],\"outputModes\":[\"text\"]}],\"defaultInputModes\":[\"text\"],\"defaultOutputModes\":[\"text\"]}",
        "contentType": "application/json"
      }
    }
  }' | jq .artifact
echo ""

# Data Enrichment Agent
echo "--- Registering Data Enrichment Agent ---"
curl -s -X POST "$REGISTRY_URL/apis/registry/v3/groups/$GROUP/artifacts" \
  -H "Content-Type: application/json" \
  -d '{
    "artifactId": "data-enrichment-agent",
    "artifactType": "JSON",
    "name": "Data Enrichment Agent",
    "description": "Enriches structured data with external sources and LLM-powered inference",
    "firstVersion": {
      "content": {
        "content": "{\"name\":\"Data Enrichment Agent\",\"description\":\"Enriches structured data with external sources and LLM-powered inference\",\"url\":\"https://agents.internal/enrichment\",\"version\":\"2.0.0\",\"capabilities\":{\"streaming\":true,\"batchProcessing\":true},\"skills\":[{\"id\":\"enrich-customer\",\"name\":\"Customer Enrichment\",\"description\":\"Enriches customer records with inferred attributes\",\"inputModes\":[\"json\"],\"outputModes\":[\"json\"]},{\"id\":\"enrich-address\",\"name\":\"Address Validation\",\"description\":\"Validates and normalizes addresses\",\"inputModes\":[\"json\"],\"outputModes\":[\"json\"]}],\"defaultInputModes\":[\"json\"],\"defaultOutputModes\":[\"json\"]}",
        "contentType": "application/json"
      }
    }
  }' | jq .artifact

echo ""
echo "=== 3 agents registered ==="
echo "View them at: $REGISTRY_URL/ui/artifacts?groupId=$GROUP"
