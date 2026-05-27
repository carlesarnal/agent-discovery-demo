# Presentation Guide — API Days Amsterdam 2026

Speaker notes and setup instructions for the **From OpenAPI to Agent Cards** demo.

## Pre-Talk Setup (15 min before)

1. Start Apicurio Registry:
   ```bash
   docker compose up -d
   ./scripts/wait-for-registry.sh
   ```

2. Open browser tabs:
   - **Registry UI**: http://localhost:8080
   - **Slides**: open `presentation.html` locally

3. Verify clean state:
   ```bash
   curl -s http://localhost:8080/apis/registry/v3/search/artifacts | jq '.count'
   # Should return 0
   ```

4. Have a terminal ready with the scripts directory.

## Demo Flow

### Act 1: The Standards Arc + The Problem (slides, ~5 min)

- Open with the OpenAPI → AsyncAPI → A2A evolution: each generation of distributed systems standardized interface descriptions
- Show the "what breaks" scenario: Team A updates a prompt, Team B's agent silently degrades
- Connect to the audience: "You already govern OpenAPI specs. AI agents need the same treatment."

### Act 2: Agent Registration (live demo, ~5 min)

```bash
./scripts/01-register-agents.sh
```

**What to show:**
- Run the script — 3 agents registered
- Switch to Registry UI → `ai-agents` group
- Click into `summarizer-agent` → show the Agent Card JSON (capabilities, skills)
- Point out: this is the A2A Protocol Agent Card format

**Talking point:** "Think of Agent Cards as OpenAPI specs for AI agents. Instead of hardcoding URLs, agents register their capabilities in the same registry that holds your OpenAPI and AsyncAPI definitions."

### Act 3: Prompt Governance (live demo, ~7 min)

```bash
./scripts/02-register-prompts.sh
```

**What to show:**
- Show v1 of `summarizer-system-prompt` in the UI
- Show the BACKWARD compatibility rule on the artifact
- Show v2 was accepted (added optional `tone` variable)
- Click version history → two versions

**Talking point:** "Adding an optional variable with a default is safe — existing agents won't break. The registry enforces this automatically."

### Act 4: Model Schema Validation (live demo, ~3 min)

```bash
./scripts/03-register-model-schemas.sh
```

**What to show:**
- Show the JSON Schema for model metadata
- Show the valid model that was accepted
- Point out: FULL validity enforcement prevents malformed model metadata

### Act 5: Discovery in Action (live demo, ~5 min)

```bash
./scripts/04-discover-agents.sh
```

**What to show:**
- List all agents → structured output
- Search by name → find the Translator
- Compare prompt versions → v1 has 2 variables, v2 has 3
- Retrieve model metadata → accuracy, framework, URI

**Talking point:** "This is what an orchestrator agent does at runtime — query the registry to find the right agent for the job, check its capabilities, and discover how to interact with it."

### Act 6: Breaking Change Protection (live demo, ~3 min)

```bash
./scripts/05-breaking-change.sh
```

**What to show:**
- Attempt to register a prompt that removes `input_text` and renames it to `content`
- Show the rejection — HTTP 409 with error details
- Emphasize: this catches the problem before production, not after

**Talking point:** "This is the same compatibility checking that protects your OpenAPI specs. The registry doesn't care if it's an API definition or a prompt template — the governance pattern is identical."

### Act 7: Takeaways (slides, ~2 min)

1. OpenAPI → AsyncAPI → A2A: the open standards arc continues into AI agents
2. A2A Agent Cards are "OpenAPI for agents" — structured, versioned, discoverable
3. Prompt templates need the same compatibility rules as API specs
4. Apicurio Registry — one CNCF sandbox tool for OpenAPI, AsyncAPI, and A2A artifacts

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Registry not starting | Check Docker is running, port 8080 is free |
| Scripts fail with connection refused | Run `./scripts/wait-for-registry.sh` first |
| jq not found | `brew install jq` or `apt install jq` |
| Want to reset state | `./scripts/cleanup.sh && docker compose up -d` |

## Timing

| Section | Duration |
|---------|----------|
| Standards Arc + Problem (slides) | 5 min |
| Agent Registration | 5 min |
| Prompt Governance | 7 min |
| Model Schemas | 3 min |
| Discovery | 5 min |
| Breaking Change | 3 min |
| Takeaways | 2 min |
| **Total** | **~30 min** |
