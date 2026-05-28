# Presentation Guide — API Days Amsterdam 2026

**Session:** From OpenAPI to Agent Cards: Governing AI Discovery with Open Standards  
**Speaker:** Carles Arnal — Principal Software Engineer  
**Conference:** API Days Amsterdam 2026  
**Time Slot:** 2:55 PM – 3:20 PM (25 minutes)

---

## Pre-Presentation Setup (do all of this BEFORE the talk)

### Infrastructure (10 min before)

```bash
# Start Apicurio Registry
docker compose up -d

# Wait for it to be ready
./scripts/wait-for-registry.sh

# Verify clean state
curl -s http://localhost:8080/apis/registry/v3/search/artifacts | jq '.count'
# Should return 0
```

### Browser tabs (pre-load)

1. `http://localhost:8080` — Apicurio Registry UI
2. `presentation.html` — The slide deck (open locally or via `python3 -m http.server 8082`)

### Terminal

Have a terminal open in the `scripts/` directory, ready to run demo commands.

### Verify everything is ready

```bash
curl -sf http://localhost:8080/health | jq .status
# Should return "UP"
```

---

## Part 1 — Introduction (3 min)

### Slide 0: Title

- "Hi everyone, I'm Carles Arnal, Principal Software Engineer. I work on Apicurio Registry, which is an open source schema and API registry — and a CNCF sandbox project. Today I want to talk about how the same open standards approach that governs your REST APIs can now govern AI agents."
- "This is an API conference, so let me start with something you already know well."

### Slide 1: The Standards Arc

- "Every generation of distributed systems solved the same problem. In 2015, OpenAPI standardized how we describe REST APIs — structured interface definitions, machine-readable, versioned. Before OpenAPI, every team described their APIs differently. Some used Word documents, some used wiki pages, some didn't document at all. OpenAPI changed that by giving us a standard format that tools could consume."
- "In 2019, AsyncAPI did the same thing for event-driven architectures. If you're running Kafka or RabbitMQ, AsyncAPI describes your channels, message formats, and bindings in a standard way."
- "Now, in 2025, the A2A Protocol — the Agent-to-Agent Protocol by Google — brings that same approach to AI agents. Structured capability declarations, typed interfaces, machine-readable discovery."
- "The pattern is the same one we've relied on for a decade: describe your interface in a standard format, register it, version it, and let consumers discover it programmatically. The only thing that changes is the artifact type."

### Slide 2: The Problem

- "But here's the thing — most organizations building with AI agents aren't doing any of this yet. There's no governance, no versioning, no compatibility checking."
- "Here's a scenario that's happening right now in hundreds of companies. Team A maintains a summarization agent. They update a prompt template — maybe they rename a variable from `input_text` to `content`. Seems harmless. But Team B's orchestrator agent was passing `input_text`. Now it silently gets ignored. The agent doesn't crash — it just produces garbage output. No error, no alert, no trace."
- "Sound familiar? It should. This is the exact same problem APIs had before OpenAPI and schema registries. One team changes a response format, and downstream consumers silently break."
- "OpenAPI solved this for REST. AsyncAPI solved it for event-driven. AI agents need the same treatment."

---

## Part 2 — The Solution (4 min)

### Slide 3: Section divider

- "So what's the solution? We don't need new tooling. We need to extend the tooling that already works."

### Slide 4: Apicurio Registry

- "This is Apicurio Registry. It's a CNCF sandbox project that already governs OpenAPI specs, AsyncAPI definitions, Avro schemas, Protobuf definitions, JSON Schema — all the interface contracts you're probably already managing."
- "What we've done is extend it with AI-native artifact types: A2A Agent Cards, prompt templates, model schemas, and MCP tool definitions. The governance framework is identical — versioning, compatibility rules, schema validation, lifecycle management. Same tool, new artifacts."
- "If you're using Apicurio Registry or Confluent Schema Registry for your Kafka schemas today, you already understand the pattern. We're applying it to AI agents."
- "And because it's a registry — not a config file, not a wiki page — it's queryable. Agents can discover each other at runtime by querying the registry for capabilities, input/output modes, or skills. It's the same evolution that APIs went through: from hardcoded URLs to API gateways to OpenAPI-driven developer portals."

### Slide 5: Architecture

- "Here's the architecture. Agent A registers its A2A Agent Card in the registry — capabilities, skills, endpoint URL. Agent B, the orchestrator, queries the registry to discover agents by capability. It doesn't need to know any URLs in advance."
- "The registry enforces three things: compatibility rules — so a breaking change to a prompt template is rejected before it reaches production. Schema validation — so malformed model metadata is rejected at registration time. And version history — so you can audit every change and roll back if needed."
- "This is the same pattern as registering an OpenAPI spec in a developer portal and letting consumers discover APIs programmatically. The difference is that the consumers are agents, not humans."

### Slide 6: A2A Protocol

- "Let me quickly explain the A2A Protocol. It's Google's open standard for agent-to-agent communication. The key construct is the Agent Card — a structured JSON document that declares an agent's name, URL, version, capabilities, and skills."
- "Think of it as an OpenAPI spec for an AI agent. An OpenAPI spec describes endpoints, request/response schemas, and authentication. An Agent Card describes skills, input/output modes, and capabilities like streaming or batch processing."
- "The protocol defines a well-known endpoint at `/.well-known/agent.json`, similar to how OAuth uses `/.well-known/openid-configuration`. But the well-known endpoint requires knowing the host first — you need a URL to discover the URL. Registry-backed discovery removes that limitation entirely. You can query by capability without knowing any URLs."

---

## Part 3 — Live Demo (10 min)

### Slide 7: Section divider

- "Alright, time for the live demo. Everything from here is live — real terminal commands against a real Apicurio Registry instance. Let me show you the open standards arc in action."

### Slide 8: Start Registry

**What to do:**
- Registry should already be running from pre-talk setup
- Run the `curl` command to show zero artifacts
- Switch to the Registry UI browser tab — show the empty state

**Script:**

- "The registry is already running. Let me verify it's clean — zero artifacts. And here's the Apicurio Registry UI. If you've used it before for OpenAPI or Avro schemas, this looks familiar. Same UI, same groups, same version history. We're just going to put different artifacts in it."

### Slide 9: Register Agents

**Type in terminal:**

```bash
./scripts/01-register-agents.sh
```

**What to show:**
- Run the script — watch three agents register
- Switch to Registry UI → click `ai-agents` group
- Click into `summarizer-agent` → show the Agent Card JSON
- Point out: capabilities (streaming: true), skills (Text Summarization, PDF Summarization), input/output modes

**Script:**

- "Let's register three agents. Each one is an A2A Agent Card — a structured JSON document describing the agent's capabilities."
- "We have a Summarizer that handles text and PDF input, a Translator for text-to-text translation, and a Data Enrichment agent that works with structured JSON and supports batch processing."
- *Switch to UI* "Here they are in the registry, under the ai-agents group. Let me click into the Summarizer. You can see the full Agent Card — name, URL, version, capabilities, and two skills with typed input/output modes."
- "This is the A2A Protocol Agent Card format, stored in the same registry as your OpenAPI specs. Any agent in the system can now query the registry to discover these agents by capability — no hardcoded URLs, no config files."

### Slide 10: Prompt Versioning

**Type in terminal:**

```bash
./scripts/02-register-prompts.sh
```

**What to show:**
- Run the script
- Switch to Registry UI → click `prompts` group → click `summarizer-system-prompt`
- Show version 1 content — two variables: `max_sentences` and `input_text`
- Show the BACKWARD compatibility rule on the artifact (click Rules tab)
- Show version 2 — added `tone` variable with default `"neutral"`
- Click version history — show both versions side by side

**Script:**

- "Now let's register prompt templates. This is where the governance story gets interesting."
- "Version 1 of the summarizer prompt has two variables: `max_sentences` with a default of 3, and `input_text` which is required."
- "I've enabled a BACKWARD compatibility rule on this artifact. This means new versions must be consumable by agents using the previous version — same rule you'd apply to an OpenAPI response schema."
- "Version 2 adds an optional `tone` variable with a default of `neutral`. This is accepted — it's backward compatible because existing agents that don't know about `tone` will still work. The default kicks in."
- *Click version history* "And here's the version history — both versions stored, auditable, rollback-ready."

### Slide 11: Breaking Change

**Type in terminal:**

```bash
./scripts/05-breaking-change.sh
```

**What to show:**
- Run the script — watch it get rejected
- Show the HTTP 409 response with error details
- This is the dramatic demo moment — pause and let the rejection sink in

**Script:**

- "Now let's try something that should fail. I'm going to register a version 3 that removes the required `input_text` variable and renames it to `content`."
- *Run the script* "HTTP 409 — rejected. The registry blocked this change because it violates the BACKWARD compatibility rule. Removing a required variable breaks downstream agents that depend on it."
- "This is the same protection you get with OpenAPI schemas. You can't remove a required field from a response schema in a backward-compatible way. The registry catches this at registration time — before any data flows, before any agent breaks."
- "In a world without this governance, Team A would rename that variable, push it to production, and Team B's agent would silently start producing garbage. With the registry, the change is blocked at the source."

### Slide 12: Agent Discovery

**Type in terminal:**

```bash
./scripts/04-discover-agents.sh
```

**What to show:**
- List all agents — structured JSON output
- Search by name — find the Translator
- Inspect the Summarizer's capabilities — skills, streaming support
- Compare prompt versions — v1 has 2 variables, v2 has 3
- Retrieve model metadata — accuracy, framework, artifact URI

**Script:**

- "Finally, let's see discovery in action. This is what an orchestrator agent does at runtime."
- "First, list all agents in the registry. I get structured output — agent IDs, names, descriptions. The orchestrator can filter by whatever it needs."
- "Search by name — I'm looking for a translator. Found it, with its full description."
- "Inspect the Summarizer's capabilities — it supports streaming, has two skills, accepts text and PDF input. An orchestrator agent can programmatically decide whether this agent fits its needs."
- "Compare prompt versions — version 1 has two variables, version 2 has three. An agent can choose which version to use."
- "And retrieve model metadata — the customer churn predictor, scikit-learn framework, 94% accuracy. All queryable via the registry API."
- "No hardcoded URLs. No config files. Just a standard API query against the registry. Same pattern as an API developer portal, but for agents."

---

## Part 4 — Why This Matters (4 min)

### Slide 13: Section divider

- "Let me step back from the demo and explain why this matters beyond the technical implementation."

### Slide 14: Discovery Approaches

- "There are four ways to do agent discovery, and each one has a limitation."
- "Hardcoded URLs — you put agent endpoints in a config file. This breaks every time an endpoint changes or an agent moves. It's the equivalent of hardcoding API base URLs in your frontend code."
- "DNS and service meshes — Istio, Consul, Kubernetes DNS. These discover *services* — they know 'this pod exists on port 8080'. But they don't discover *capabilities*. They can't tell you 'this agent summarizes PDFs and supports streaming'."
- "The A2A well-known endpoint is better — it describes capabilities in a standard format. But you need to know the host first. You need a URL to get the URL. That's a chicken-and-egg problem."
- "Registry-backed discovery solves all three. You can query by capability, by skill, by input mode — without knowing any URLs. It's the same evolution that APIs went through: from hardcoded URLs to API gateways to OpenAPI-driven developer portals."

### Slide 15: Compatibility Rules

- "The compatibility rules are identical to what you already know from OpenAPI and Avro schema evolution."
- "Adding an optional field with a default — compatible. Whether it's adding an optional property to an OpenAPI response schema or adding an optional variable to a prompt template."
- "Removing a required field — breaking change. Whether it's removing a required property from an API response or removing a required variable from a prompt."
- "The governance pattern doesn't care what the artifact is. It cares about the structural contract: can consumers of the old version still consume the new version? That's backward compatibility. Same definition, same enforcement, new artifact types."

---

## Part 5 — Production and Wrap Up (4 min)

### Slide 16: Section divider

- "Let's quickly talk about what you'd add to take this from a demo to a real deployment."

### Slide 17: Production Considerations

- "Four things you'd add for production."
- "First, automated registration. In the demo I ran scripts manually. In production, agents register on startup via a sidecar or init container — similar to how services register with Consul. They deregister on shutdown. No manual intervention."
- "Second, health-aware discovery. Combine the registry with Kubernetes health checks. The registry should only return healthy agents. If an agent's pod is crashing, it should be pruned from discovery results."
- "Third, schema evolution strategy. Define compatibility modes per artifact group — BACKWARD for prompt templates, FULL for agent cards. Enforce in CI/CD so incompatible changes are rejected before merge, not at deploy time."
- "Fourth, multi-tenancy. Use registry groups to isolate agent namespaces across teams — the same way you'd separate OpenAPI specs per domain or per team. The registry supports fine-grained access control."

### Slide 18: Key Takeaways

- "Let me wrap up with four takeaways."
- "One: the open standards arc continues. OpenAPI standardized REST APIs. AsyncAPI standardized event-driven architectures. The A2A Protocol standardizes AI agent interfaces. Each generation solved the same problem — describe your interface, register it, version it, discover it."
- "Two: Agent Cards are OpenAPI for agents. They're structured capability declarations — versioned, discoverable, and machine-readable. If you know how to work with OpenAPI specs, you already know how to work with Agent Cards."
- "Three: prompt templates need governance. The same version control and compatibility rules you apply to API specs are not optional for AI. Without them, you get silent failures, untraceable bugs, and broken pipelines."
- "Four: the tooling already exists. Apicurio Registry — one CNCF sandbox project — governs OpenAPI, AsyncAPI, Avro, Protobuf, and now A2A Agent Cards, prompt templates, and model schemas. Same tool, same rules, new artifact types."

### Slide 19: Thank You

- "Thank you. The entire demo — scripts, schemas, this presentation — is on GitHub. The link is right here on the slide. Feel free to fork it, run it, and try it with your own agents."
- "I'm happy to take any questions."

---

## Troubleshooting Quick Reference

| Problem | Fix |
|---------|-----|
| Registry not starting | Check Docker is running, port 8080 is free: `docker ps` |
| Scripts fail with connection refused | Run `./scripts/wait-for-registry.sh` first |
| `jq` not found | `brew install jq` (macOS) or `apt install jq` (Linux) |
| Breaking change not rejected | Compatibility rule may not be set; re-run `02-register-prompts.sh` which enables BACKWARD |
| Want to reset state | `./scripts/cleanup.sh && docker compose up -d && ./scripts/wait-for-registry.sh` |
| Port 8080 in use | `docker stop $(docker ps -q --filter publish=8080)` or change port in docker-compose.yaml |
| Registry UI not loading | Clear browser cache or try incognito; check `curl http://localhost:8080/health` |

---

## Potential Q&A Questions

### A2A Protocol / Agent Discovery

**"Why not just use the A2A well-known endpoint?"**
- The well-known endpoint is great for direct agent-to-agent discovery when you already know the host. But in an enterprise with dozens of agents deployed across multiple clusters, you don't know all the hosts in advance. Registry-backed discovery lets you query by capability — "find me all agents that support PDF summarization with streaming" — without knowing any URLs. It's the same reason we have API developer portals instead of just OpenAPI specs at each service's well-known endpoint.

**"How does this compare to MCP (Model Context Protocol)?"**
- A2A and MCP are complementary. MCP defines how tools and resources are exposed to LLMs — it's about what an agent *can do internally*. A2A defines how agents discover and communicate with *each other* — it's about inter-agent interaction. You'd use both: MCP to describe an agent's internal tools, A2A to describe how other agents can interact with it. The registry can store both MCP tool definitions and A2A Agent Cards.

**"Is the A2A Protocol production-ready?"**
- The protocol specification is mature and backed by Google. It's being adopted by multiple frameworks and vendors. Apicurio Registry's support for A2A Agent Cards means you get production-grade governance — versioning, compatibility, validation — on top of the protocol from day one.

### Apicurio Registry / Schema Governance

**"Why Apicurio Registry and not Confluent Schema Registry?"**
- Confluent Schema Registry is excellent for Kafka-centric deployments with Avro and Protobuf. Apicurio Registry supports a broader range of artifact types — OpenAPI, AsyncAPI, JSON Schema, GraphQL, and now A2A Agent Cards and prompt templates. It's also a CNCF sandbox project, which means it's vendor-neutral and cloud-native. For an AI agent governance use case, the broader artifact support matters.

**"How do you handle agents that don't use the A2A Protocol?"**
- The registry is artifact-type agnostic. You can store any JSON document as a registry artifact. If your agents use a custom capability format, you can register that format and still get versioning, compatibility checking, and discovery. The A2A Protocol just gives you a standard format that tooling can consume.

**"What about latency? Doesn't querying a registry add overhead?"**
- Agent discovery is a startup-time or cache-refreshing operation, not a per-request call. An orchestrator agent queries the registry once, caches the results, and refreshes periodically — similar to how a service mesh refreshes its routing tables. The actual agent-to-agent communication goes direct, not through the registry.

### Prompt Governance

**"Can the registry actually detect prompt breaking changes, or is it just JSON diff?"**
- In the current demo, compatibility checking works at the JSON structure level — same as Avro or JSON Schema compatibility. It detects added/removed fields, type changes, and required/optional changes. For prompt-specific semantics (e.g., detecting that a template's *meaning* changed even if the structure didn't), you'd need custom validation rules or an LLM-based compatibility checker — an interesting extension but not yet implemented.

**"Who should own prompt templates — the platform team or the AI team?"**
- Both, with the registry as the boundary. The AI team creates and evolves prompt templates. The platform team sets compatibility rules and CI/CD enforcement. This mirrors how API teams own their OpenAPI specs but platform teams enforce compatibility rules and deployment gates.

**"What about prompt templates that use different LLMs?"**
- The prompt template includes metadata about the target model (e.g., `"model": "llama3.2"`). Different versions can target different models. The registry tracks all versions, and agents can query for prompts targeting a specific model. This is analogous to API versions targeting different backend implementations.

### Production / Operations

**"How does this scale to hundreds of agents?"**
- Apicurio Registry supports PostgreSQL and Kafka-based storage backends for production deployments. Registry groups provide namespacing — you'd organize agents by team, domain, or environment. The search API supports filtering by group, name, labels, and content. Performance is comparable to any REST API backed by a database — discovery queries return in milliseconds.

**"Can you enforce governance in CI/CD?"**
- Yes. You'd add a CI step that validates new artifacts against the registry's compatibility rules before merging. Apicurio provides a REST API and a Maven plugin for this. The pattern is: developer changes a prompt template, CI validates it against the registry, if it passes compatibility checks it gets merged and registered, if not the PR is blocked. Same pattern as validating OpenAPI spec changes in CI.

---

## Timing Summary

**Time slot: 2:55 PM – 3:20 PM (25 minutes)**

| Section | Duration | Cumulative | Clock |
|---------|----------|------------|-------|
| Introduction (Title + Standards Arc + Problem) | 3 min | 3 min | 2:58 |
| The Solution (Registry + Architecture + A2A Protocol) | 4 min | 7 min | 3:02 |
| Live Demo (Register + Prompts + Breaking Change + Discovery) | 10 min | 17 min | 3:12 |
| Why This Matters (Discovery Approaches + Compatibility) | 4 min | 21 min | 3:16 |
| Production + Wrap Up (Production + Takeaways + Thank You) | 4 min | 25 min | 3:20 |
| **Total** | **25 min** | | **3:20** |

**Pacing notes:**
- Start the presenter timer (press `T`) at 2:55
- If running long at the demo stage, skip the model schema section in discovery (slide 12) and go straight to the breaking change highlight
- The production slide (17) can be covered briefly — the takeaways (18) are the essential closing
- If Q&A is separate, you have a small buffer; if Q&A is within the 25 min, trim the demo to 8 min
