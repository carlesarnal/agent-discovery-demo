# Connected workflow — rehearsal notes

## Timing (25 minutes; add discussion for a 30-minute slot)

1. **0–3:** A caller needs a tool and two agents. Who owns discovery, sequencing and contracts?
2. **3–6:** Architecture: programmatic Agent Card publication, explicit MCP registration,
   endpoint discovery through extensions, fixed application roles.
3. **6–12:** Submit one briefing. Inspect weather output in summarizer input, then the
   exact summary in translator input. Show the observed contract versions.
4. **12–18:** Run `python3 scripts/workflow.py governance`. Show the precise removed-skill
   rejection, unchanged accepted registration, successful rerun and compatible addition.
5. **18–22:** Explain metadata versus deployments, observation versus version pinning,
   and what would require behavioral tests or a lifecycle policy.
6. **22–25:** Questions.

## Preflight

- Build the pinned source with `python3 scripts/workflow.py prepare`.
- Run the smoke check with the intended hardware/model and inspect the outputs.
- Start the interactive runner and open http://localhost:11020/.
- Open slides separately; no embedded terminal server is required.
- Record a backup of the same revision. Do not describe a recording as live execution.
- Model quality is a human evaluation; smoke assertions verify handoffs and execution,
  not the truth of every generated sentence.

## Claims to avoid

- No runtime negotiation, automatic dependency graph or general-purpose agent planner.
- No consumer-side compatibility/deprecation gate, deployment pinning or hot upgrade.
- An added metadata skill is not a newly implemented skill in the running translator.
- A successful rerun after rejection proves the unchanged demo still works, not that
  Registry enforces all future deployment behavior.
- Paris is accepted and company funding is approved (October 23); the presentation
  is upcoming, not yet delivered.

## Local verification

October 23, 2026, this worktree, Java 25.0.2 and host Ollama qwen2.5:7b:

- `python3 scripts/workflow.py prepare`: extensions installed into isolated Maven
  cache; all three sample applications built; nine tests passed.
- Initial isolated build exposed a missing reactor dependency. The wrapper now
  explicitly includes `agentic/deployment`; no published or local snapshot fallback
  was used to satisfy it.
- `python3 scripts/workflow.py smoke`: real A2A summary/translation, MCP invocation
  and connected briefing passed. The actual MCP output fed the summary and the
  actual summary fed the translation.
- Governance: exact HTTP 400 removed-skill violation; rejected version absent;
  accepted content/version/globalId unchanged; full briefing reran successfully.
  Compatible publication created a new registration observed by another briefing.
- The runner stopped its processes and removed its Registry/container network.
- Upstream split-package and optional gRPC indexing warnings remain.
- Slides have not been visually reviewed in a browser; do that before presenting.
