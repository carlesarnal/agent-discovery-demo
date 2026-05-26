#!/bin/bash
# Register versioned prompt templates in Apicurio Registry
set -euo pipefail

REGISTRY_URL="${REGISTRY_URL:-http://localhost:8080}"
GROUP="prompts"

echo "=== Registering Prompt Templates ==="

# System prompt v1
echo "--- Registering summarizer-system-prompt v1 ---"
curl -s -X POST "$REGISTRY_URL/apis/registry/v3/groups/$GROUP/artifacts" \
  -H "Content-Type: application/json" \
  -H "X-Registry-ArtifactId: summarizer-system-prompt" \
  -H "X-Registry-ArtifactType: JSON" \
  -d '{
    "template": "You are a summarization assistant. Summarize the following text in {{max_sentences}} sentences.\n\nText: {{input_text}}\n\nSummary:",
    "variables": {
      "max_sentences": {"type": "integer", "default": 3},
      "input_text": {"type": "string", "required": true}
    },
    "metadata": {
      "model": "llama3.2",
      "temperature": 0.3,
      "version": "1.0.0"
    }
  }' | jq .
echo ""

# Enable BACKWARD compatibility
echo "--- Enabling BACKWARD compatibility rule ---"
curl -s -X POST "$REGISTRY_URL/apis/registry/v3/groups/$GROUP/artifacts/summarizer-system-prompt/rules" \
  -H "Content-Type: application/json" \
  -d '{"ruleType": "COMPATIBILITY", "config": "BACKWARD"}'
echo ""
echo "BACKWARD compatibility enabled for summarizer-system-prompt"
echo ""

# System prompt v2 (compatible: adds optional variable)
echo "--- Creating version 2 (compatible change: add optional 'tone' variable) ---"
curl -s -X POST "$REGISTRY_URL/apis/registry/v3/groups/$GROUP/artifacts/summarizer-system-prompt/versions" \
  -H "Content-Type: application/json" \
  -d '{
    "template": "You are a summarization assistant. Summarize the following text in {{max_sentences}} sentences. Use a {{tone}} tone.\n\nText: {{input_text}}\n\nSummary:",
    "variables": {
      "max_sentences": {"type": "integer", "default": 3},
      "input_text": {"type": "string", "required": true},
      "tone": {"type": "string", "default": "neutral"}
    },
    "metadata": {
      "model": "llama3.2",
      "temperature": 0.3,
      "version": "2.0.0"
    }
  }' | jq .
echo ""
echo "Version 2 registered successfully (backward compatible)"
echo ""

# Chat prompt template
echo "--- Registering chat-prompt template ---"
curl -s -X POST "$REGISTRY_URL/apis/registry/v3/groups/$GROUP/artifacts" \
  -H "Content-Type: application/json" \
  -H "X-Registry-ArtifactId: chat-prompt" \
  -H "X-Registry-ArtifactType: JSON" \
  -d '{
    "template": "{{system_prompt}}\n\nConversation history:\n{{conversation_history}}\n\nUser: {{question}}\n\nAssistant:",
    "variables": {
      "system_prompt": {"type": "string", "required": true},
      "question": {"type": "string", "required": true},
      "conversation_history": {"type": "string", "default": ""},
      "include_examples": {"type": "boolean", "default": false}
    }
  }' | jq .

echo ""
echo "=== Prompt templates registered ==="
echo "View them at: $REGISTRY_URL/ui/artifacts?groupId=$GROUP"
