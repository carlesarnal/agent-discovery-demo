# Paris presentation guide — Default mode

**Use this guide for the 14-slide, unauthenticated demo.** Presenting the 15-slide Keycloak
variant? Open the [secured presentation guide](SECURED.md) instead. Use only one guide on stage.

**Talk:** Agent Sprawl Is the New Microservice Sprawl: Discovery and Governance for Multi-Agent AI

**Speaker:** Carles Arnal — Principal Software Engineer, IBM

**Event:** December 2–3, 2026, CNIT Forest, Paris

**Slot:** 25 minutes (confirm exact date, time, room, and whether Q&A is inside the 25 minutes)

**Deck:** [presentation.html](presentation.html), 14 slides. The live deck opens at
<http://127.0.0.1:8765/> and its toolbar should say **Default demo**.

**Demo:** the agent registry workflow sample from the Quarkus LangChain4j 1.15.1 release

**Pacing note:** The verbatim rehearsal script in Section 3 is sized for **~25 minutes at 130 words per minute** (~22 minutes of speech plus ~3 minutes of live demo waits, clicks, and console inspection). If you naturally speak faster on stage (~150–160 wpm), delivering this full script lands around **20–21 minutes**, leaving a clean **4–5 minutes for Q&A** inside a 25-minute slot. Treat it as a rehearsal script you can say in your own cadence, not lines to memorize robotically.

## The one sentence to remember

> “The registry tells us where the contracts live and checks what can be published. The application still owns the workflow, and tests still have to verify what the running code does.”

The demonstration is deliberately small: fictional weather → summary → French translation. Its purpose is to make boundaries visible, not demonstrate autonomous planning, a large production fleet or model quality.

---

## 1. Prepare before conference day

Run commands from the **repository root**, on `main`.

### Check prerequisites

```bash
git log -1 --oneline
java -version
python3 --version
docker info
docker compose version
ollama list
```

- Note the commit you rehearse with, and present from the same one.
- Java 17 or newer. The recorded verification used Java 25.
- Python 3.9+, Docker Compose v2 and Git are required.
- Run Ollama on the host. On macOS this allows native acceleration.
- Allow enough memory for the 7B model and four JVM processes. Close unrelated heavy workloads.
- `jq` is useful for the optional inspection commands in this guide, but isn't required by the runner.

### Download and build while you have reliable internet

If Ollama is not running, start the desktop app or run `ollama serve` in a separate terminal. Do not start a second server if one already uses port 11434.

```bash
ollama pull qwen2.5:7b
python3 scripts/workflow.py prepare
```

`prepare` fetches the Quarkus LangChain4j `1.15.1` tag, builds the talk's copy against the released extensions and runs its nine automated tests. The talk's copy uses a shorter briefing dashboard; the upstream source stays untouched:

```text
target/talk-sample/
```

Do not change versions immediately before presenting.

### Run the live smoke test

Stop any running demo first (`python3 scripts/stop.py`), then run:

```bash
python3 scripts/workflow.py smoke
```

Expected checks:

- Both agents discovered through `ApicurioAgentsRegistry`.
- Real A2A summary and translation return text.
- MCP fixture weather is returned and the server log confirms tool invocation.
- Actual MCP output feeds the summarizer; the actual summary feeds the translator.
- Removing the translator skill returns the expected compatibility error.
- The rejected version is absent; accepted content/version/globalId remain unchanged.
- The connected briefing works after rejection and after a compatible addition.
- The runner removes its processes and Registry container/network on completion.

The smoke test doesn't cover the tools step (slide 11). Rehearse it with `run` and `present.py`, using all three live slide controls (slides 7, 9, and 11).

These checks validate invocation, handoffs and publication behavior. Read the generated text yourself: a passing smoke run does not prove translation accuracy or factual correctness in general.

### Record a backup

Record the in-slide briefing (slide 7), the governance and tools runs (slides 9 and 11), and the console views on the same sample release, Registry image and model. Save the video locally and verify playback without network access. Record actual execution; don't fabricate successful output.

The governance command makes **three complete briefings**: baseline, after rejection and after compatible publication. Time it on the presentation laptop; correctness tests do not establish its stage duration.

---

## 2. Venue setup — 30 minutes before

### Terminal A: keep the demo running

```bash
python3 scripts/workflow.py run
```

Wait for:

```text
Ready: http://localhost:11020/workflow/agents
```

The runner starts an ephemeral Registry (`latest-snapshot`, for MCP tool import until 3.4 ships), the Registry console and a small proxy in Docker, and four local Java processes. It registers the weather tool and installs the translator's BACKWARD compatibility rule. **Leave Terminal A running.** Afterward, run `python3 scripts/stop.py` in a spare terminal to stop everything cleanly.

### Terminal B: readiness checks and presenter server

```bash
curl -fsS http://localhost:8180/apis/registry/v3/system/info
curl -fsS http://localhost:11020/workflow/agents | python3 -m json.tool
```

You should see the two roles `summarizer` and `translator`. Descriptions may vary with future revisions; the current sample expects those role names.

| Component | Address |
|---|---|
| Live slides | http://127.0.0.1:8765/ |
| Briefing dashboard (fallback) | http://localhost:11020/ |
| Orchestrator API | http://localhost:11020/workflow |
| Registry API | http://localhost:8180/apis/registry/v3 |
| Registry console | http://localhost:8888/ |
| Summarizer | http://localhost:11010 |
| Translator | http://localhost:11030 |
| Weather MCP endpoint | http://localhost:11040/mcp |
| Host Ollama | http://localhost:11434 |

The slide runner needs no ttyd or Kubernetes port-forwarding. The tools step needs Docker Desktop or Podman on macOS: they route `host.docker.internal` to the laptop's localhost, where the weather server listens.

### Open the live deck

In Terminal B, start the presenter server and leave it running:

```bash
python3 scripts/present.py
```

Open these two tabs in the browser:

```bash
open http://127.0.0.1:8765/
open http://localhost:8888/explore/default/translator
```

All **three** live demo steps run directly inside the deck:
- **Slide 7:** embedded briefing panel (Amsterdam and the visitor-context sentence are already pre-filled).
- **Slide 9:** **Run governance** streams formatted output directly into the slide.
- **Slide 11:** **Run tools** streams formatted output directly into the slide.

Before the audience arrives:
1. Confirm the toolbar says **Default demo** and the counter says **1 / 14**.
2. Glance at slides 7, 9, and 11 to verify **Show live briefing**, **Run governance**, and **Run tools** are visible — **do not click governance or tools**.
3. Warm up the Ollama model by running **one briefing** on slide 7 (or via `curl` in a spare terminal), then click **Back to slide** and press **Home** to return to slide 1.
4. Keep the Registry console tab (`http://localhost:8888/explore/default/translator`) and your offline backup recording ready. Keep `http://localhost:11020/` only as a fallback link (already on slide 7).

If the presenter server is unavailable, open `presentation.html` directly and run the printed commands in a spare terminal. For attendees, share the GitHub repo (or QR code on slide 14), not the localhost URL.

### Presentation controls

- **← / →**, Page Up / Page Down: navigate (prefer **→** over Space after clicking live buttons).
- **F:** fullscreen.
- **N:** notes overlay; **Escape:** close notes.
- **R:** replay animations; slide 4 also has a replay button.
- **Home / End:** first/last slide.
- `#slide-8` in the URL jumps directly to slide 8.

The notes overlay appears on the same screen as the slides. Don't open it on the projected display unless you intend the audience to see it. Keep this guide on a separate, non-mirrored display or print it.

Animations illustrate order, **not real traffic or elapsed time**. Reduced-motion settings disable them; printing gives complete static slides.

---

## 3. Slide-by-slide verbatim rehearsal script (130 wpm · 25 min)

### Slide 1 — Agent sprawl is the new microservice sprawl
**0:00–1:15 · ~180 words · Introduce yourself, the thesis, and why the demo is intentionally small.**

> Good morning everyone. I'm Carles Arnal. I'm a Principal Software Engineer at IBM, I'm one of the core maintainers of Apicurio Registry, and I contribute to Quarkus.
>
> Today I want to talk about a problem that is starting to look very familiar to anyone who lived through the early years of microservices: agent sprawl. Teams are spinning up specialist AI agents, exposing MCP tools, wiring them together in code, and changing their capabilities independently. And very quickly, the same operational questions we solved years ago for REST APIs and event streams come right back. Where does this service live? What does it promise it can do? And what happens when someone changes that contract before every caller is ready?
>
> Everything I'm going to show you today is live, running locally on this laptop, and deliberately small: one MCP tool and two A2A agents. That's on purpose. If we can't be clear about the boundaries with three services, scaling it to thirty won't make the architecture clearer. Let's start with the first honest question: do we even need a registry?

---

### Slide 2 — Three URLs are fine. Until ownership changes.
**1:15–3:15 · ~260 words · Connect the title to OpenAPI, AsyncAPI, and Kafka Schema Registry.**

> Before I sell you any new infrastructure, I want to give you permission to keep simple systems simple.
>
> Look at the left side of this slide. If one team owns an orchestrator, a couple of agents, and a tool server, and releases them together in the same pipeline, putting three URLs in an `application.properties` file may be completely fine. Adding a registry adds another service to operate, monitor, and secure. If you don't have an ownership boundary yet, static configuration is often the right engineering choice.
>
> The inflection point is the right side of the slide, and it's the exact same inflection point we hit ten years ago with microservices and event-driven systems. Back then, three REST URLs in config were fine too—until ten different teams owned those services. We didn't solve microservice sprawl by putting more URLs in YAML. We standardized how services describe their contracts with OpenAPI, AsyncAPI, Avro, and Protobuf, and we put those contracts in a registry—a schema registry or an API registry—so producers and consumers could evolve without breaking each other on every deploy.
>
> Multi-agent AI is hitting that exact same moment. Now the contract isn't just a REST endpoint or a Kafka topic schema. It's an A2A Agent Card declaring an agent's skills and endpoint, or an MCP tool definition declaring a tool's name and JSON input schema. Once different teams publish those agents and tools on different schedules, callers need a shared place to find endpoints, inspect what changed between versions, and reject a breaking publication before anyone relies on it.

---

### Slide 3 — A shared contract. Three useful jobs.
**3:15–5:00 · ~230 words · Separate discovery and governance from orchestration.**

> When people hear "agent registry", they sometimes picture a magical control plane that plans workflows, picks the smartest agent automatically, and proxies every prompt. I want to be very clear about what we are actually asking the registry to do here. In this design, Apicurio Registry has three concrete jobs.
>
> Job one is **locate**: agents and tool servers publish structured definitions—Agent Cards for A2A, tool and server definitions for MCP—and callers resolve their network endpoints at runtime instead of hardcoding hosts and ports.
>
> Job two is **review**: every publication creates an immutable version in the registry. When a capability changes, you have an auditable history of what was declared before and what was declared after.
>
> Job three is **check**: the registry runs compatibility rules when a new version is submitted. For an Agent Card, a `BACKWARD` compatibility rule checks that you didn't silently drop a skill callers already depend on. For an MCP tool, the same rule engine checks the JSON Schema so you don't suddenly add a required parameter that breaks every existing caller.
>
> Look at the sentence at the bottom of the slide: **the application still owns the roles and the execution order**. Discovery is not orchestration. The registry tells us where contracts live and whether a proposed version is compatible. Our application code still decides which roles it needs and in what order to call them. Let's put that on a diagram.

---

### Slide 4 — Discover through the registry. Call the services directly.
**5:00–7:15 · ~275 words · Press R if needed to narrate the orange vs. teal paths.**

> *(Let the diagram animate once, or press **R** as you begin explaining the paths.)*
>
> Here is the whole architecture running on my laptop right now, and there are two colors to pay attention to: orange and teal.
>
> The **orange paths** are the control plane—publication and discovery. On the right, we have three backend services: a Weather server exposing a `getWeather` tool over MCP, a Summarizer agent over A2A, and a Translator agent over A2A. Both text agents publish their Agent Cards into Apicurio Registry at startup using the Quarkus LangChain4j A2A extension. For the briefing workflow, our demo runner also registers the Weather MCP tool metadata in Apicurio Registry, and later we'll see Registry 3.4 import tools directly from the running MCP server itself.
>
> On the left sits the Orchestrator, a Quarkus application using the two Quarkus LangChain4j Apicurio Registry extensions—one for A2A and one for MCP. When a request comes in, the orchestrator queries the registry to resolve the current endpoints for those roles.
>
> Now look at the **teal paths**: invocation. Once the orchestrator knows the endpoints, it calls the Weather MCP server directly, takes the forecast, calls the Summarizer agent directly over A2A JSON-RPC, takes that summary, and calls the Translator agent directly over A2A JSON-RPC. Apicurio Registry is **never** in the data path and never proxies a prompt or a tool call. Just like a Kafka Schema Registry, it governs the contract metadata, while runtime traffic flows directly between services.
>
> Notice also that the Summarizer never calls the Translator. The orchestrator passes the actual output of step one into step two, and the output of step two into step three. Let's look at the actual Java code that wires this up.

---

### Slide 5 — One registry URL. Zero hardcoded endpoints.
**7:15–9:15 · ~260 words · Walk left to right through the real 1.15.1 sample code.**

> I promised this isn't slideware, so here are the actual excerpts from the Quarkus LangChain4j 1.15.1 sample running behind this presentation.
>
> Start on the **left card**, inside the Orchestrator. We inject two CDI beans provided by the extensions: `ApicurioAgentsRegistry`, which implements LangChain4j's `AgentsRegistry` SPI for A2A discovery, and `ApicurioRegistryMcpTools`, which provides discovery and invocation over Streamable HTTP for MCP servers. Look at what is missing from this code: there is not a single `localhost`, port number, or agent URL anywhere in the orchestrator. To find our agents, we call `registry.allAgents()` and look up the two roles our workflow expects: `"summarizer"` and `"translator"`. For the weather tool, our typed wrapper `DiscoveredWeather` calls `searchMcpServers("weather")`, connects to the registered `weather-tools/weather` artifact, and invokes `getWeather`. Notice why we wrapped that in a typed Java method: role selection, step ordering, and JSON argument encoding stay predictable application code, while only the network location comes from the registry.
>
> Now look at the **right card**. In `application.properties`, the orchestrator only needs to know one address: the Apicurio Registry URL, configured for the A2A extension and the MCP extension. Below that is `TextAgent.java`. Both the summarizer and translator are actually the same Quarkus binary started twice with different profiles. On startup, each instance uses `A2AAgentCardPublisher` from the extension to publish its own Agent Card—name, description, endpoint URL, and skill—into Apicurio Registry.
>
> That's the whole integration surface. Now let's talk about what counts as proof before we run it live.

---

### Slide 6 — Follow the data. Not just the final answer.
**9:15–10:45 · ~195 words · Define what proves the handoffs really happened.**

> With LLM demos, it is dangerously easy to fool yourself—and an audience. If I send "give me a weather briefing for Amsterdam in French" and a nice French paragraph comes back on screen, did the orchestrator actually call the MCP server? Did the summarizer actually read that tool output? Did the translator translate the summarizer's output, or did one model just make up plausible French weather from its training data?
>
> That's why we made two deliberate choices in this sample. First, the weather data is a deterministic fixture: for Amsterdam, the MCP tool always returns **16 degrees Celsius, light rain**, explicitly labeled as fictional sample data. That way we can tell immediately whether the real tool output made it downstream.
>
> Second, the orchestrator returns every intermediate step, not just the final string. In a second, we're going to check three specific things on screen: first, that the exact fixture weather string shows up inside the **input** to the Summarizer; second, that the exact summary returned by the Summarizer shows up verbatim inside the **input** to the Translator; and third, which registered contract versions the orchestrator observed in Apicurio Registry before making those calls. Let's run one live.

---

### Slide 7 — One briefing. Every handoff visible.
**10:45–13:30 · ~295 words + ~30s live execution · Demo 1 of 3 inside the slide.**

*Actions:*
1. Click **Show live briefing** (Amsterdam and the visitor-context sentence are already pre-filled).
2. Click **Run briefing** once. *(Say the "while running" block while Ollama completes both A2A calls.)*
3. Once complete, click **01 · MCP weather**, **02 · A2A summarize**, **03 · A2A translate**, and expand **Registry versions observed before the calls**.
4. When done, press **→** (or click **→** in the toolbar) to move to Slide 8.

*While clicking **Show live briefing** → **Run briefing** (~10–20s wait):*

> Let's open the live briefing panel right here inside the deck. Amsterdam and our visitor context are already filled in: "Visitors will walk through the city and meet colleagues to discuss open standards." I'll click **Run briefing** once.
>
> While those calls execute locally on Ollama, remember what is happening under the hood. The orchestrator is reading the current contract metadata from Apicurio Registry, resolving the MCP weather server and connecting to it over Streamable HTTP, then invoking the Summarizer and Translator over A2A JSON-RPC using the endpoints it discovered from their Agent Cards.

*Once results appear — click through steps 01, 02, 03, then expand the versions disclosure:*

> Alright, it finished. Let's trace the data step by step instead of just admiring the French.
>
> On **step 01, MCP weather**, the input is `"Amsterdam"`, and the output from the `weather-tools/weather` server is our exact fixture string: `"Fictional sample data: Amsterdam: 16 C, light rain"`.
>
> Now let's click **step 02, A2A summarize**. Look at the left box—the **Input** sent to `default/summarizer`. Right there inside the prompt is that exact `"Amsterdam: 16 C, light rain"` string from the MCP server, combined with our visitor context. On the right is the concise English summary generated by the summarizer agent.
>
> Now click **step 03, A2A translate**. Look at the **Input** sent to `default/translator`: it matches the summarizer's output character for character, and on the right is the French translation. That proves all three hops actually executed and handed off real data.
>
> Finally, if I expand **Registry versions observed before the calls**, you can see all three contracts—`weather`, `summarizer`, and `translator`—each with its version and `globalId`. One important boundary to keep in mind: that version identifies the contract stored in the registry when the workflow read it, not a cryptographic pin to the running JVM binary.
>
> Now let's break one of those contracts and see what happens.

*(Fallback if the live panel fails: click **Open dashboard in a separate tab** or run the `curl` in Section 5.)*

---

### Slide 8 — A small metadata change. A broken promise.
**13:30–15:00 · ~195 words · Explain the invariant before clicking Run governance.**

> Suppose the team that owns the Translator agent prepares a new release and tries to publish an updated Agent Card.
>
> On the left is the accepted card currently in Apicurio Registry: it declares the skill `id: translator`. On the right is the candidate card they submit. Notice two things about this abbreviated skill-ID diff. First, it removes `id: translator`. Second, it adds a different skill, `id: unrelated-...`. We keep a skill in the array on purpose: we don't want the registry to reject the card simply because the skills list is empty or the JSON is malformed. We want a true **semantic compatibility check**.
>
> Because we attached a `BACKWARD` compatibility rule to the `translator` artifact in Apicurio Registry, the registry understands the `AGENT_CARD` artifact type natively. It doesn't just check JSON syntax—it compares the proposed skills list against the accepted version. Removing an existing skill breaks callers that rely on it, so we expect Apicurio Registry to reject this publication with HTTP **400** and tell us specifically: `Skill 'translator' was removed`.
>
> Now, in a real system, seeing an HTTP 400 error on its own isn't enough proof. What if the registry partially wrote the version, or corrupted the latest pointer? Let's test both the rejection and what survived.

---

### Slide 9 — Don't just show the error. Check what survived.
**15:00–18:00 · ~280 words + ~45s live execution & console view · Demo 2 of 3 inside the slide.**

*Actions:*
1. Click **Run governance** once. *(Do NOT press Space; use **→** when moving on.)*
2. Immediately deliver the **"While it runs"** block — this covers the ~15–20 seconds while the baseline briefing runs before the first output line streams in.
3. Point to the coral `REJECTED` block and the two green `PASS:` lines as they appear.
4. Click `translator versions ↗` (or switch to the preloaded Registry console tab at `http://localhost:8888/explore/default/translator`) to show the version history visually.

*Click **Run governance**, then say right away while the timer ticks:*

> I'll click **Run governance** right here on the slide. You'll see the timer ticking because this script isn't just firing a single `curl`—it runs a full end-to-end verification sequence without pausing between phases.
>
> First, it runs a baseline briefing and records the exact accepted translator version and `globalId`. Second, it submits that breaking Agent Card with the `translator` skill removed and asserts that Apicurio returns HTTP 400 with `RuleViolationException` and the exact `/skills` cause. Third, it checks the three things listed on the right of this slide: it queries the rejected version ID directly to confirm a **404**—it was never stored; it reads the latest version to verify that the accepted content, version, and `globalId` are untouched; and it reruns the entire live briefing end to end to prove the workflow keeps working. Finally, it also submits a compatible additive version and reruns the briefing one more time.

*As the formatted output streams in — point to the coral block, then the green PASS lines:*

> Look at the output. In orange: **REJECTED · HTTP 400 · RuleViolationException — Skill 'translator' was removed (`/skills`)**. That's a semantic diff straight from Apicurio Registry's `AGENT_CARD` compatibility engine, not a generic JSON schema failure.
>
> Right below it in green: **PASS: connected briefing still works with the unchanged translator registration**. And right below that, the final phase just finished too: **PASS: Registry accepted additive translator version; connected briefing reports the new registration**.

*Switch briefly to the Registry console tab (`http://localhost:8888/explore/default/translator`), then return to the deck:*

> Let's glance at the Apicurio Registry console for `translator`. Look at the version history: you see the initial `1.0.0` version, and the compatible additive version that was just accepted. The `breaking-demo` version that was rejected never appears in history at all. That's what a clean publication boundary looks like. Let's look closer at that second, compatible change.

---

### Slide 10 — Keep the promise. Publish the addition.
**18:00–19:15 · ~160 words · Clarify metadata acceptance vs. runtime implementation.**

> That last `PASS` line we just saw tested the positive path, because a governance rule that rejects everything is useless—teams still need to ship new capabilities safely.
>
> Here, the candidate Agent Card kept `id: translator` intact and added a second declared skill alongside it. Under `BACKWARD` compatibility, adding a capability while keeping existing ones is safe for current callers, so Apicurio Registry accepted it, created a new version, and the next briefing automatically observed the new registered version.
>
> Now, I want to be completely transparent about what that step proves and what it does **not** prove. Adding a skill entry to an Agent Card updates the **published contract metadata**. It does not deploy new code into the running translator JVM, and it does not mean the agent suddenly implements that new skill. That's still the job of your application tests and deployment pipeline. The registry guarantees the contract evolution rule; your delivery process has to tie contract publication to deployed behavior.
>
> So far we've governed an A2A Agent Card. What about MCP tools?

---

### Slide 11 — Same promise. For tools.
**19:15–21:30 · ~240 words + ~15s live execution & console view · Demo 3 of 3 inside the slide.**

*Actions:*
1. Click **Run tools** once. *(Finishes in ~1–2 seconds — no LLM calls!)*
2. Walk through the output lines: import (`CREATED`), `PASS`, coral `REJECTED` with two causes, `PASS`, `PASS`.
3. Click `the imported tool ↗` (`http://localhost:8888/explore/weather-tools/weather-server.getWeather`) if on schedule, then return to the deck.

*Before and right as you click **Run tools**:*

> Agents aren't the only things sprawling—MCP servers and tools are multiplying even faster. And maintaining hand-written copies of every MCP tool's JSON Schema in a registry quickly drifts out of sync with reality.
>
> So in Apicurio Registry 3.4—which we're running here from a snapshot image—we added live MCP tool import. Let's click **Run tools** and watch how fast this runs, since there are no LLM calls here.

*Point to the streamed blocks:*

> Done in about a second. Look at the three steps that just ran.
>
> First: **Imported from the running weather server: `getWeather` — `CREATED`**. Nobody hand-wrote that tool definition. Apicurio Registry called `tools/list` directly on our running Weather MCP server, extracted `getWeather` and its JSON input schema, stored it as an `MCP_TOOL` artifact, and linked it from an `MCP_SERVER` artifact.
>
> Second, the script put a `BACKWARD` compatibility rule on that imported tool and submitted a breaking change: making a new `units` field required. Look at the coral block: **REJECTED · HTTP 400** with **two** exact JSON Schema causes—properties were narrowed to accept fewer objects, and a property was made required—and the registered tool stayed untouched.
>
> Third, we imported from the unchanged weather server a second time, and Apicurio reported **UNCHANGED**—idempotent, no noisy duplicate versions.
>
> *(Optional, if on schedule: click `the imported tool ↗` in the console.)* If we open that imported tool in the Registry console, there is the real `getWeather` JSON Schema and its compatibility rule, right beside our A2A Agent Cards in the same registry.

---

### Slide 12 — Put each guarantee in the right place.
**21:30–23:15 · ~225 words · The single honest architectural boundary summary.**

> If you take one architectural diagram or table back from this talk, make it this one. Most disappointment with registries—whether API registries, schema registries, or agent registries—comes from expecting one layer to guarantee things that actually belong to another layer.
>
> Let's read the four rows straight across.
>
> Row one: **Locate registered endpoints**. That belongs to the registry and its client extensions—here, Quarkus LangChain4j's Apicurio extensions for A2A and MCP.
>
> Row two: **Choose roles, order, and argument encoding**. That belongs to application code. Even when endpoints are dynamic, making the workflow structure explicit in code keeps your system debuggable and predictable.
>
> Row three: **Check a candidate card or tool**. That belongs to registry publication rules—catching breaking skill removals or incompatible JSON Schema changes before they ever become the latest contract.
>
> Row four, highlighted in orange: **Verify deployed behavior**. That belongs to your automated tests and deployment process.
>
> Let me state the remaining boundaries clearly once. In this default demo, everything runs on loopback without authentication; in a shared deployment, you add OIDC authentication and per-group access rules—which Apicurio Registry already supports, and we have client-side authentication PRs in review in Quarkus LangChain4j. There is no automatic deprecation-routing gate here, and reading registered metadata is an observation, not a deployment pin or a transactional fleet snapshot. Knowing where the registry stops is what lets you design the rest cleanly.

---

### Slide 13 — Discover. Connect. Verify.
**23:15–24:15 · ~130 words · Three crisp takeaways, then stop adding detail.**

> To wrap up, three practical habits you can bring back to your team.
>
> **First: use a registry when ownership warrants it.** If one team deploys everything together, static config is fine. Once multiple teams publish agents and MCP tools independently, move those contracts into a shared registry so endpoints and capability versions have an explicit home.
>
> **Second: make every handoff explicit.** Don't judge a multi-agent workflow only by whether the final paragraph sounds fluent. Inspect the actual inputs and outputs crossing each tool and agent boundary.
>
> **Third: test what a rejected change preserves, and verify running implementations separately.** Don't just assert an HTTP 400—verify that the accepted contract stayed intact and callers kept working, and keep behavioral testing in your delivery pipeline.

---

### Slide 14 — Try the workflow. Inspect the contract.
**24:15–25:00 · ~95 words · Share QR code + links and open for Q&A.**

> Everything you saw today is open source and runnable locally. You can scan the QR code on the right or visit `github.com/carlesarnal/agent-discovery-demo` to get these exact slides, the presenter guides, and the runner scripts.
>
> The connected workflow sample itself ships upstream in **Quarkus LangChain4j 1.15.1**. And if you want to explore every agent registry feature we didn't have time for today—including per-user visibility with Keycloak, the AI Catalog, ARD, the MCP Registry API, and the Registry's own MCP server—check out the Apicurio Registry demo linked at the bottom right.
>
> Thank you very much. I'm happy to take your questions.

---

## 4. Timing and fallback rules

| Checkpoint | Target (130 wpm) | Faster pace (~155 wpm) | If behind |
|---|---:|---:|---|
| Finish architecture & code (slides 1–5) | 9:15 | ~7:45 | Summarize slide 5 in 30s; keep the two traffic paths on slide 4 |
| Finish first briefing (slides 6–7) | 13:30 | ~11:30 | Show handoff fields, not whole paragraphs |
| Finish governance & compatible path (slides 8–10) | 19:15 | ~16:15 | Skip the console tab on slide 9; narrate slide 10 in 20s |
| Finish tool governance (slide 11) | 21:30 | ~18:00 | Skip the console tab on slide 11 |
| Finish takeaways & open Q&A (slides 12–14) | 25:00 | ~21:00 | Never cut slide 12 (ownership table) or slide 13 (takeaways) |

If a live briefing or governance run shows no progress after about 30 seconds, narrate what it is testing once. If the delay persists, switch to your local backup recording and say:

> “The live model call is taking too long, so I'll use a recording of this same revision. The important part is the boundary we're testing.”

Avoid live debugging beyond one quick check. Do not claim the backup is live or interpret a timeout as a successful rejection.

---

## 5. Troubleshooting quick reference

Logs live under:

```text
target/talk-sample/target/run/
  summarizer.log
  translator.log
  weather.log
  orchestrator.log
```

| Symptom | Check / response |
|---|---|
| Model not installed / Ollama connection refused | Start host Ollama; run `ollama list`; install `qwen2.5:7b` before the session |
| Missing application JAR | Run `python3 scripts/workflow.py prepare` before the talk |
| Wrapper rejects a dirty or different checkout | Inspect `git -C target/quarkus-langchain4j status --short`; preserve changes, or delete the directory to fetch the tag again |
| Port already in use | Identify its owner with `lsof -nP -iTCP:11020 -sTCP:LISTEN` (replace port as needed); don't kill unrelated processes |
| “Sample containers already exist” | Run `python3 scripts/stop.py`; the new run refuses to reuse old Registry data |
| Application exits during startup | Read the corresponding log; wait for all four readiness checks before presenting |
| Briefing reports registry-read failure / HTTP 503 | Check Registry health and required artifact metadata; don't mistake absence of metadata for agent-code failure |
| Missing required agent / HTTP 503 | Inspect `/workflow/agents` and publication logs in summarizer/translator logs |
| Slow or failed inference | Check host Ollama and agent logs; switch to the recording instead of rebuilding on stage |
| Weather answer looks plausible but tool execution is uncertain | Inspect `weather.log` for the actual invocation; plausibility alone isn't proof |
| Governance request gets unexpected status | Inspect the exact error. Authentication, connectivity or syntax failures are not successful compatibility checks |
| Slide flow does not animate | Check reduced-motion preference; press **R**. Static slides still carry the complete explanation |
| Slides open on the last page | Remove the URL hash or press **Home** |
| No live buttons on slides 7, 9, or 11 | Open `http://127.0.0.1:8765/` with `scripts/present.py` running; check the toolbar says **Default demo**. Fallback: separate dashboard tab + printed commands in a spare terminal |

Readiness checks:

```bash
curl -fsS http://localhost:8180/apis/registry/v3/system/info
for port in 11010 11020 11030 11040; do
  curl -fsS "http://localhost:$port/q/health/ready"
done
```

Briefing CLI fallback (if the browser UI fails while the backend is healthy):

```bash
curl -fsS --max-time 120 http://localhost:11020/workflow/briefing \
  -H 'Content-Type: application/json' \
  --data '{"city":"Amsterdam","context":"Visitors will walk through the city and meet colleagues to discuss open standards."}' \
  | python3 -m json.tool
```

Optional registration/rule inspection (no writes):

```bash
curl -fsS http://localhost:8180/apis/registry/v3/groups/default/artifacts/translator/versions/branch=latest \
  | python3 -m json.tool
curl -fsS http://localhost:8180/apis/registry/v3/groups/default/artifacts/translator/rules/COMPATIBILITY \
  | python3 -m json.tool
```

### Cleanup and fresh rehearsal

From the repository root, in a spare terminal, run:

```bash
python3 scripts/stop.py
```

This stops the presenter, demo runners, and sample JVMs from this checkout, then tears down the
default and secured Compose projects, including their temporary Registry data. Host Ollama and
unrelated containers stay running. Use `python3 scripts/stop.py --dry-run` to preview. A fresh run
starts with new Registry state.

---

## 6. Likely questions and short answers

### “Why a registry instead of DNS or configuration?”

“Configuration may be enough for a small, coordinated deployment. DNS gives a network name; this example adds versioned capability declarations and checks on publication. It does not replace DNS, a service mesh or deployment health checks.”

### “Does the registry choose the agent?”

“Not in this sample. The application requires the summarizer and translator roles. The extensions resolve their registered locations, and application code chooses the sequence.”

### “Is this an autonomous agent workflow?”

“The briefing is an explicitly orchestrated workflow using remote A2A agents and an MCP tool. The separate weather assistant demonstrates model-driven tool invocation. Neither is a general-purpose autonomous planner.”

### “Why use both MCP and A2A?”

“Here MCP exposes a weather tool and A2A exposes summarization and translation services. This lets us examine both integration paths. It isn't a claim that those capabilities can only use those protocols; interface choice depends on caller requirements and implementation support.”

### “What exactly does BACKWARD compatibility mean here?”

“In the tested AGENT_CARD rule, a proposed version removing the existing translation skill is rejected. Adding a skill while retaining it is accepted. That is the demonstrated property. I would check the artifact rule implementation before generalizing to every card field or another artifact type.”

### “Did the rejection stop an incompatible deployment?”

“No. It stopped publication of that candidate metadata. We did not deploy new agent code during the test. To make the registry decision a deployment gate, your delivery process has to enforce that relationship.”

### “Can I trust the version shown on the dashboard?”

“It identifies the Registry version observed before execution. It doesn't identify the actual deployed binary, pin the endpoint's behavior or give an atomic snapshot across services. Those are separate controls.”

### “Does the newly added skill actually run?”

“No new implementation is deployed in that step. It demonstrates accepted publication and version observation. The translator continues implementing the same translation operation.”

### “What about deprecation?”

“There is no deprecation gate in this sample. A consumer or gateway needs to implement an explicit policy. Registering a lifecycle state alone doesn't stop direct calls to running services.”

### “How do you know the model called the tool?”

“The live smoke test checks the returned fixture and a server-side invocation log. The connected workflow also checks that the actual tool result appears in the summary input. Unit tests separately check sequencing and handoffs using mocks.”

### “Why does the briefing call the weather tool through your own code?”

“The orchestrator wraps search, connect and invoke in one typed method, `weatherForCity(String city)`. In testing, the model sometimes built the generic tool arguments wrong. The wrapper moves connection sequencing and JSON encoding into ordinary application code. The model supplies a city rather than constructing generic invocation arguments. It doesn't eliminate unsupported cities, transport failures, model mistakes or the need for authorization.”

### “What if Registry is down?”

“The briefing first reads required registration metadata. If that read fails, it reports a registry-read error before invocation. That is not a comprehensive outage study of every extension path; there are still races between observing metadata and resolving/calling endpoints.”

### “Is this secure for multiple teams?”

“This demo is loopback-only and unauthenticated, on purpose. The Registry itself already supports authentication with OIDC, roles, and access rules per group, including which agents each user can discover. What isn't released yet is the client side: the Quarkus extensions authenticating to the Registry and to the agents they call. Those changes are in review upstream. A shared deployment also needs tenant boundaries, credential management and operational policies; I haven't demonstrated those here.”

### “Who can publish or change a card?”

“Whoever the Registry authorizes to write to that group. With authentication on, that's a role plus, optionally, per-group access rules. Labels and references never grant access. In this demo nothing is authenticated, so anyone on the laptop could.”

### “Can this run on Kubernetes?”

“The architectural pattern can, but this sample runs four local JVMs and a Registry container. It doesn't supply a tested Kubernetes deployment. Moving it to a cluster requires reachable advertised URLs, service configuration and security/lifecycle work.”

### “Are the integrations released?”

“Yes. Both extensions and this sample ship in Quarkus LangChain4j 1.15.1, and the demo builds against that release. Registry authentication for the extensions is still in review, which is why the demo runs without it. Importing tools from a running MCP server is in Apicurio Registry 3.4; the demo uses its snapshot image until then.”

### “What performance or scale have you measured?”

“This is a functional integration demonstration, not a fleet-scale or latency benchmark. We verified the handoffs and compatibility outcomes. Model response times on my laptop aren't a production capacity claim.”

---

## 7. Final rehearsal checklist

- [ ] Confirm exact event date/time, room, and whether Q&A is inside the 25 minutes.
- [ ] Record the exact repository commit and sample release used for rehearsal.
- [ ] Complete `prepare` and one live `smoke` run on the presentation laptop.
- [ ] Rehearse the full verbatim script out loud with a stopwatch, including all three in-slide demos and console views.
- [ ] Run from a fresh Registry (`scripts/stop.py`, then `workflow.py run`) and warm Ollama with one briefing before the session.
- [ ] Confirm toolbar says **Default demo**, counter says **1 / 14**, and slides 7, 9, and 11 show live buttons.
- [ ] Confirm speaker notes are closed on the projected display and the QR code on slide 14 scans cleanly with a phone.
- [ ] Have a local backup recording ready without network access.
- [ ] Use **→** (not Space) to advance slides after interacting with live demo buttons.

**Closing line:** “Discover the endpoints. Make the handoffs explicit. Test the contract—and verify the implementation separately.”
