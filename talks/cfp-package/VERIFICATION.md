# Verification ledger

Prepared October 23, 2026; updated November 2026.

The sample (merged as #2932) and publisher fix #2897 both ship in Quarkus LangChain4j 1.15.0. Its nine tests and the live MCP/A2A smoke and governance checks were rerun against that release in November 2026; see `../cloud-native-paris-2026/RUNBOOK.md`.

## Reproduce

```sh
python3 scripts/workflow.py prepare   # from the repository root
```

A documentation or model check is not an integration test. New results must record source revision, runtime versions, command, observed outcome and remaining limits.

## Package checks

Offline deck navigation/layout and documentation links are checked separately from application behavior.
