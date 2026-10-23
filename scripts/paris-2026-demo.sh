#!/bin/bash
# Start the connected workflow; run workflow.py prepare once beforehand.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
exec python3 "$ROOT/scripts/workflow.py" run
