# Paris presentation guide — Secured mode

**Use this guide for the 15-slide Keycloak demo.** The 14-slide release-based demo has its own
[default guide](PRESENTATION_GUIDE.md) with the full 130 wpm verbatim script for the shared
slides. Keep only this guide open on stage; slide numbers below are the numbers you actually see
in the secured deck.

## Before the event

From the repository root, with the container engine and host Ollama running:

```sh
ollama pull qwen2.5:7b
python3 scripts/secured.py prepare
python3 scripts/secured.py smoke
```

Rehearse on the presentation laptop with the **same commit, images and model** you will use on
stage. Use secured mode only if smoke passes twice and you have rehearsed all three live slide
controls (slides 8, 10, and 12), the 401 example, and the alice/bob console logins. Record a
successful rehearsal for offline playback. If those checks fail before the talk, stop this stack
and switch to the default guide and deck; do not try to change modes on stage.

## Before you go on stage

1. **Terminal A:** run `python3 scripts/secured.py run` and wait for `Ready:`.
2. **Terminal B:** run `python3 scripts/present.py --mode secured`. Open
   <http://127.0.0.1:8765/>. The toolbar must say **Secured demo**, and the counter must say
   **1 / 15**. Glance at slides **8, 10, and 12** to check that **Show live briefing**,
   **Run governance**, and **Run tools** appear, **without clicking governance or tools**.
   Press **Home** to return to slide 1, **F** for fullscreen; keep notes off the projected screen.
3. Open the [translator card](http://localhost:8888/explore/default/translator) in a second tab
   and sign in as `alice` (`alice`); preload a private window signed in as `bob` (`bob`). Keep
   the backup recording ready, `http://localhost:11020/` only as a fallback, and the request below
   typed in a spare terminal, not yet run.
4. Warm up Ollama with one briefing on slide 8 (Amsterdam and the visitor context are already
   pre-filled), then click **Back to slide** and press **Home**. Leave governance and tools
   untouched on the fresh stack so their first changes are visible live.

```sh
curl -sS -o /dev/null -w 'Anonymous A2A call: HTTP %{http_code}\n' \
  -X POST http://localhost:11010/ -H 'Content-Type: application/json' -d '{}'
```

## On-stage cues — 25 minutes

> “The registry tells us where the contracts live and checks what can be published. The
> application still owns the workflow, and each service decides which callers to accept.”

Because Slide 6 adds ~2.5 minutes for the live 401 and console checks, keep Slide 5 (code) to ~1
minute and treat the Registry console views on slides 10 and 12 as optional. Use **→** (not
Space) to advance after clicking live demo buttons.

| Time | Slide | What to say or show |
|---|---|---|
| 0–4:30 | **1–3 · Why a registry?** | Three URLs are fine for one team. Connect to OpenAPI/Kafka Schema Registry; independent publishers need locate, review, and check. |
| 4:30–7:30 | **4–5 · Architecture & code** | Replay Slide 4 with **R** (orange = metadata, teal = direct calls). On Slide 5, point out zero hardcoded endpoints — only `REGISTRY_URL` plus OIDC client config. |
| 7:30–10:00 | **6 · Identity on every hop** | Run the anonymous `curl` (**HTTP 401**), explain 401 vs. 403, then show `alice` (can edit) vs. `bob` in the private window (read-only). Mention Agent Cards require `a2a-caller` too. *(Verbatim text below.)* |
| 10:00–13:30 | **7–8 · Handoffs and briefing** | Say the weather is fictional. On Slide 8, click **Show live briefing** → **Run briefing** (already pre-filled). Walk through steps 01–03 and the observed versions disclosure, then press **→**. |
| 13:30–17:30 | **9–10 · Breaking card change** | Explain the removed `translator` skill on Slide 9. On Slide 10, click **Run governance** once; narrate the phases while the timer ticks, then point to the coral `REJECTED` block and both green `PASS` lines. |
| 17:30–18:30 | **11 · Compatible addition** | The same command already proved the additive version. Updating metadata does not deploy new code into the running translator. Don't run governance again. |
| 18:30–20:30 | **12 · Tool governance** | Click **Run tools** once. Show anonymous import refused (401), real `getWeather` import, the required `units` change rejected, and re-import unchanged. |
| 20:30–23:00 | **13–14 · Responsibility and takeaways** | Separate discovery, sequencing, publication checks, and deployment verification. Roles on loopback with demo secrets are not tenant isolation or production credential management. |
| 23:00–25:00 | **15 · Links, QR & Q&A** | Point to the QR code and links. State clearly that the authentication client changes are built from a pinned branch in review, not the 1.15.1 release. |

### Verbatim script for Slide 6 — Every call carries an identity (~230 words · ~2.5 min with live checks)

> Now, in a real multi-team organization, the very next question after seeing that architecture is: who is allowed to publish a card, who can discover it, and who can call these endpoints?
>
> In this version of the demo, nothing is anonymous—except the health endpoints. Every human and every service gets its identity from one Keycloak realm. People log into the Registry console: `alice` has admin rights, `bob` is read-only. Each A2A agent publishes its own card using client credentials with a write role. The orchestrator discovers endpoints using a read-only identity. And when the orchestrator calls the A2A agents or the Weather MCP server, it sends a bearer token, and each service checks for the exact role it expects: `a2a-caller` or `mcp-caller`. Even the A2A Agent Cards themselves require `a2a-caller` here—the spec allows public cards too, and here the client sends the same credentials on the card fetch.
>
> Let's check two things quickly. First, in this terminal, an anonymous POST directly to the summarizer: *(run the pre-typed `curl`)* — **HTTP 401**. No token, refused at the door. A valid token without the `a2a-caller` role gets **403**. Second, in the Registry console: as `alice` I can edit artifacts; in this private window as `bob`, I can browse everything, and I cannot modify anything.
>
> One honest boundary: the Registry side of this is already released, while the Quarkus LangChain4j client authentication changes are still in review upstream—this demo builds them from a pinned branch. Now every call behind the briefing we're about to run carries a token.

**If something fails:** use `python3 scripts/secured.py governance` or `python3 scripts/secured.py tools`
in a spare terminal if the slide buttons are missing and you have not already launched the command.
The standalone fallback is `presentation.html?mode=secured` with those same commands. If inference
stalls or authentication fails on stage, say you're switching to a recording of the same revision.
Don't treat an unexpected 401 or timeout as a compatibility rejection. Cut the optional console
views before cutting the takeaways.

**After the talk:** run `python3 scripts/stop.py` from a spare terminal to stop the stack and
presenter. Host Ollama stays running.

## Technical reference (rehearsal and Q&A)

| Hop | Credentials |
|---|---|
| People to the Registry console | Login through Keycloak (alice: admin, bob: read-only) |
| Agents to the Registry (publish their cards) | Each agent's client credentials (role `sr-developer`) |
| Orchestrator to the Registry (discover agents and tools, read contracts) | Orchestrator's client credentials (role `sr-readonly`) |
| Orchestrator to the agents (A2A) | Orchestrator token; the agents require role `a2a-caller` |
| Orchestrator to the weather server (MCP) | Orchestrator token; the server requires role `mcp-caller` |
| Orchestrator fetching the agents' Agent Cards | Orchestrator token; the cards require role `a2a-caller` too |
| Registry importing tools from the weather server | The importer's token, sent to the MCP server only, never stored |

The backend health checks (`/q/*`) are public; the local presenter and briefing UI are not a
public-facing authenticated application.

## What it runs on

Unreleased client code, on purpose:

- **Quarkus LangChain4j** from the branch `demo/paris-auth` of carlesarnal/quarkus-langchain4j,
  pinned to one commit: `main` plus
  [#3145](https://github.com/quarkiverse/quarkus-langchain4j/pull/3145) (credentials for the
  Registry) and [#3157](https://github.com/quarkiverse/quarkus-langchain4j/pull/3157)
  (credentials for A2A agents), both in review, plus the change for
  [#3205](https://github.com/quarkiverse/quarkus-langchain4j/issues/3205) (the same credentials on
  the Agent Card request), proposed on top of #3157. Built locally as `999-SNAPSHOT`, with the 2.x
  sample (Quarkus 4).
- **The weather server** from the 1.15.1 sample (Quarkus 3), because the MCP server extension does
  not support Quarkus 4 yet.
- **Apicurio Registry and console:** the same `latest-snapshot` images as the default mode, plus Keycloak
  26.2 on http://localhost:8190 (realm `agents`, from [`secured/realm.json`](../../secured/realm.json)).

The demo realm's passwords and client secrets are public in this repository. Localhost only.

## Smoke checks and troubleshooting

`smoke` checks, on top of the default flow:

- the agents refuse A2A calls and Agent Card requests without a token (401) and with a token
  without `a2a-caller` (403), and serve their cards to the orchestrator;
- the weather server refuses MCP calls without a token (401) and tokens without `mcp-caller` (403);
- the Registry refuses anonymous reads (401), lets a read-only identity read, and refuses its
  writes (403).

`tools` also checks that the Registry refuses an anonymous tool import.

`smoke` and `run` check the container VM's clock first. If the laptop slept, the Podman VM clock
can fall behind, and Keycloak then issues tokens that are already expired. The fix:

```sh
podman machine ssh "sudo date -u -s @$(date +%s)"
```

If the console fails to redirect, check Keycloak at <http://localhost:8190/>. Application logs
are in `target/secured-sample/target/run/`. The [workflow Q&A](PRESENTATION_GUIDE.md#6-likely-questions-and-short-answers)
is useful for rehearsal; use the answers below for security and release status.

## Claims and answers that change

- Say it runs on upstream changes in review, built from a pinned branch, not on a release.
- Agent Cards are protected here; the A2A spec also allows public ones. The client sends the same
  credentials on the card request as on the calls.
- Don't claim tenant boundaries, key rotation or production credential management. The secrets
  are demo values in a file.
- **"Is this secure for multiple teams?"** "Every hop in this demo is authenticated, and each
  service needs a role for the calls it receives. A shared deployment still needs tenant
  boundaries, credential management and operational policies; I haven't demonstrated those."
- **"Who can publish or change a card?"** "Only identities with a write role: the agents publish
  with their own credentials. Here bob can read but not write."
- **"Are the integrations released?"** "The extensions are, in Quarkus LangChain4j 1.15.1. The
  authentication you saw is in review upstream; the demo builds it from that branch."

## Local verification

19 November 2026, Podman (as `docker`), host Ollama, `demo/paris-auth` at `c169e04`, the pinned
Registry and console snapshots:

- `prepare`: the snapshot, the sample and the weather server built.
- `smoke`: all 21 checks passed (6 for the workflow, 15 for security). With access logs on, the
  agents' logs showed the orchestrator fetching their cards with its token (HTTP 200).
- `run`, then `governance` and `tools` twice: all passed, including the anonymous import refusal.
- The console redirected to the Keycloak login page for the `apicurio-console` client. Logging in
  through a browser as alice and bob is still to be checked by hand.
- **Note (26 November 2026):** Secured mode has not yet been re-run end-to-end since the 24–26
  November updates (1.15.1 weather server, refreshed Registry/console snapshot digests, and the
  simplified briefing dashboard). Re-run `secured.py prepare` + `secured.py smoke` + manual
  alice/bob browser login before choosing secured mode on stage.
