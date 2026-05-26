#!/bin/bash
# Tear down all demo containers
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "Stopping and removing containers..."
docker compose -f "$PROJECT_DIR/docker-compose.yaml" down -v
echo "Cleanup complete."
