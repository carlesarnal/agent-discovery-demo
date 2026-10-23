# Presentation Guide — Cloud Native AI Summit Europe 2026

**Talk:** Agent Sprawl Is the New Microservice Sprawl: Discovery and Governance for Multi-Agent AI

**Speaker:** Carles Arnal — Principal Software Engineer, IBM

**Event:** December 2–3, 2026, CNIT Forest, Paris

**Slot:** 25 minutes; exact presentation date/time and room to confirm

**Deck:** `presentation.html`, 13 slides, numbered 1–13

**Demo:** connected workflow pinned from Quarkus LangChain4j PR #2932

Use this as a rehearsal script, not text to memorize word for word. Aim for **22 minutes of content plus 3 minutes of questions or recovery time**.

## The one sentence to remember

> “The registry tells us where the contracts live and checks what can be published. The application still owns the workflow, and tests still have to verify what the running code does.”

The demonstration is deliberately small: fictional weather → summary → French translation. Its purpose is to make boundaries visible, not demonstrate autonomous planning, a large production fleet or model quality.

---

## 1. Prepare before conference day

Run commands from **this branch's repository root**. If your normal checkout contains other work, use the separate Paris worktree; don't overwrite that work to switch branches.

### Check prerequisites

```bash
git branch --show-current
java -version
python3 --version
docker info
docker compose version
ollama list
```

- Branch: `cloud-native-paris-2026`.
- Use Java 25 to match the recorded verification. The pinned sample declares Java 17+.
- Python 3.9+, Docker Compose v2 and Git are required.
- Run Ollama on the host. On macOS this allows native acceleration; the demo no longer starts an Ollama container.
- Allow enough memory for the 7B model and four JVM processes. Close unrelated heavy workloads.
- `jq` is useful for the optional inspection commands in this guide, but isn't required by the runner.

### Download and build while you have reliable internet

If Ollama is not running, start the desktop app or run `ollama serve` in a separate terminal. Do not start a second server if one already uses port 11434.

```bash
ollama pull qwen2.5:7b
python3 scripts/workflow.py prepare
```

`prepare` fetches commit `682928b714574e1ca8f0405c4319b36cd244dc0e`, builds the required extensions and runs the sample's nine automated tests. The source and isolated Maven cache live under `target/`:

```text
target/registry-workflow-source/samples/agent-registry-workflow/
target/workflow-m2/
```

These are checkout-built `999-SNAPSHOT` extensions, not a released upstream sample. The pin makes the source revision explicit. Do not update versions immediately before presenting.

### Run the live smoke test

Stop any interactive instance of this sample first:

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

These checks validate invocation, handoffs and publication behavior. Read the generated text yourself: a passing smoke run does not prove translation accuracy or factual correctness in general.

### Record a backup

Record the dashboard briefing and the governance command on the same pinned revision and model. Save the video locally and verify playback without network access. Record actual execution; don't fabricate successful output.

The governance command makes **three complete briefings**: baseline, after rejection and after compatible publication. That means several model calls. Time it on the presentation laptop; correctness tests do not establish its stage duration.

---

## 2. Venue setup — 30 minutes before

### Terminal A: keep the demo running

```bash
bash scripts/paris-2026-demo.sh
```

Wait for:

```text
Ready: http://localhost:11020/workflow/agents
```

The runner starts a dedicated ephemeral Registry in Docker and four local Java processes. It registers the weather tool and installs the translator's BACKWARD compatibility rule. **Leave Terminal A running.** Ctrl+C stops the demo and removes its Registry data.

### Terminal B: checks and on-stage commands

```bash
curl -fsS http://localhost:8180/apis/registry/v3/system/info
curl -fsS http://localhost:11020/workflow/agents | python3 -m json.tool
```

You should see the two roles `summarizer` and `translator`. Descriptions may vary with future revisions; the current sample expects those role names.

| Component | Address |
|---|---|
| Slides | Local `presentation.html` file |
| Briefing dashboard | http://localhost:11020/ |
| Orchestrator API | http://localhost:11020/workflow |
| Registry API | http://localhost:8180/apis/registry/v3 |
| Summarizer | http://localhost:11010 |
| Translator | http://localhost:11030 |
| Weather MCP endpoint | http://localhost:11040/mcp |
| Host Ollama | http://localhost:11434 |

There is **no Registry UI deployed by this sample**, no embedded terminal and no need for ttyd or Kubernetes port-forwarding.

### Open the browser

```bash
open presentation.html
open http://localhost:11020/
```

The slides work from a local file and have no external font/assets dependency. If a browser restricts local-file functionality, serve this repository on loopback:

```bash
python3 -m http.server 8082 --bind 127.0.0.1
```

Then open http://localhost:8082/presentation.html. Don't serve it on a public interface.

Preload three tabs/windows:

1. Presentation, slide 1.
2. Briefing dashboard with **Amsterdam** selected.
3. Local backup recording, paused and ready.

Suggested input:

```text
Visitors will walk through the city and meet colleagues to discuss open standards.
```

Run one briefing before the audience arrives to warm the model. Do not run the governance command on that fresh instance yet if you want the simplest one-skill baseline on stage. If you do rehearse it, stop and restart Terminal A to get an empty Registry, then warm with only a briefing.

### Presentation controls

- **← / →**, Page Up / Page Down: navigate.
- **F:** fullscreen.
- **N:** notes overlay; **Escape:** close notes.
- **R:** replay animations; slide 4 also has a replay button.
- **Home / End:** first/last slide.
- `#slide-7` in the URL jumps directly to slide 7.

The notes overlay appears on the same screen as the slides. Don't open it on the projected display unless you intend the audience to see it. Keep this guide on a separate, non-mirrored display or print it.

Animations illustrate order, **not real traffic or elapsed time**. Reduced-motion settings disable them; printing gives complete static slides.

---

## 3. Slide-by-slide delivery

### Slide 1 — Agent sprawl is the new microservice sprawl
**0:00–0:45 · Introduce the problem, not the whole stack.**

Suggested words:

> “I'm Carles. I maintain Apicurio Registry and contribute to Quarkus. Today we're going to connect a tool and two agents, change one of their published contracts, and see exactly what the registry protects.”
>
> “It's a small example on purpose. If we can't explain the boundaries with three services, adding twenty won't help.”

**Transition:** “First: do we even need a registry?”

### Slide 2 — Three URLs are fine. Until ownership changes.
**0:45–2:00 · Give the audience permission to keep simple systems simple.**

> “If one team owns all three services and releases them together, putting three URLs in configuration may be perfectly reasonable.”
>
> “The problem changes when teams publish and evolve capabilities independently. Where does a caller find the endpoint? Where does it inspect the declared interface? And where can we review a change before publishing it?”

Point to the two columns. Don't claim independent teams or a production fleet were load-tested by this sample. A registry adds a service to operate, not free reliability.

**Transition:** “In this example, we give that registry three jobs.”

### Slide 3 — A shared contract. Three useful jobs.
**2:00–3:00 · Locate, review, check.**

> “Locate: publish definitions and resolve endpoints. Review: keep versions so a changed declaration can be inspected. Check: apply a publication rule—in our case, a new card must preserve the translation skill.”
>
> “What isn't on this list is deciding the workflow. That remains application code.”

**Transition:** “Let's put those responsibilities on a diagram.”

### Slide 4 — Discover through the registry. Call the services directly.
**3:00–5:00 · Use the flow animation to explain the two paths.**

Let the animation run once, or press **R** when ready to narrate it.

> “The text agents publish Agent Cards through the Quarkus LangChain4j extension. The runner registers the Weather MCP service's metadata explicitly.”
>
> “The orchestrator discovers the registered endpoints. Then it calls the services directly: weather, summarization, translation. Apicurio isn't proxying those requests.”
>
> “The sequence and role names are fixed in our application. Network locations come from the registry.”

Point out:
- Orange paths: publication and discovery requests.
- Teal paths: direct calls from the orchestrator.
- The summarizer does **not** call the translator itself; the orchestrator passes the summary into a second call.

**Transition:** “The interesting evidence is what crosses those handoffs.”

### Slide 5 — Follow the data. Not just the final answer.
**5:00–6:00 · Define what success means before showing it.**

> “Weather is a fixture: fictional Amsterdam weather, 16 degrees and light rain. That output goes into the summarizer along with our context. The summary, not the original input, then goes into the translator.”
>
> “A plausible French paragraph isn't enough to prove that happened. We'll look at the intermediate values.”

**Transition:** “Let's run one.”

### Slide 6 — One briefing. Every handoff visible.
**6:00–11:00 · Switch to the dashboard; don't build on stage.**

Terminal A is already running. The command on the slide explains the entry point; do not launch a second instance.

1. Open the dashboard at http://localhost:11020/.
2. Choose **Amsterdam**.
3. Paste the visitor-context sentence above.
4. Click **Create briefing in French** once.
5. While it runs, explain the role split; don't repeatedly click.

Show, in this order:

**Registered contracts observed before execution**
> “These are the definitions we observed in Registry. The version and globalId identify the registration, not a deployed binary.”

**MCP weather**
> “This is the tool's actual fixture output. It is not a live weather forecast.”

**A2A summarize**
> “Here is that weather output inside the next request, together with our context.”

**A2A translate**
> “And this input is the summary we just received. That's the handoff we're checking.”

Don't read the complete generated paragraphs aloud. Their exact wording is variable.

If the dashboard fails but the backend works, use Terminal B:

```bash
curl -fsS --max-time 120 http://localhost:11020/workflow/briefing \
  -H 'Content-Type: application/json' \
  --data '{"city":"Amsterdam","context":"Visitors will walk through the city and meet colleagues to discuss open standards."}' \
  | python3 -m json.tool
```

**Transition:** “Now let's change the declaration that this workflow depends on.”

### Slide 7 — A small metadata change. A broken promise.
**11:00–12:00 · Explain the candidate before running the check.**

> “The accepted card advertises a translator skill. This candidate removes it and leaves an unrelated skill. The card isn't empty: we want to test compatibility, not just whether an empty card is invalid.”
>
> “Registry has a BACKWARD compatibility rule on this artifact. We expect it to reject removal of that declared skill.”

The slide is an abbreviated skill-ID diff. The actual card is JSON with additional fields; do not present the slide as a literal request payload.

**Transition:** “A 400 on its own wouldn't tell us enough. Let's check what survived.”

### Slide 8 — Don't just show the error. Check what survived.
**12:00–16:00 · Run the governance command in Terminal B.**

```bash
python3 scripts/workflow.py governance
```

This command has several phases. It does not pause between them:

1. Run a baseline briefing and observe the translator registration.
2. Submit the card with the translator skill removed.
3. Check the exact rejection, not just any HTTP failure.
4. Verify the rejected version is absent and accepted metadata is unchanged.
5. Rerun the connected briefing.
6. Publish a compatible addition and rerun again.

Point to the error:

```text
HTTP 400
RuleViolationException
Skill 'translator' was removed
/skills
```

Then point to:

```text
PASS: connected briefing still works with the unchanged translator registration
```

Suggested words:

> “We checked the exact cause, checked that the rejected version does not exist, and checked that the accepted content and version stayed the same.”
>
> “The same running workflow still works after that failed publication. We have not deployed changed agent code, and this result doesn't claim to stop someone doing so independently.”

Keep Terminal B's output visible while it completes. The checks happen inside the script; don't manually repeat all the requests on stage.

**Transition:** “Now the positive path: a change that keeps the existing promise.”

### Slide 9 — Keep the promise. Publish the addition.
**16:00–17:00 · Explain the final phase of that same command.**

Expected final message:

```text
PASS: Registry accepted additive translator version; connected briefing reports the new registration
```

> “The translator skill remains, and an additional skill declaration is accepted. The next briefing reports the new registered version.”
>
> “That does not implement a new skill in the running translator. This experiment changes metadata only. It demonstrates publication and observation, not a code rollout.”

If useful, run another briefing in the dashboard to show the new version—but only if comfortably ahead of schedule. Don't launch a second governance command just to show the addition again.

**Transition:** “There was also a very practical integration lesson while building this.”

### Slide 10 — Let the model choose the city. Let code shape the call.
**17:00–19:00 · Show one engineering decision, not a second full demo.**

> “The generic MCP tools exposed search, connection and invocation details. During sample testing, the model sometimes supplied an object where an API expected a JSON string, or omitted the connection key.”
>
> “We wrapped that sequence in one typed application tool: `weatherForCity(String city)`. Code performs search, connection and invocation, and Jackson encodes the arguments.”
>
> “The separate weather assistant lets the model call that tool. Our connected briefing calls the same wrapper directly—its sequence is fixed.”

Only if asked or ahead of time, inspect:

```text
target/registry-workflow-source/samples/agent-registry-workflow/
  orchestrator/src/main/java/io/quarkiverse/langchain4j/sample/registry/orchestrator/
    DiscoveredWeather.java
    WorkflowResource.java
```

Or run the separate model-driven endpoint:

```bash
curl -fsS --max-time 120 http://localhost:11020/workflow/weather \
  -H 'Content-Type: text/plain' \
  --data 'What is the weather in Amsterdam?'
```

This is optional. Don't let it consume the closing explanation.

### Slide 11 — Put each guarantee in the right place.
**19:00–20:30 · Use the table as the answer to “what does this solve?”**

> “Registry extensions locate endpoints. Application code chooses roles, order and argument encoding. Registry rules check proposed card versions. Tests and deployment controls have to verify actual behavior.”
>
> “The failure is expecting one of these layers to guarantee all the others.”

Mention once:
- Observed metadata isn't a deployment pin or a transactional snapshot.
- This sample doesn't implement a deprecation-routing gate.
- It is local and unauthenticated; authorization is additional work for a shared deployment.

**Transition:** “Three things to take back.”

### Slide 12 — Discover. Connect. Verify.
**20:30–22:00 · Three takeaways, then stop adding detail.**

> “First, use a registry when independent ownership makes publication and discovery worth centralizing.”
>
> “Second, make handoffs explicit. Check the real data passing between a tool and the agents—not just whether the final answer looks good.”
>
> “Third, test what a rejected change preserves. And test deployed behavior separately.”

### Slide 13 — Try the workflow. Inspect the contract.
**22:00–25:00 · Questions and buffer.**

> “The example, checks and setup commands are linked here. The demo pins the proposed Quarkus LangChain4j sample so you can inspect the same revision. Thank you—happy to take questions.”

If the PR merges or a release ships before the event, recheck the status and update the slide and guide. Until then, call it a **proposed upstream sample**, not an already released feature bundle.

---

## 4. Timing and fallback rules

| Checkpoint | Target | If behind |
|---|---:|---|
| Finish architecture | 6:00 | Skip implementation class names; retain the two traffic paths |
| Finish first briefing | 11:00 | Show handoff fields, not whole paragraphs |
| Explain rejected publication | 16:00 | Use the local backup if the live command is still waiting |
| Finish compatible path and wrapper lesson | 19:00 | Explain the verified compatible result without another dashboard request |
| Start questions | 22:00 | Keep the ownership table and three takeaways; omit optional code inspection |

The governance runner executes all phases together. “Skip the last rerun” means **stop waiting on stage and continue explaining**, not that the current command has an interactive skip feature.

If nothing useful has appeared after about 20–30 seconds, continue narrating the expected sequence once. If the delay persists, switch to the recording and say:

> “The live model call is taking too long, so I'll use a recording of this same revision. The important part is the boundary we're testing.”

Avoid live debugging beyond one quick check. Do not claim the backup is live or interpret a timeout as a successful rejection.

---

## 5. Troubleshooting quick reference

Logs live under:

```text
target/registry-workflow-source/samples/agent-registry-workflow/target/run/
  summarizer.log
  translator.log
  weather.log
  orchestrator.log
```

| Symptom | Check / response |
|---|---|
| Model not installed / Ollama connection refused | Start host Ollama; run `ollama list`; install the configured model before the session |
| Missing application JAR | Run `python3 scripts/workflow.py prepare` before the talk |
| Wrapper rejects dirty or different source revision | Inspect `git -C target/registry-workflow-source status --short` and `rev-parse HEAD`; preserve changes rather than resetting blindly |
| Port already in use | Identify its owner with `lsof -nP -iTCP:11020 -sTCP:LISTEN` (replace port as needed); don't kill unrelated processes |
| “Sample containers already exist” | Find the existing runner and stop it normally; the new run refuses to reuse its data |
| Application exits during startup | Read the corresponding log; wait for all four readiness checks before presenting |
| Dashboard reports registry-read failure / HTTP 503 | Check Registry health and required artifact metadata; don't mistake absence of metadata for agent-code failure |
| Missing required agent / HTTP 503 | Inspect `/workflow/agents` and publication logs in summarizer/translator logs |
| Slow or failed inference | Check host Ollama and agent logs; switch to the recording instead of rebuilding on stage |
| Weather answer looks plausible but tool execution is uncertain | Inspect weather-server logs for the actual invocation; plausibility alone isn't proof |
| Governance request gets unexpected status | Inspect the script's exact error. Authentication, connectivity or syntax failures are not successful compatibility checks |
| Slide flow does not animate | Check reduced-motion preference; press R. Static slides still carry the complete explanation |
| Slides open on the last page | Remove the URL hash or press Home |

Readiness checks:

```bash
curl -fsS http://localhost:8180/apis/registry/v3/system/info
for port in 11010 11020 11030 11040; do
  curl -fsS "http://localhost:$port/q/health/ready"
done
```

Optional registration/rule inspection (no writes):

```bash
curl -fsS http://localhost:8180/apis/registry/v3/groups/default/artifacts/translator/versions/branch=latest \
  | python3 -m json.tool
curl -fsS http://localhost:8180/apis/registry/v3/groups/default/artifacts/translator/rules/COMPATIBILITY \
  | python3 -m json.tool
```

### Cleanup and fresh rehearsal

Stop Terminal A with Ctrl+C and let cleanup finish. It terminates the sample's four Java processes and removes the ephemeral Registry container/network. Host Ollama stays running. A fresh run starts new Registry state; do not assume previous publication history persists.

If a crash left only the sample's Docker resources behind, **first confirm no sample JVM/runner is still running**, then:

```bash
docker compose -p langchain4j-agent-workflow \
  -f target/registry-workflow-source/samples/agent-registry-workflow/compose.yaml ps

docker compose -p langchain4j-agent-workflow \
  -f target/registry-workflow-source/samples/agent-registry-workflow/compose.yaml down
```

The second command removes this sample's ephemeral Registry data. It does not stop orphaned host JVMs; inspect their process IDs before terminating them. Don't use the removed legacy `cleanup.sh` or `docker-compose.yaml` commands.

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

### “Does the typed wrapper eliminate tool errors?”

“It moves connection sequencing and JSON encoding into ordinary application code. The model supplies a city rather than constructing generic invocation arguments. It doesn't eliminate unsupported cities, transport failures, model mistakes or the need for authorization.”

### “What if Registry is down?”

“The briefing first reads required registration metadata. If that read fails, it reports a registry-read error before invocation. That is not a comprehensive outage study of every extension path; there are still races between observing metadata and resolving/calling endpoints.”

### “Is this secure for multiple teams?”

“This demo is loopback-only and unauthenticated. A shared deployment needs authenticated publication and calls, authorization, tenant boundaries, credential management and operational policies. I haven't demonstrated those guarantees here.”

### “Can this run on Kubernetes?”

“The architectural pattern can, but this sample runs four local JVMs and a Registry container. It doesn't supply a tested Kubernetes deployment. Moving it to a cluster requires reachable advertised URLs, service configuration and security/lifecycle work.”

### “Are the integrations released?”

“This wrapper builds the extension source from a pinned checkout because the sample targets those changes. PR #2932 was open when we prepared this version. The README gives the exact revision and build commands; I won't call it a released sample until that is true.”

### “What performance or scale have you measured?”

“This is a functional integration demonstration, not a fleet-scale or latency benchmark. We verified the handoffs and compatibility outcomes. Model response times on my laptop aren't a production capacity claim.”

---

## 7. Final rehearsal checklist

- [ ] Recheck event slot, room and whether questions are included in the 25 minutes.
- [ ] Record the exact branch commit and pinned sample revision used for rehearsal.
- [ ] Complete `prepare` and one live smoke run on the presentation laptop.
- [ ] Time the entire talk, including browser/terminal switching and model calls.
- [ ] Run from a fresh Registry and warm the model before the main rehearsal.
- [ ] Practice explaining the architecture in under two minutes.
- [ ] Practice the “rejected metadata is not blocked deployment” answer in one sentence.
- [ ] Have a local recording and static screenshots ready without internet access.
- [ ] Confirm that notes are not visible on the projected display.
- [ ] Start questions by minute 22; finish at minute 25.

**Closing line:** “Discover the endpoints. Make the handoffs explicit. Test the contract—and verify the implementation separately.”
