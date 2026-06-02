#!/bin/bash
# Register model metadata schemas in Apicurio Registry
set -euo pipefail

REGISTRY_URL="${REGISTRY_URL:-http://localhost:8080}"
GROUP="model-schemas"

echo "=== Registering Model Metadata Schema ==="

# Model metadata JSON Schema
curl -s -X POST "$REGISTRY_URL/apis/registry/v3/groups/$GROUP/artifacts" \
  -H "Content-Type: application/json" \
  -d '{
    "artifactId": "model-context-schema",
    "artifactType": "JSON",
    "name": "ML Model Metadata Schema",
    "firstVersion": {
      "content": {
        "content": "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"$id\":\"urn:ml:model:metadata\",\"title\":\"MLModelMetadata\",\"type\":\"object\",\"required\":[\"name\",\"version\",\"framework\",\"artifactUri\",\"createdAt\"],\"properties\":{\"name\":{\"type\":\"string\",\"description\":\"Name of the model\"},\"version\":{\"type\":\"string\",\"description\":\"Semantic version\"},\"framework\":{\"type\":\"string\",\"enum\":[\"scikit-learn\",\"xgboost\",\"tensorflow\",\"pytorch\"]},\"artifactUri\":{\"type\":\"string\",\"description\":\"URI to model binary (S3, GCS, etc.)\"},\"createdAt\":{\"type\":\"string\",\"format\":\"date-time\"},\"createdBy\":{\"type\":\"string\"},\"inputSchema\":{\"type\":\"string\",\"description\":\"Reference to input schema\"},\"outputSchema\":{\"type\":\"string\",\"description\":\"Reference to output schema\"},\"hyperparameters\":{\"type\":\"object\"},\"metrics\":{\"type\":\"object\",\"properties\":{\"accuracy\":{\"type\":\"number\"},\"f1_score\":{\"type\":\"number\"},\"roc_auc\":{\"type\":\"number\"}}}},\"additionalProperties\":false}",
        "contentType": "application/json"
      }
    }
  }' | jq .artifact
echo ""

# Enable VALIDITY rule
echo "--- Enabling FULL validity enforcement ---"
curl -s -X POST "$REGISTRY_URL/apis/registry/v3/groups/$GROUP/artifacts/model-context-schema/rules" \
  -H "Content-Type: application/json" \
  -d '{"ruleType": "VALIDITY", "config": "FULL"}'
echo ""
echo "FULL validity enforcement enabled"
echo ""

echo "=== Registering sample model metadata ==="

# Valid model
echo "--- Submitting valid model ---"
curl -s -X POST "$REGISTRY_URL/apis/registry/v3/groups/$GROUP/artifacts" \
  -H "Content-Type: application/json" \
  -d '{
    "artifactId": "customer-churn-predictor",
    "artifactType": "JSON",
    "name": "Customer Churn Predictor",
    "firstVersion": {
      "content": {
        "content": "{\"name\":\"customer-churn-predictor\",\"version\":\"1.0.0\",\"framework\":\"scikit-learn\",\"artifactUri\":\"s3://models/churn/random-forest-v1.pkl\",\"createdAt\":\"2026-04-01T10:00:00Z\",\"createdBy\":\"data-science-team\",\"metrics\":{\"accuracy\":0.94,\"f1_score\":0.91,\"roc_auc\":0.96}}",
        "contentType": "application/json"
      }
    }
  }' | jq .artifact
echo ""

echo "=== Model schemas registered ==="
echo "View them at: $REGISTRY_URL/ui/artifacts?groupId=$GROUP"
