#!/usr/bin/env python3
"""Fetch, build and run the agent registry workflow sample from Quarkus LangChain4j.

The sample lives upstream in quarkus-langchain4j (samples/agent-registry-workflow), pinned to a
release tag, so it builds against released extensions. The talk runs a copy of it with its own
Compose file: Apicurio Registry from REGISTRY_IMAGE (latest-snapshot by default, for importing
tools from a running MCP server) and the Registry web console on http://localhost:8888.
"""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
CHECKOUT = ROOT / "target" / "quarkus-langchain4j"
TALK_SAMPLE = ROOT / "target" / "talk-sample"
TAG = "1.15.0"
REMOTE = "https://github.com/quarkiverse/quarkus-langchain4j.git"
SAMPLE = Path("samples/agent-registry-workflow")
REGISTRY_IMAGE = os.environ.get("REGISTRY_IMAGE", "quay.io/apicurio/apicurio-registry:latest-snapshot")
UI_IMAGE = os.environ.get("REGISTRY_UI_IMAGE", "quay.io/apicurio/apicurio-registry-ui:latest-snapshot")
REGISTRY = "http://localhost:8180/apis/registry/v3"
# Where the Registry container reaches the weather MCP server. The server listens on the host's
# localhost and, as it should, only accepts requests addressed to localhost. A proxy in the stack
# forwards to it through host.docker.internal (routed to the host's localhost by Docker Desktop and
# Podman on macOS) and presents itself as localhost, so nothing has to listen on other interfaces.
WEATHER_MCP_URL = "http://weather-proxy:8080/mcp"

COMPOSE = f"""# Written by scripts/workflow.py. Localhost-only, no authentication: for the demo only.
services:
  registry:
    image: {REGISTRY_IMAGE}
    ports:
      - "127.0.0.1:8180:8080"
    environment:
      APICURIO_STORAGE_KIND: sql
      APICURIO_STORAGE_SQL_KIND: h2
      APICURIO_FEATURES_EXPERIMENTAL_ENABLED: "true"
      # Import tools from the weather MCP server on the host, and only from there
      APICURIO_MCP_IMPORT_ENABLED: "true"
      APICURIO_MCP_IMPORT_ALLOWED_HOSTS: "weather-proxy"
      APICURIO_MCP_IMPORT_INSECURE_HTTP_ENABLED: "true"
      QUARKUS_HTTP_CORS_ORIGINS: "http://localhost:8888"
  weather-proxy:
    image: docker.io/library/nginx:1.27-alpine
    volumes:
      - ./weather-proxy.conf:/etc/nginx/conf.d/default.conf:ro
  console:
    image: {UI_IMAGE}
    ports:
      - "127.0.0.1:8888:8080"
    environment:
      REGISTRY_API_URL: "http://localhost:8180/apis/registry/v3"
"""


WEATHER_PROXY = """server {
  listen 8080;
  location / {
    proxy_pass http://host.docker.internal:11040;
    proxy_set_header Host localhost:11040;
    proxy_http_version 1.1;
    proxy_buffering off;
  }
}
"""


def command(*args, cwd=CHECKOUT):
    subprocess.run(args, cwd=cwd, check=True)


def source():
    """A clean checkout of the sample at TAG."""
    if not CHECKOUT.exists():
        CHECKOUT.parent.mkdir(parents=True, exist_ok=True)
        command("git", "clone", "--quiet", "--depth=1", "--branch", TAG, REMOTE, str(CHECKOUT), cwd=ROOT)
    actual = subprocess.check_output(["git", "describe", "--tags", "--exact-match"], cwd=CHECKOUT, text=True).strip()
    dirty = subprocess.check_output(["git", "status", "--porcelain"], cwd=CHECKOUT, text=True).strip()
    if actual != TAG or dirty:
        raise RuntimeError(f"Expected a clean checkout of {TAG} in {CHECKOUT}. Delete it to fetch again.")


def talk_sample():
    """The built sample, with the talk's Compose file. Rebuilt from the checkout every time."""
    shutil.rmtree(TALK_SAMPLE, ignore_errors=True)
    shutil.copytree(CHECKOUT / SAMPLE, TALK_SAMPLE, symlinks=True)
    (TALK_SAMPLE / "compose.yaml").write_text(COMPOSE)
    (TALK_SAMPLE / "weather-proxy.conf").write_text(WEATHER_PROXY)
    return TALK_SAMPLE


def request(path, data=None, expected=200):
    body = json.dumps(data).encode() if data is not None else None
    req = urllib.request.Request(REGISTRY + path, data=body, headers={"Content-Type": "application/json"},
                                 method="POST" if body is not None else "GET")
    try:
        with urllib.request.urlopen(req, timeout=60) as response:
            status, raw = response.status, response.read().decode()
    except urllib.error.HTTPError as error:
        status, raw = error.code, error.read().decode()
    if status != expected:
        raise RuntimeError(f"{req.get_method()} {path}: expected {expected}, got {status}: {raw[:600]}")
    return json.loads(raw) if raw else None


def tools():
    """Tool governance on a running sample: import the weather server's tools, then reject a
    breaking change to the imported tool."""
    server = {"name": "io.github.sample/weather", "description": "Fictional sample weather",
              "version": "1.0.0", "remotes": [{"type": "streamable-http", "url": WEATHER_MCP_URL}]}
    status = artifact_status("weather-tools", "weather-server")
    if status == 404:
        request("/groups/weather-tools/artifacts", {
            "artifactId": "weather-server", "artifactType": "MCP_SERVER", "name": "Weather server",
            "firstVersion": {"content": {"contentType": "application/json", "content": json.dumps(server)}},
        })
    result = request("/groups/weather-tools/artifacts/weather-server/mcp-import", {"url": WEATHER_MCP_URL})
    imported = [(t["name"], t["artifactId"], t["status"]) for t in result["tools"]]
    print("Imported from the running weather server:", imported, flush=True)
    tool = next(t for t in result["tools"] if t["name"] == "getWeather")
    base = f"/groups/{tool['groupId']}/artifacts/{tool['artifactId']}"
    if artifact_status(tool["groupId"], tool["artifactId"], "/rules/COMPATIBILITY") == 404:
        request(base + "/rules", {"ruleType": "COMPATIBILITY", "config": "BACKWARD"}, expected=204)
    before = request(base + "/versions/branch=latest/content")
    before_version = request(base + "/versions/branch=latest")["version"]
    print("PASS: tools imported from the running MCP server, no hand-written definitions", flush=True)

    # A breaking change: every caller must now also send "units"
    changed = json.loads(json.dumps(before))
    changed["inputSchema"].setdefault("properties", {})["units"] = {"type": "string"}
    changed["inputSchema"]["required"] = list(changed["inputSchema"].get("required", [])) + ["units"]
    rejection = request(base + "/versions", {
        "content": {"contentType": "application/json", "content": json.dumps(changed)}}, expected=400)
    causes = [(c.get("description"), c.get("context")) for c in rejection.get("causes", [])]
    print("Registry rejected the tool change (HTTP 400):", causes, flush=True)
    if request(base + "/versions/branch=latest")["version"] != before_version:
        raise AssertionError("The rejected tool change replaced the registered tool")
    print("PASS: breaking tool change rejected; the registered tool is unchanged", flush=True)

    again = request("/groups/weather-tools/artifacts/weather-server/mcp-import", {"url": WEATHER_MCP_URL})
    if again["status"] != "UNCHANGED":
        raise AssertionError(f"Re-import changed something: {again}")
    print("PASS: importing again from the unchanged server changes nothing", flush=True)


def artifact_status(group, artifact, suffix=""):
    try:
        with urllib.request.urlopen(f"{REGISTRY}/groups/{group}/artifacts/{artifact}{suffix}", timeout=30) as r:
            return r.status
    except urllib.error.HTTPError as error:
        return error.code


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["prepare", "test", "run", "smoke", "governance", "tools"],
                        help="prepare: fetch and build (with tests); test: run the sample tests; "
                             "run: start the sample; smoke: live end-to-end check, then stop; "
                             "governance: Agent Card checks against a running sample; "
                             "tools: MCP tool import and checks against a running sample")
    args = parser.parse_args()
    if args.action == "tools":
        tools()
        return
    source()
    if args.action in ("prepare", "test"):
        command("./mvnw", "-B", "-ntp", "-f", str(SAMPLE / "pom.xml"), "verify")
        return
    sample = talk_sample() if args.action != "governance" else TALK_SAMPLE
    flags = [] if args.action == "run" else ["--" + args.action]
    command(sys.executable, "scripts/run.py", *flags, cwd=sample)


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, AssertionError, subprocess.CalledProcessError) as error:
        sys.exit(str(error))
