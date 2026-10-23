# Verification ledger

Prepared October 23, 2026.

Nine sample tests and live MCP/A2A smoke were recorded October 23 in the existing Paris branch. Those tests were not rerun by this packaging step. PR #2932 is open at the pinned revision; publisher fix #2897 is merged, release inclusion unchecked.

## Reproduce

```sh
python3 scripts/workflow.py test
```

A documentation or model check is not an integration test. New results must record source revision, runtime versions, command, observed outcome and remaining limits. No acceptance score is guaranteed.

## Package checks

Offline deck navigation/layout and documentation links are checked separately from application behavior. No claim of a five-star speaking performance is made by these artifacts.
