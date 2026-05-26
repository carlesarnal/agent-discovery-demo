#!/bin/bash
# Wait for Apicurio Registry to be healthy
set -euo pipefail

REGISTRY_URL="${REGISTRY_URL:-http://localhost:8080}"

echo "Waiting for Apicurio Registry at $REGISTRY_URL..."
until curl -sf "$REGISTRY_URL/health" > /dev/null 2>&1; do
  sleep 2
done
echo "Registry is ready!"
