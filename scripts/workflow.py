#!/usr/bin/env python3
"""Fetch, build and run the agent registry workflow sample from Quarkus LangChain4j.

The sample lives upstream in quarkus-langchain4j (samples/agent-registry-workflow). It is pinned
to a release tag, so it builds against released extensions, with no source build of the
extensions themselves.
"""
import argparse
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
CHECKOUT = ROOT / "target" / "quarkus-langchain4j"
TAG = "1.15.0"
REMOTE = "https://github.com/quarkiverse/quarkus-langchain4j.git"
SAMPLE = Path("samples/agent-registry-workflow")


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


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["prepare", "test", "run", "smoke", "governance"],
                        help="prepare: fetch and build (with tests); test: run the sample tests; "
                             "run: start the sample; smoke: live end-to-end check, then stop; "
                             "governance: compatibility checks against a running sample")
    args = parser.parse_args()
    source()
    maven = ["./mvnw", "-B", "-ntp", "-f", str(SAMPLE / "pom.xml")]
    if args.action in ("prepare", "test"):
        command(*maven, "verify")
    else:
        flags = [] if args.action == "run" else ["--" + args.action]
        command(sys.executable, str(SAMPLE / "scripts/run.py"), *flags)


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, subprocess.CalledProcessError) as error:
        sys.exit(str(error))
