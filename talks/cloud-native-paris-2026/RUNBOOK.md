# Connected workflow — rehearsal notes

For the full speaking script, setup checklist, demo commands and Q&A, use
[PRESENTATION_GUIDE.md](PRESENTATION_GUIDE.md). This file is the short runbook
and verification record.

## Timing (25 minutes; add discussion for a 30-minute slot)

1. **0–3:** Why not three configured URLs? Contrast one-team ownership with independent
   publishers. The small demo illustrates a design choice, not measured fleet scale.
2. **3–6:** Diagram: separate registration/discovery paths from direct execution calls.
   Introduce programmatic A2A publication, explicit MCP registration and fixed roles.
3. **6–11:** Submit one briefing. Inspect weather output in summarizer input, then the
   exact summary in translator input. Show the observed contract versions.
4. **11–16:** Show the minimal card diff, then run `python3 scripts/workflow.py governance`.
   Show the precise removed-skill rejection, unchanged registration and successful rerun,
   then the translator's version history in the console (http://localhost:8888).
5. **16–19:** Compatible publication, then `python3 scripts/workflow.py tools`: tools imported
   from the running weather server, a breaking tool change rejected, re-import unchanged.
6. **19–22:** Responsibility matrix and takeaways: discovery, sequencing, publication
   checks and deployment verification each have an owner.
7. **22–25:** Questions/buffer.

## Preflight

- Build the sample with `python3 scripts/workflow.py prepare`.
- Run the smoke check with the intended hardware/model and inspect the outputs.
- Start the interactive runner and open http://localhost:11020/ and http://localhost:8888/.
- Pull the images beforehand: the Registry and console images pinned in `scripts/workflow.py`
  (the `latest-snapshot` builds rehearsed on 24 November 2026) and `nginx:1.27-alpine`. To
  rehearse a newer snapshot, set `REGISTRY_IMAGE` and `REGISTRY_UI_IMAGE`, and pin it only if the
  whole runbook passes.
- Start `python3 scripts/present.py` and open http://127.0.0.1:8765/ for the live governance
  and tools buttons. Use `--mode secured` with the secured stack. The standalone HTML and
  printed commands are the fallback.
- After the demo, run `python3 scripts/stop.py` from a spare terminal. This stops the presenter
  and both demo modes without stopping host Ollama.
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
- Default mode: the demo is not authenticated, so no claim about securing agent or tool calls.
  Secured mode has its own claims and checklist: [SECURED.md](SECURED.md).

## Local verification

18 November 2026, Podman (as `docker`), host Ollama, Quarkus LangChain4j 1.15.0 sample
(released extensions), Apicurio Registry and console snapshots pinned in `scripts/workflow.py`
(`sha256:16624cdf…` and `sha256:46f422ce…`). `governance` and `tools` passed against the running
sample too:

- `python3 scripts/workflow.py prepare`: nine sample tests passed.
- `python3 scripts/workflow.py smoke`: both agents discovered; real A2A summary and
  translation; `getWeather` executed on the MCP server; connected briefing with the actual
  MCP output in the summary input and the actual summary in the translation input;
  governance passed (removed skill rejected, accepted registration unchanged, briefing reran;
  additive version accepted).
- `run`, then `governance` and `tools` from a second terminal, each twice: all passed. `tools`
  imported `getWeather` (`CREATED`, then `UNCHANGED`), rejected the breaking change with HTTP 400
  and two causes, and left the tool unchanged.
- Console (http://localhost:8888) shows the translator's versions and the imported tool.
- Ctrl+C stops the runner. Stopped any other way (for example from a script), the runner
  leaves its JVMs and containers running; stop them as described in the guide.

Slides were checked in headless Chrome on October 23 at 1920×1080, 1280×720, 1024×768 and
390×844 (animations, navigation, notes, print). After the November changes (slides 3, 6–11 and 13), all 13
slides were rechecked at the same four sizes: no overflow or clipped content, no JavaScript errors.

24 November 2026: Quarkus LangChain4j 1.15.1 was out (a patch release; diffing the two tags
confirms the `a2a-apicurio-registry` and `mcp-apicurio-registry` extensions are unchanged between
them — both already carried the input-key and republish-on-restart fixes since 1.15.0). Bumped
`scripts/workflow.py` and `scripts/secured.py` from the `1.15.0` tag to `1.15.1` anyway, to track
the latest release, and re-pinned the Registry/console snapshots, which was the actually necessary
fix: the 18 November digests had been garbage collected from quay.io, as floating
`latest-snapshot` tags are. Re-ran the full verification, Docker on macOS, host Ollama,
against `1.15.1` (released extensions) and the new snapshot pins:

- `python3 scripts/workflow.py prepare`: nine sample tests passed.
- `python3 scripts/workflow.py smoke`: both agents discovered; real A2A summary and translation;
  `getWeather` executed on the MCP server; connected briefing with the actual MCP output in the
  summary input and the actual summary in the translation input; governance passed (removed skill
  rejected, accepted registration unchanged, briefing reran; additive version accepted).
- `run`, then `governance` and `tools` from a second terminal, each twice: all passed, same
  results as 18 November (`tools` imported `getWeather` as `CREATED` then `UNCHANGED`, rejected
  the breaking change with HTTP 400 and two causes, left the tool unchanged).
- Console (http://localhost:8888) and the orchestrator (http://localhost:11020) both reachable.
- `docker compose ... down` left no containers or orphaned host JVMs.
