# Agent discovery and governance: talks and demo

Talk material about using [Apicurio Registry](https://www.apicur.io/registry/) to discover and
govern AI agents and tools, with the [A2A](https://a2a-protocol.org) and
[MCP](https://modelcontextprotocol.io) protocols and
[Quarkus LangChain4j](https://docs.quarkiverse.io/quarkus-langchain4j/dev/).

The demo code lives upstream, where it is tested and maintained. This repository adds what is
specific to talks: slides, presenter guides, runbooks and a runner.

| Demo | What it shows | Where |
|---|---|---|
| **Connected workflow** (used in the talks) | Quarkus agents publish their Agent Cards; an orchestrator discovers two A2A agents and an MCP server through the Registry and runs weather → summary → French translation; a compatibility rule rejects a breaking card change while the workflow keeps running | [Quarkus LangChain4j sample](https://github.com/quarkiverse/quarkus-langchain4j/tree/1.15.0/samples/agent-registry-workflow), run with `scripts/workflow.py` |
| **Every agent registry feature** | Agent artifact types and references, compatibility rules, discovery (agents, MCP tools, AI Catalog, ARD), the MCP Registry API, importing tools from a running MCP server, visibility per user with Keycloak, the Registry MCP server, with a step-by-step guide and screenshots | [Apicurio Registry `examples/agent-registry`](https://github.com/Apicurio/apicurio-registry/tree/main/examples/agent-registry) |

## Run the connected workflow

Requires Git, Java 17+, Docker (or Podman) with Compose v2, Python 3.9+ and
[Ollama](https://ollama.com/) running on the host with `qwen2.5:7b`.

```sh
ollama pull qwen2.5:7b
python3 scripts/workflow.py prepare     # fetch the Quarkus LangChain4j 1.15.0 sample, build, run its tests
python3 scripts/workflow.py run         # start it; open http://localhost:11020/
python3 scripts/workflow.py governance  # in a second terminal: the compatibility checks
```

`python3 scripts/workflow.py smoke` runs the whole flow with assertions, then stops. Ctrl+C stops
`run`, including its temporary Registry. The sample is fetched into `target/` (ignored).

## Talks

| Folder | Event | Status |
|---|---|---|
| [`talks/cloud-native-paris-2026`](talks/cloud-native-paris-2026) | Cloud Native AI Summit, Paris 2026 | Upcoming: slides, presenter guide, runbook |
| [`talks/apidays-amsterdam-2026`](talks/apidays-amsterdam-2026) | apidays Amsterdam 2026 | Delivered: slides; demo code at a tag |
| [`talks/cfp-package`](talks/cfp-package) | Future submissions | Abstract, claims and run of show |

Add a talk as a new folder under `talks/` (for example `talks/<event>-<year>`), reusing
`scripts/workflow.py` for the demo.

Design notes are in [`docs/adr`](docs/adr).

## License

Apache License 2.0. See [LICENSE](LICENSE).
