#!/usr/bin/env python3
"""The Paris workflow with authentication on every hop.

Keycloak (realm "agents", http://localhost:8190) issues every identity. The Registry and its
console require a login and enforce roles; the agents' A2A endpoints accept only callers with the
a2a-caller role, the weather MCP endpoint only callers with mcp-caller. The applications publish
and discover through the secured Registry, and the orchestrator sends its own token to the agents
and to the MCP server it discovers.

The client side is not released yet: this builds Quarkus LangChain4j from a branch combining
quarkiverse/quarkus-langchain4j#3145 (Registry credentials), #3157 (credentials for A2A agents)
and the #3205 change on top of it (the same credentials on the Agent Card request), installed as
999-SNAPSHOT, and the 2.x sample (Quarkus 4) on top of it. The weather
server stays on the 1.15.0 sample (Quarkus 3): the MCP server extension does not support Quarkus 4
yet. Agent Cards are protected like the A2A endpoints.

Localhost-only demo credentials (secured/realm.json). Do not reuse them anywhere else.
"""
import argparse
import json
from pathlib import Path
import shutil
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request

import workflow

ROOT = workflow.ROOT
SECURED = ROOT / "secured"
QL4J = ROOT / "target" / "quarkus-langchain4j-secured"
QL4J_REMOTE = "https://github.com/carlesarnal/quarkus-langchain4j.git"
QL4J_BRANCH = "demo/paris-auth"
QL4J_COMMIT = "c169e046351c88c89868bc6affc234d2f8032fa4"
SAMPLE = ROOT / "target" / "secured-sample"
WEATHER = ROOT / "target" / "secured-weather"
PROJECT = "langchain4j-agent-workflow-secured"
KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:26.2"
TOKEN_URL = "http://localhost:8190/realms/agents/protocol/openid-connect/token"
EXTENSIONS = "model-providers/ollama/deployment,a2a-apicurio-registry/deployment,mcp-apicurio-registry/deployment"
PACKAGE = "io/quarkiverse/langchain4j/sample/registry"

COMPOSE = f"""# Written by scripts/secured.py. Localhost-only demo credentials: for the demo only.
services:
  keycloak:
    image: {KEYCLOAK_IMAGE}
    environment:
      KC_BOOTSTRAP_ADMIN_USERNAME: admin
      KC_BOOTSTRAP_ADMIN_PASSWORD: admin
      KC_HEALTH_ENABLED: "true"
    command: ["start-dev", "--import-realm", "--hostname-strict=false"]
    ports:
      - "127.0.0.1:8190:8080"
    volumes:
      - ./realm.json:/opt/keycloak/data/import/realm.json:ro
    healthcheck:
      test: ["CMD-SHELL", "exec 3<>/dev/tcp/localhost/9000 && printf 'GET /health/ready HTTP/1.0\\\\r\\\\n\\\\r\\\\n' >&3 && grep -q UP <&3"]
      interval: 5s
      timeout: 5s
      retries: 40
  registry:
    image: {workflow.REGISTRY_IMAGE}
    ports:
      - "127.0.0.1:8180:8080"
    environment:
      APICURIO_STORAGE_KIND: sql
      APICURIO_STORAGE_SQL_KIND: h2
      APICURIO_FEATURES_EXPERIMENTAL_ENABLED: "true"
      APICURIO_MCP_IMPORT_ENABLED: "true"
      APICURIO_MCP_IMPORT_ALLOWED_HOSTS: "weather-proxy"
      APICURIO_MCP_IMPORT_INSECURE_HTTP_ENABLED: "true"
      QUARKUS_HTTP_CORS_ORIGINS: "http://localhost:8888"
      # Every request needs a token from the demo realm; roles come from the token
      QUARKUS_OIDC_TENANT_ENABLED: "true"
      QUARKUS_OIDC_AUTH_SERVER_URL: "http://keycloak:8080/realms/agents"
      QUARKUS_OIDC_CLIENT_ID: "registry-api"
      # The applications get their tokens from http://localhost:8190, the Registry reads the
      # realm at http://keycloak:8080: the issuers differ only by host name
      QUARKUS_OIDC_TOKEN_ISSUER: "any"
      APICURIO_AUTH_ROLE_BASED_AUTHORIZATION: "true"
      APICURIO_AUTH_ANONYMOUS_READ_ACCESS_ENABLED: "false"
      APICURIO_UI_AUTH_OIDC_CLIENT_ID: "apicurio-console"
      APICURIO_UI_AUTH_OIDC_REDIRECT_URI: "http://localhost:8888/"
    depends_on:
      keycloak:
        condition: service_healthy
  weather-proxy:
    image: docker.io/library/nginx:1.27-alpine
    volumes:
      - ./weather-proxy.conf:/etc/nginx/conf.d/default.conf:ro
  console:
    image: {workflow.UI_IMAGE}
    ports:
      - "127.0.0.1:8888:8080"
    environment:
      REGISTRY_API_URL: "http://localhost:8180/apis/registry/v3"
      REGISTRY_AUTH_TYPE: "oidc"
      REGISTRY_AUTH_URL: "http://localhost:8190/realms/agents"
      REGISTRY_AUTH_CLIENT_ID: "apicurio-console"
      REGISTRY_AUTH_REDIRECT_URL: "http://localhost:8888/"
      REGISTRY_AUTH_RBAC_ENABLED: "true"
"""

# Inserted into the sample's scripts/run.py: Registry calls carry the demo-admin token, the stack
# waits for Keycloak, and smoke ends with the checks that unauthenticated or under-privileged
# callers are refused.
RUN_HELPERS = '''
import urllib.parse

TOKEN_URL = "http://localhost:8190/realms/agents/protocol/openid-connect/token"
_tokens = {}


def token(client):
    cached = _tokens.get(client)
    if cached and cached[1] > time.monotonic():
        return cached[0]
    body = urllib.parse.urlencode({"grant_type": "client_credentials", "client_id": client,
                                   "client_secret": client + "-secret"}).encode()
    with urllib.request.urlopen(urllib.request.Request(TOKEN_URL, data=body), timeout=30) as response:
        data = json.load(response)
    _tokens[client] = (data["access_token"], time.monotonic() + data["expires_in"] - 60)
    return data["access_token"]


def headers_for(url, content_type):
    headers = {"Content-Type": content_type}
    if url.startswith(REGISTRY):
        headers["Authorization"] = "Bearer " + token("demo-admin")
    return headers


def status_of(url, data=None, client=None, method=None):
    headers = {"Content-Type": "application/json", "Accept": "application/json, text/event-stream"}
    if client:
        headers["Authorization"] = "Bearer " + token(client)
    body = json.dumps(data).encode() if data is not None else None
    req = urllib.request.Request(url, data=body, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=60) as response:
            return response.status
    except urllib.error.HTTPError as error:
        return error.code


def expect_status(label, expected, actual):
    if actual != expected:
        raise AssertionError(f"{label}: expected HTTP {expected}, got {actual}")
    print(f"PASS: {label} (HTTP {actual})", flush=True)


def check_security():
    message = {"jsonrpc": "2.0", "id": "security-check", "method": "SendMessage", "params": {}}
    for name, port in (("summarizer", 11010), ("translator", 11030)):
        expect_status(f"{name} rejects an A2A call without a token", 401,
                      status_of(f"http://localhost:{port}/", message))
        expect_status(f"{name} rejects a token without the a2a-caller role", 403,
                      status_of(f"http://localhost:{port}/", message, "demo-readonly"))
        expect_status(f"{name} rejects an Agent Card request without a token", 401,
                      status_of(f"http://localhost:{port}/.well-known/agent-card.json"))
        expect_status(f"{name} rejects an Agent Card request without the a2a-caller role", 403,
                      status_of(f"http://localhost:{port}/.well-known/agent-card.json", client="demo-readonly"))
        expect_status(f"{name} serves its Agent Card to an a2a-caller", 200,
                      status_of(f"http://localhost:{port}/.well-known/agent-card.json", client="orchestrator"))
    ping = {"jsonrpc": "2.0", "id": 1, "method": "ping"}
    expect_status("weather MCP server rejects a call without a token", 401,
                  status_of("http://localhost:11040/mcp", ping))
    expect_status("weather MCP server rejects a token without the mcp-caller role", 403,
                  status_of("http://localhost:11040/mcp", ping, "demo-readonly"))
    expect_status("Registry rejects an anonymous read", 401,
                  status_of(REGISTRY + "/groups/default/artifacts/translator"))
    expect_status("Registry lets a read-only identity read", 200,
                  status_of(REGISTRY + "/groups/default/artifacts/translator", client="demo-readonly"))
    expect_status("Registry rejects a write from a read-only identity", 403,
                  status_of(REGISTRY + "/groups/default/artifacts/translator/rules",
                            {"ruleType": "VALIDITY", "config": "FULL"}, "demo-readonly"))
'''


def command(*args, cwd=ROOT):
    subprocess.run(args, cwd=cwd, check=True)


def replace(path, old, new, count=1):
    text = path.read_text()
    if text.count(old) != count:
        raise RuntimeError(f"{path}: expected {count} occurrence(s) of {old!r}; the sample changed")
    path.write_text(text.replace(old, new))


def add_dependency(pom, artifact):
    replace(pom, "\t<dependencies>\n", f"\t<dependencies>\n\t\t<dependency>\n\t\t\t<groupId>io.quarkus</groupId>\n"
                                      f"\t\t\t<artifactId>{artifact}</artifactId>\n\t\t</dependency>\n")


def ql4j_snapshot():
    """The Quarkus LangChain4j branch with the auth changes, installed as 999-SNAPSHOT."""
    if not QL4J.exists():
        command("git", "clone", "--quiet", "--branch", QL4J_BRANCH, QL4J_REMOTE, str(QL4J))
    command("git", "checkout", "--quiet", QL4J_COMMIT, cwd=QL4J)
    dirty = subprocess.check_output(["git", "status", "--porcelain"], cwd=QL4J, text=True).strip()
    if dirty:
        raise RuntimeError(f"Expected a clean checkout of {QL4J_COMMIT} in {QL4J}. Delete it to fetch again.")
    command("./mvnw", "-B", "-ntp", "-q", "install", "-DskipTests", "-Dno-format", "-pl", EXTENSIONS, "-am", cwd=QL4J)


def build_sample():
    """The 2.x sample with the secured overlay: OIDC on the agents, client credentials everywhere."""
    shutil.rmtree(SAMPLE, ignore_errors=True)
    shutil.copytree(QL4J / workflow.SAMPLE, SAMPLE, symlinks=True)
    for module in ("text-agent", "orchestrator"):
        properties = SAMPLE / module / "src/main/resources/application.properties"
        properties.write_text(properties.read_text() + "\n" + (SECURED / f"overlay/{module}.properties").read_text())
    add_dependency(SAMPLE / "text-agent/pom.xml", "quarkus-oidc")
    add_dependency(SAMPLE / "orchestrator/pom.xml", "quarkus-oidc-client")

    # The text agent publishes its own card with its own client, so it applies the credentials
    agent = SAMPLE / "text-agent/src/main/java" / PACKAGE / "agent/TextAgent.java"
    replace(agent, ".create(registryConfig.url().orElseThrow()).httpAdapter(HttpAdapterType.JDK));",
            ".create(registryConfig.url().orElseThrow()).httpAdapter(HttpAdapterType.JDK)\n"
            "                .oauth2(registryConfig.auth().tokenEndpoint().orElseThrow(),\n"
            "                        registryConfig.auth().clientId().orElseThrow(),\n"
            "                        registryConfig.auth().clientSecret().orElseThrow(), null));")

    orchestrator = SAMPLE / "orchestrator/src/main/java" / PACKAGE / "orchestrator"
    shutil.copy(SECURED / "overlay/orchestrator/DemoCredentials.java", orchestrator)
    contracts = orchestrator / "ContractClient.java"
    replace(contracts, "import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;",
            "import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;\n"
            "import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;")
    replace(contracts, '@RegisterRestClient(configKey = "contracts")',
            '@RegisterRestClient(configKey = "contracts")\n@RegisterClientHeaders(DemoCredentials.class)')

    # The unauthenticated sample tests do not apply to the secured overlay; the plain sample runs
    # them in workflow.py prepare.
    command("./mvnw", "-B", "-ntp", "-f", str(SAMPLE / "pom.xml"), "package", "-DskipTests",
            "-Dquarkus-langchain4j.version=999-SNAPSHOT", "-Da2a.version=1.5.1.Final", cwd=QL4J)


def build_weather():
    """The 1.15.0 weather server (Quarkus 3) with OIDC on its MCP endpoint."""
    workflow.source()
    shutil.rmtree(WEATHER, ignore_errors=True)
    shutil.copytree(workflow.CHECKOUT / workflow.SAMPLE, WEATHER, symlinks=True)
    properties = WEATHER / "weather-server/src/main/resources/application.properties"
    properties.write_text(properties.read_text() + "\n" + (SECURED / "overlay/weather-server.properties").read_text())
    add_dependency(WEATHER / "weather-server/pom.xml", "quarkus-oidc")
    command("./mvnw", "-B", "-ntp", "-f", str(WEATHER / "pom.xml"), "package", "-DskipTests", "-pl", "weather-server",
            cwd=workflow.CHECKOUT)
    target = SAMPLE / "weather-server/target"
    shutil.rmtree(target, ignore_errors=True)
    shutil.copytree(WEATHER / "weather-server/target/quarkus-app", target / "quarkus-app")


def runner():
    """The sample's run.py, with the secured stack and checks."""
    (SAMPLE / "compose.yaml").write_text(COMPOSE)
    (SAMPLE / "weather-proxy.conf").write_text(workflow.WEATHER_PROXY)
    shutil.copy(SECURED / "realm.json", SAMPLE / "realm.json")
    run = SAMPLE / "scripts/run.py"
    replace(run, 'ORCHESTRATOR = "http://localhost:11020/workflow"\n',
            'ORCHESTRATOR = "http://localhost:11020/workflow"\n' + RUN_HELPERS)
    replace(run, 'headers={"Content-Type": content_type}', "headers=headers_for(url, content_type)")
    replace(run, '"langchain4j-agent-workflow"', f'"{PROJECT}"', count=2)
    replace(run, "for port in (8180, 11010, 11020, 11030, 11040):",
            "for port in (8180, 8190, 8888, 11010, 11020, 11030, 11040):")
    replace(run, "    check_governance()\n\n\ndef main", "    check_governance()\n    check_security()\n\n\ndef main")


def tools():
    """workflow.tools against the secured Registry: admin token, and the import sends the
    demo-admin token (it has mcp-caller) to the weather server. The Registry never stores it."""
    original = workflow.request

    def request(path, data=None, expected=200):
        if path.endswith("/mcp-import") and data is not None:
            data = dict(data, headers={"Authorization": "Bearer " + admin_token()})
        body = json.dumps(data).encode() if data is not None else None
        req = urllib.request.Request(workflow.REGISTRY + path, data=body, method="POST" if body is not None else "GET",
                                     headers={"Content-Type": "application/json",
                                              "Authorization": "Bearer " + admin_token()})
        try:
            with urllib.request.urlopen(req, timeout=60) as response:
                status, raw = response.status, response.read().decode()
        except urllib.error.HTTPError as error:
            status, raw = error.code, error.read().decode()
        if status != expected:
            raise RuntimeError(f"{req.get_method()} {path}: expected {expected}, got {status}: {raw[:600]}")
        return json.loads(raw) if raw else None

    def artifact_status(group, artifact, suffix=""):
        req = urllib.request.Request(f"{workflow.REGISTRY}/groups/{group}/artifacts/{artifact}{suffix}",
                                     headers={"Authorization": "Bearer " + admin_token()})
        try:
            with urllib.request.urlopen(req, timeout=30) as response:
                return response.status
        except urllib.error.HTTPError as error:
            return error.code

    workflow.request, workflow.artifact_status = request, artifact_status
    try:
        unauthenticated = original("/groups/weather-tools/artifacts/weather-server/mcp-import", {
            "url": workflow.WEATHER_MCP_URL, "dryRun": True}, expected=401)
        del unauthenticated
        print("PASS: the Registry refuses an anonymous tool import (HTTP 401)", flush=True)
        workflow.tools()
    finally:
        workflow.request = original


def admin_token():
    body = urllib.parse.urlencode({"grant_type": "client_credentials", "client_id": "demo-admin",
                                   "client_secret": "demo-admin-secret"}).encode()
    with urllib.request.urlopen(urllib.request.Request(TOKEN_URL, data=body), timeout=30) as response:
        return json.load(response)["access_token"]


def check_clock():
    """Tokens are issued inside the container VM and checked on the host: a VM clock that drifted
    (for example while the laptop slept) makes every token look expired."""
    import time
    container = int(subprocess.check_output(
        ["docker", "run", "--rm", "--entrypoint", "date", "docker.io/library/nginx:1.27-alpine", "+%s"],
        text=True).strip())
    drift = abs(container - time.time())
    if drift > 30:
        raise RuntimeError(f"The container VM clock is {drift:.0f}s off the host clock, so tokens would be "
                           "rejected. Fix it with: podman machine ssh \"sudo date -u -s @$(date +%s)\"")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("action", choices=["prepare", "run", "smoke", "governance", "tools"],
                        help="prepare: build the snapshot, the sample and the weather server; "
                             "run: start the secured sample; smoke: live end-to-end and security checks, then stop; "
                             "governance: Agent Card checks against a running sample; "
                             "tools: MCP tool import and checks against a running sample")
    args = parser.parse_args()
    if args.action == "prepare":
        ql4j_snapshot()
        build_sample()
        build_weather()
        runner()
        return
    if args.action == "tools":
        tools()
        return
    if not (SAMPLE / "weather-server/target/quarkus-app/quarkus-run.jar").exists():
        raise RuntimeError("Run: python3 scripts/secured.py prepare")
    if args.action != "governance":
        check_clock()
    flags = [] if args.action == "run" else ["--" + args.action]
    command(sys.executable, "scripts/run.py", *flags, cwd=SAMPLE)


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, AssertionError, subprocess.CalledProcessError) as error:
        sys.exit(str(error))
