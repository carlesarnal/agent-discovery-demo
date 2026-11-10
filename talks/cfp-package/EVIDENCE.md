# Evidence and claim boundaries

The sample (merged as #2932) and publisher fix #2897 both ship in Quarkus LangChain4j 1.15.0. Its nine tests and the live MCP/A2A smoke and governance checks were rerun against that release in November 2026; see `../cloud-native-paris-2026/RUNBOOK.md`.

## Run one connected workflow

The example creates a briefing: a fictional weather result from an MCP tool feeds an A2A summarizer, and that summary feeds an A2A translator. The calls and handoffs are real; the weather is fixture data. Application code chooses the roles and order. Registry discovery supplies endpoint metadata rather than planning the workflow.

## Inspect the rejected publication

Removing the translation skill from an Agent Card must be rejected by the configured Registry rule. The checks inspect more than an HTTP error: the rejected version must be absent, the accepted content/version/globalId must remain unchanged, and the workflow must still run. A compatible addition then exercises the accepted path.

## Keep the publisher honest too

My merged Quarkus LangChain4j change #2897 handles changed cards after an agent restart even when the declared agent version did not change. That is a distinct concern from rejecting incompatible publication: discovery is only useful when metadata is refreshed correctly. The fix includes a republishing regression test. The connected sample is part of the same release.

## Understand what the test does not prove

A rejected card does not stop an independently deployed implementation from breaking callers. Registry versions observed before execution are not deployment pins or a transactional fleet snapshot. Static URLs may be sufficient for a small jointly deployed application. Use the responsibility matrix to decide what belongs in publication, deployment checks and runtime testing.
