# Paris — stage cue sheet and rehearsal record

Choose one presenter guide before the talk: [Default · 14 slides](PRESENTATION_GUIDE.md) or
[Secured · 15 slides](SECURED.md). This sheet is for timing and recovery, not setup.

## Stage cues (25 minutes at 130 wpm; ~21 minutes at faster stage pace)

| Time | Cue | Default | Secured |
|---|---|---:|---:|
| 0–5 | Why a registry? Connect to OpenAPI/Kafka schemas; three jobs (locate, review, check). | 1–3 | 1–3 |
| 5–9:15 | Architecture (replay with **R**) + real sample code (one Registry URL, zero hardcoded endpoints). | 4–5 | 4–5 |
| — | *(Secured only · ~2.5 min)* Show anonymous 401 `curl` and alice vs. bob in the console. | — | 6 |
| 9:15–13:30 | Handoffs → click **Show live briefing** → **Run briefing** → walk steps 01–03 & versions, then **→**. | 6–7 | 7–8 |
| 13:30–18:00 | Show removed skill → click **Run governance** (narrate while timer ticks) → point to coral `REJECTED` + green `PASS` lines + optional console tab. | 8–9 | 9–10 |
| 18:00–21:30 | Explain compatible addition, then click **Run tools** → show import, coral `REJECTED` (2 causes), and unchanged re-import. | 10–11 | 11–12 |
| 21:30–24:15 | Keep the responsibility matrix and three takeaways even if time is short. | 12–13 | 13–14 |
| 24:15–25:00 | Share the QR code and links; take questions. | 14 | 15 |

## Preflight

- Follow the chosen guide's build and smoke instructions. Rehearse all three live slide controls (briefing, governance, tools).
- Start the interactive runner and open http://127.0.0.1:8765/ and http://localhost:8888/explore/default/translator.
- Pull the images beforehand so Compose reuses your rehearsed local cache on stage:
  `quay.io/apicurio/apicurio-registry:latest-snapshot`,
  `quay.io/apicurio/apicurio-registry-ui:latest-snapshot`, and `nginx:1.27-alpine`. Do not re-pull
  after your final rehearsal (or override `REGISTRY_IMAGE` / `REGISTRY_UI_IMAGE` with your local
  image ID/digest).
- Match the toolbar mode (**Default demo** or **Secured demo**) and slide count (**1 / 14** or
  **1 / 15**) to the chosen guide. The standalone HTML and its printed commands are the fallback;
  share the GitHub repo (or slide 14/15 QR code), not the localhost URL.
- After the demo, run `python3 scripts/stop.py` from a spare terminal. This stops the presenter
  and both demo modes without stopping host Ollama.
- Slides: **→ / ←** navigate (prefer **→** over Space after clicking live buttons); **N** toggles
  notes; **F** toggles fullscreen; **R** replays animations. Slide 4 reveals publication, discovery
  and invocation in sequence—illustrative animations, not live telemetry.
- Record a backup of the same revision. Do not describe a recording as live execution.
- If behind at minute 16, skip optional console views. If live execution stalls, name the
  switch to the recording. Don't relaunch a running command or treat a timeout as a rejection.
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

27 November 2026: Switched `scripts/workflow.py` defaults to `:latest-snapshot` (the 24 November
digests had already been garbage collected on quay.io), added the sample code slide, offline QR
code, styled in-slide briefing, formatted governance/tools rejections, and the 130 wpm verbatim
rehearsal script. Re-verified default mode end-to-end on macOS with fresh `:latest-snapshot`
images, host Ollama (`qwen2.5:7b`), `scripts/workflow.py`, and `scripts/present.py`:

- `python3 scripts/workflow.py prepare`: rebuilt `target/talk-sample` with the simplified
  briefing dashboard; all nine sample tests passed.
- Live stack (`workflow.py run` + `present.py`): simplified dashboard on `:11020` and console on
  `:8888` reachable; all three presenter endpoints passed live (`briefing` in 10.4s, `governance`
  in 24.9s, `tools` in 0.6s); `python3 scripts/stop.py` stopped all processes and containers
  cleanly.
- Headless Chrome checked all 14 default / 15 secured slides at 1920×1080, 1280×720, and 1024×768
  (zero overflow, formatted rejection blocks, briefing panel, pointer blur); Slide 14 QR code
  screenshot verified with macOS Vision to `https://github.com/carlesarnal/agent-discovery-demo`.
