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
- Pull the images beforehand: `quay.io/apicurio/apicurio-registry:latest-snapshot`,
  `quay.io/apicurio/apicurio-registry-ui:latest-snapshot`, `nginx:1.27-alpine`. Pin the
  Registry image you rehearsed with (`REGISTRY_IMAGE`) so a newer snapshot can't surprise you.
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
- The demo is not authenticated: no claim about securing agent or tool calls.

## Local verification

November 2026, Java 25.0.2, Podman (as `docker`), host Ollama qwen2.5:7b, Quarkus LangChain4j
1.15.0 sample (released extensions), Apicurio Registry `latest-snapshot`:

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
