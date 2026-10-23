# Presentation guide

Agent Sprawl Is the New Microservice Sprawl: Discovery and Governance for Multi-Agent AI

## Audience and central claim

The example creates a briefing: a fictional weather result from an MCP tool feeds an A2A summarizer, and that summary feeds an A2A translator. The calls and handoffs are real; the weather is fixture data. Application code chooses the roles and order. Registry discovery supplies endpoint metadata rather than planning the workflow.

## 30-minute run of show

### 0:00 — Metadata and execution travel different paths

Say: The example creates a briefing: a fictional weather result from an MCP tool feeds an A2A summarizer, and that summary feeds an A2A translator. The calls and handoffs are real; the weather is fixture data. Application code chooses the roles and order. Registry discovery supplies endpoint metadata rather than planning the workflow.

Show the corresponding diagram, source record or actual command output. Pause to identify the observation that supports the claim.

### 4:00 — One real handoff at each stage

Say: Removing the translation skill from an Agent Card must be rejected by the configured Registry rule. The checks inspect more than an HTTP error: the rejected version must be absent, the accepted content/version/globalId must remain unchanged, and the workflow must still run. A compatible addition then exercises the accepted path.

Show the corresponding diagram, source record or actual command output. Pause to identify the observation that supports the claim.

### 9:00 — Reject a removed translation skill

Say: My merged Quarkus LangChain4j change #2897 handles changed cards after an agent restart even when the declared agent version did not change. That is a distinct concern from rejecting incompatible publication: discovery is only useful when metadata is refreshed correctly. The fix includes a republishing regression test. The connected sample is separately pinned from open PR #2932.

Show the corresponding diagram, source record or actual command output. Pause to identify the observation that supports the claim.

### 15:00 — Retain accepted content and rerun

Say: A rejected card does not stop an independently deployed implementation from breaking callers. Registry versions observed before execution are not deployment pins or a transactional fleet snapshot. Static URLs may be sufficient for a small jointly deployed application. Use the responsibility matrix to decide what belongs in publication, deployment checks and runtime testing.

Show the corresponding diagram, source record or actual command output. Pause to identify the observation that supports the claim.

### 21:00 — Keep publisher metadata fresh

Say: Use the evidence boundary to choose the next action.

Show the corresponding diagram, source record or actual command output. Pause to identify the observation that supports the claim.

### 26:00 — Questions and buffer

Ask what the demonstration establishes and what it leaves open. Do not add another feature if time is short.

## Setup and fallback

```sh
python3 scripts/workflow.py test
```

Nine sample tests and live MCP/A2A smoke were recorded October 23 in the existing Paris branch. Those tests were not rerun by this packaging step. PR #2932 is open at the pinned revision; publisher fix #2897 is merged, release inclusion unchecked.

Prebuild/download dependencies. Keep a recording of an actual successful run; if unavailable, use the documented walkthrough and say it is not live execution. Never substitute fabricated success output.

## Questions to prepare

- What is the simplest alternative?
- Which failure was actually exercised?
- Which guarantee belongs to the application rather than the infrastructure?
- Which result changes with versions or workload?

## 25-minute adaptation

Use one central trace, shorten setup and reserve three minutes for questions. Move the secondary implementation example to backup.
