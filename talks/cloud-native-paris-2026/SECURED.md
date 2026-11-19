# Secured mode: every hop authenticated

The same workflow as the default mode, with authentication on every hop. Run it with
`scripts/secured.py`; the default mode (`scripts/workflow.py`) stays the fallback.

| Hop | Credentials |
|---|---|
| People to the Registry console | Login through Keycloak (alice: admin, bob: read-only) |
| Agents to the Registry (publish their cards) | Each agent's client credentials (role `sr-developer`) |
| Orchestrator to the Registry (discover agents and tools, read contracts) | Orchestrator's client credentials (role `sr-readonly`) |
| Orchestrator to the agents (A2A) | Orchestrator token; the agents require role `a2a-caller` |
| Orchestrator to the weather server (MCP) | Orchestrator token; the server requires role `mcp-caller` |
| Orchestrator fetching the agents' Agent Cards | Orchestrator token; the cards require role `a2a-caller` too |
| Registry importing tools from the weather server | The importer's token, sent to the MCP server only, never stored |

Only the health checks (`/q/*`) are public.

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
- **The weather server** from the 1.15.0 sample (Quarkus 3), because the MCP server extension does
  not support Quarkus 4 yet.
- **Apicurio Registry and console:** the same pinned snapshots as the default mode, plus Keycloak
  26.2 on http://localhost:8190 (realm `agents`, from [`secured/realm.json`](../../secured/realm.json)).

The demo realm's passwords and client secrets are public in this repository. Localhost only.

## Commands

```sh
python3 scripts/secured.py prepare     # fetch and install the snapshot, build the sample and the weather server
python3 scripts/secured.py smoke       # the whole flow plus the security checks, then stop
python3 scripts/secured.py run         # start it; open http://localhost:11020/ and http://localhost:8888/
python3 scripts/secured.py governance  # in a second terminal
python3 scripts/secured.py tools       # in a second terminal
```

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

## Showing it

- **Slides:** open `presentation.html?mode=secured`. It adds slide 5, "Every call carries an
  identity" (after the architecture, about one minute), and switches the slide 11 and closing texts
  and speaker notes to secured mode. Without `?mode=secured` you get the default deck.
- In the console, log in as **alice** (password `alice`): she can edit. Log in as **bob** (`bob`)
  in a private window: he can browse, but not change anything.
- In a terminal, show a refused call:
  ```sh
  curl -i -X POST http://localhost:11010/ -H 'Content-Type: application/json' -d '{}'   # 401
  ```
- The briefing, governance and tools steps are the same as in the default mode; every call
  behind them now carries a token.

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
- **"Are the integrations released?"** "The extensions are, in Quarkus LangChain4j 1.15.0. The
  authentication you saw is in review upstream; the demo builds it from that branch."

## One week before (25–28 November)

1. `git pull`, then `prepare` for both modes (`workflow.py` and `secured.py`).
2. On the presenting laptop: `smoke` for both modes. Then `run`, with `governance` and `tools`
   from a second terminal, twice. Log in to the console as alice and bob. Time the demo.
3. **Go/no-go for secured mode by 28 November:** go if it passed twice in a row on that laptop;
   otherwise present the default mode.
4. Make sure nothing gets downloaded on stage: the images (Registry, console, nginx, Keycloak), the
   Ollama model, and the Maven dependencies. Run `prepare` once more with Wi-Fi off.
5. Record a screen capture of a successful run of each mode, as the plan B.

## On the day

1. Start Podman and Ollama; send Ollama one request so the model is loaded.
2. No leftovers: `docker ps` shows no `langchain4j-agent-workflow` containers, and
   `pkill -f talk-sample; pkill -f secured-sample` is quiet.
3. `smoke` once in the mode you'll present. If it fails, switch modes or to the recording; don't
   debug on stage.
4. `run` in terminal A. Open http://localhost:11020/ and http://localhost:8888/ (logged in as
   alice in secured mode). Terminal B is ready for `governance` and `tools`.
5. Larger terminal font; notifications and sleep off.
6. Afterwards, Ctrl-C in terminal A stops everything.

## Local verification

19 November 2026, Podman (as `docker`), host Ollama, `demo/paris-auth` at `c169e04`, the pinned
Registry and console snapshots:

- `prepare`: the snapshot, the sample and the weather server built.
- `smoke`: all 21 checks passed (6 for the workflow, 15 for security). With access logs on, the
  agents' logs showed the orchestrator fetching their cards with its token (HTTP 200).
- `run`, then `governance` and `tools` twice: all passed, including the anonymous import refusal.
- The console redirected to the Keycloak login page for the `apicurio-console` client. Logging in
  through a browser as alice and bob is still to be checked by hand.
