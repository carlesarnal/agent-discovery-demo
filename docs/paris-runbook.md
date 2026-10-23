# Connected workflow — rehearsal notes

For the full speaking script, setup checklist, demo commands and Q&A, use
[PRESENTATION_GUIDE.md](../PRESENTATION_GUIDE.md). This file is the short runbook
and verification record.

## Timing (25 minutes; add discussion for a 30-minute slot)

1. **0–3:** Why not three configured URLs? Contrast one-team ownership with independent
   publishers. The small demo illustrates a design choice, not measured fleet scale.
2. **3–6:** Diagram: separate registration/discovery paths from direct execution calls.
   Introduce programmatic A2A publication, explicit MCP registration and fixed roles.
3. **6–11:** Submit one briefing. Inspect weather output in summarizer input, then the
   exact summary in translator input. Show the observed contract versions.
4. **11–16:** Show the minimal card diff, then run `python3 scripts/workflow.py governance`.
   Show the precise removed-skill rejection, unchanged registration and successful rerun.
5. **16–19:** Compatible publication, then the typed MCP wrapper integration lesson.
   Distinguish model-driven /workflow/weather from the fixed-sequence briefing.
6. **19–22:** Responsibility matrix and takeaways: discovery, sequencing, publication
   checks and deployment verification each have an owner.
7. **22–25:** Questions/buffer.

## Preflight

- Build the pinned source with `python3 scripts/workflow.py prepare`.
- Run the smoke check with the intended hardware/model and inspect the outputs.
- Start the interactive runner and open http://localhost:11020/.
- Open slides separately; no embedded terminal server is required.
- Slides: arrows navigate; N toggles notes; F toggles fullscreen; R replays
  animation. Slide 4 reveals publication, discovery and invocation in sequence.
  These are illustrative animations, not live telemetry. Keep the real dashboard
  in a separate tab for the actual workflow execution.
- Record a backup of the same revision. Do not describe a recording as live execution.
- Rehearse to 22 minutes. If behind at minute 16, explain the compatible-addition
  result already observed in smoke tests instead of waiting for another generation.
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
- Slides checked in headless Chrome at 1920×1080, 1280×720 and 1024×768:
  all 13 slides fit above navigation with no horizontal overflow. Title and
  demo-slide screenshots inspected. Keyboard/button navigation, notes toggle,
  print visibility and absence of JavaScript errors checked.
- Removed the unused agents, legacy Compose files, schemas, ADRs and numbered
  scripts. The pinned upstream sample is the sole execution path.

Visual redesign checked October 23:
- All 13 redesigned slides inspected as Chrome screenshots.
- Fixed 1600×900 canvas scales into desktop/projector and narrow viewports;
  checked at 1920×1080, 1280×720, 1024×768 and 390×844.
- Tested sequential flow animations, replay, reduced motion, hash navigation,
  keyboard/buttons, notes visibility and print rendering; no JavaScript errors.
- Print/PDF rendering disables animations so the full flow is visible.
- No new runtime behavior was introduced by the slide redesign; earlier sample
  verification remains the evidence for the actual demo.
