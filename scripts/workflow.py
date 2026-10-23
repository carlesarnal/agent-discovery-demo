#!/usr/bin/env python3
"""Build and run the pinned registry workflow from Quarkus LangChain4j PR #2932."""
import argparse
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
CHECKOUT = ROOT / "target" / "registry-workflow-source"
REVISION = "682928b714574e1ca8f0405c4319b36cd244dc0e"
REMOTE = "https://github.com/carlesarnal/quarkus-langchain4j.git"
SAMPLE = Path("samples/agent-registry-workflow")


def command(*args, cwd=CHECKOUT):
    subprocess.run(args, cwd=cwd, check=True)


def source():
    CHECKOUT.parent.mkdir(parents=True, exist_ok=True)
    if not CHECKOUT.exists():
        command("git", "init", str(CHECKOUT), cwd=ROOT)
        command("git", "fetch", "--depth=1", REMOTE, REVISION)
        command("git", "checkout", "--detach", "FETCH_HEAD")
    actual = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=CHECKOUT, text=True).strip()
    dirty = subprocess.check_output(["git", "status", "--porcelain"], cwd=CHECKOUT, text=True).strip()
    if actual != REVISION or dirty:
        raise RuntimeError(f"Expected a clean checkout at {REVISION}: {CHECKOUT}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["prepare", "test", "run", "smoke", "governance"])
    args = parser.parse_args()
    source()
    # Keep the sample's 999-SNAPSHOT artifacts isolated from the user's Maven cache.
    maven = ["./mvnw", "-B", "-ntp", f"-Dmaven.repo.local={ROOT / 'target' / 'workflow-m2'}"]
    if args.action == "prepare":
        command(*maven, "-pl",
                "agentic/deployment,a2a-apicurio-registry/deployment,mcp-apicurio-registry/deployment,model-providers/ollama/deployment",
                "-am", "install", "-DskipTests", "-Dno-format")
    if args.action in ("prepare", "test"):
        command(*maven, "-f", str(SAMPLE / "pom.xml"), "verify")
    else:
        flags = [] if args.action == "run" else ["--" + args.action]
        command(sys.executable, str(SAMPLE / "scripts/run.py"), *flags)


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, subprocess.CalledProcessError) as error:
        sys.exit(str(error))
