#!/bin/bash
# Register model metadata schemas using MODEL_SCHEMA artifact type
set -euo pipefail

REGISTRY_URL="${REGISTRY_URL:-http://localhost:8080}"
GROUP="model-schemas"

echo "=== Registering Model Schema ==="

# Model metadata schema
curl -s -X POST "$REGISTRY_URL/apis/registry/v3/groups/$GROUP/artifacts" \
  -H "Content-Type: application/json" \
  -d '{
    "artifactId": "customer-churn-predictor",
    "artifactType": "MODEL_SCHEMA",
    "name": "Customer Churn Predictor",
    "description": "ML model for predicting customer churn",
    "firstVersion": {
      "content": {
        "content": "{\"modelId\":\"customer-churn-predictor\",\"provider\":\"internal\",\"version\":\"1.0.0\",\"input\":{\"type\":\"object\",\"properties\":{\"customer_id\":{\"type\":\"string\"},\"tenure_months\":{\"type\":\"integer\"},\"monthly_charges\":{\"type\":\"number\"},\"total_charges\":{\"type\":\"number\"}}},\"output\":{\"type\":\"object\",\"properties\":{\"churn_probability\":{\"type\":\"number\"},\"risk_category\":{\"type\":\"string\",\"enum\":[\"low\",\"medium\",\"high\"]}}},\"modelDetails\":{\"name\":\"Customer Churn Predictor\",\"overview\":\"Random Forest classifier trained on customer behavior data\",\"owners\":[{\"name\":\"data-science-team\"}]},\"considerations\":{\"users\":[\"Customer Success\",\"Retention Team\"],\"limitations\":[\"Trained on US market data only\"]}}",
        "contentType": "application/json"
      }
    }
  }' | jq .artifact
echo ""

# Enable BACKWARD compatibility
echo "--- Enabling BACKWARD compatibility rule ---"
curl -s -X POST "$REGISTRY_URL/apis/registry/v3/groups/$GROUP/artifacts/customer-churn-predictor/rules" \
  -H "Content-Type: application/json" \
  -d '{"ruleType": "COMPATIBILITY", "config": "BACKWARD"}'
echo ""
echo "BACKWARD compatibility enabled for model schema"

echo ""
echo "=== Model schema registered (type: MODEL_SCHEMA) ==="
echo "View at: $REGISTRY_URL/ui/artifacts?groupId=$GROUP"
