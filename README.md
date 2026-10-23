# Agent Sprawl: Connected Discovery and Contract Governance

This branch now uses the connected workflow in
[Quarkus LangChain4j #2932](https://github.com/quarkiverse/quarkus-langchain4j/pull/2932),
pinned to `682928b714574e1ca8f0405c4319b36cd244dc0e`.
The PR was open when checked on October 23, 2026; this is not a released upstream sample.

One request creates a multilingual briefing:

```text
Registry discovery → MCP weather → A2A summarizer → A2A translator
```

The application chooses required roles and sequence. Registry extensions resolve
endpoints; each step passes its actual result to the next. The dashboard displays
handoffs and the registry versions observed before execution.

## Prepare and run

Requirements: Git, Java (upstream verification used Java 25), Docker Compose v2,
Python 3.9+, and a running host Ollama with `qwen2.5:7b` installed. The pinned
sample declares Java 17+, but use Java 25 to match its verification environment.
Allow memory for the model and four JVMs. Ports: 8180, 11010, 11020, 11030, 11040.

```sh
ollama pull qwen2.5:7b
python3 scripts/workflow.py prepare
bash scripts/paris-2026-demo.sh
```

Open http://localhost:11020/. In a second terminal:

```sh
python3 scripts/workflow.py governance
```

The governance sequence runs the briefing, attempts to remove the translator's
skill, checks the precise Registry HTTP 400 compatibility violation, verifies that
the rejected version is absent and accepted content/version/globalId are unchanged,
then runs the briefing again. A compatible addition creates a new registered version
and is followed by another complete briefing.

Ctrl+C stops the runner's JVMs and ephemeral Registry. It leaves host Ollama and
other demo stacks alone. Stop the interactive run before running the smoke test:

```sh
python3 scripts/workflow.py test
python3 scripts/workflow.py smoke
```

`prepare` fetches the pinned source into ignored `target/registry-workflow-source`,
builds the extensions and tests the sample. It uses `target/workflow-m2` for Maven
artifacts so its checkout-built `999-SNAPSHOT` dependencies do not replace those
used by other projects. It refuses a modified or different-revision source checkout.
`test` runs the sample tests only; `smoke` exercises real Registry, MCP, A2A and Ollama
calls. `OLLAMA_URL` and `OLLAMA_MODEL` are supported by the upstream runner.

## Talk boundaries

- A2A agents publish through the extension's programmatic publisher. The runner
  explicitly registers the MCP tool; it is not automatic server-side ingestion.
- The connected briefing uses fixed roles and ordering, not autonomous LLM planning.
- Weather is fictional fixture data. Generated summaries/translations remain variable.
- Registry checks protect published metadata. They do not prevent deploying
  incompatible code or calling an endpoint directly.
- Observed registry versions are not deployment pins or a transactional snapshot.
- There is **no deprecation gate** in this sample. Deprecation is discussion material,
  not a demonstrated stop-routing feature.
- This is a loopback-only, unauthenticated demonstration, not production security.

## Presentation

Open `presentation.html`. It follows the connected workflow and publication checks
in a 25–30-minute slot. Speaker notes and rehearsal steps: `docs/paris-runbook.md`.
The 13-slide narrative explains when a registry helps, separates metadata and
execution paths visually, shows the rejected card change, and finishes with the
typed MCP wrapper lesson and a responsibility matrix.
Do builds/model downloads before the session. Record a backup before presenting.

Slides use a self-contained 16:9 layout with no external font or asset downloads.
Arrow keys / Page Up / Page Down navigate; N toggles speaker notes, F toggles
fullscreen, and R replays the current slide's animation. The architecture flow
has a replay button. Animations illustrate sequencing, not live calls or measured
latency. Reduced-motion preferences disable them; print output shows complete
static slides without controls or speaker notes. URLs such as `#slide-4` open a
particular slide directly.

## Repository layout

- `scripts/workflow.py`: pinned source checkout, build, tests and demo commands.
- `scripts/paris-2026-demo.sh`: starts the interactive demo.
- `presentation.html`: slides for this workflow.
- `docs/paris-runbook.md`: rehearsal plan and verification record.
- `target/` (ignored): upstream source, application builds and isolated Maven cache.

The actual Java services, dashboard, Compose file and tests live in the pinned
upstream sample fetched by `prepare`. This branch keeps one implementation path.

## Verification provenance

The pinned PR includes nine automated tests and an October 7 live verification record:
[VERIFICATION.md](https://github.com/carlesarnal/quarkus-langchain4j/blob/682928b714574e1ca8f0405c4319b36cd244dc0e/samples/agent-registry-workflow/VERIFICATION.md).
That is the upstream author's record, not proof of a fresh run of this wrapper.
See `docs/paris-runbook.md` for current local check results.

## License

Apache License 2.0 — see [LICENSE](LICENSE). The fetched upstream repository
retains its own license and notices.
