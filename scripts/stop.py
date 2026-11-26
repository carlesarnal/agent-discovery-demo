#!/usr/bin/env python3
"""Stop the Paris demo and presenter from this checkout (leave host Ollama running)."""

import argparse
import os
from pathlib import Path
import re
import signal
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
STACKS = {
    "langchain4j-agent-workflow": ROOT / "target/talk-sample",
    "langchain4j-agent-workflow-secured": ROOT / "target/secured-sample",
}
JARS = {sample / module / "target/quarkus-app/quarkus-run.jar"
        for sample in STACKS.values() for module in ("text-agent", "weather-server", "orchestrator")}
SCRIPTS = {ROOT / "scripts" / name for name in ("workflow.py", "secured.py", "present.py")}
SCRIPTS.update(sample / "scripts/run.py" for sample in STACKS.values())
RUNNER = re.compile(r"^(?:-u +)?(.*/)?scripts/(workflow|secured|present|run)\.py(?: +([^ ]+).*)?$")


def working_directory(pid):
    result = subprocess.run(["lsof", "-a", "-p", str(pid), "-d", "cwd", "-Fn"],
                            capture_output=True, text=True)
    return next((Path(line[1:]) for line in result.stdout.splitlines() if line.startswith("n")), None)


def is_demo(pid, command):
    executable, _, args = command.partition(" ")
    executable = Path(executable).name.lower()
    args = args.lstrip()
    if executable == "java":
        _, separator, jar = f" {args}".partition(" -jar ")
        return bool(separator) and Path(jar) in JARS
    if not re.fullmatch(r"python(?:\d+(?:\.\d+)?)?", executable):
        return False
    match = RUNNER.fullmatch(args)
    if not match or match[3] in ("prepare", "test"):
        return False
    script = Path(f"{match[1] or ''}scripts/{match[2]}.py")
    if not script.is_absolute():
        cwd = working_directory(pid)
        if cwd is None:
            return False
        script = cwd / script
    return script.resolve() in SCRIPTS


def demo_processes():
    listing = subprocess.check_output(
        ["ps", "-ww", "-U", str(os.getuid()), "-o", "pid=", "-o", "command="], text=True)
    matches = {}
    for line in listing.splitlines():
        pid, _, command = line.strip().partition(" ")
        if command and is_demo(int(pid), command.lstrip()):
            matches[int(pid)] = command.lstrip()
    return matches


def stop_processes(dry_run):
    processes = demo_processes()
    for pid, command in processes.items():
        print(f"{'Would stop' if dry_run else 'Stopping'} PID {pid}: {command}", flush=True)
        if not dry_run:
            signalled(pid, signal.SIGTERM)
    if dry_run or not processes:
        return
    deadline = time.monotonic() + 10
    while time.monotonic() < deadline:
        remaining = demo_processes()
        if not remaining:
            return
        time.sleep(0.25)
    for pid in demo_processes():
        print(f"PID {pid} did not exit; sending SIGKILL.", flush=True)
        signalled(pid, signal.SIGKILL)


def signalled(pid, sig):
    try:
        os.kill(pid, sig)
    except ProcessLookupError:
        pass


def stop_stack(project, directory, dry_run):
    containers = subprocess.check_output(
        ["docker", "ps", "-aq", "--filter", f"label=com.docker.compose.project={project}"],
        text=True).split()
    if containers:
        owners = subprocess.check_output(
            ["docker", "inspect", "--format", '{{index .Config.Labels "com.docker.compose.project.working_dir"}}',
             *containers], text=True).splitlines()
        if any(Path(owner).resolve() != directory for owner in owners):
            raise RuntimeError(f"Not stopping {project}: its containers belong to another checkout.")
    compose = directory / "compose.yaml"
    if not compose.is_file():
        if containers:
            raise RuntimeError(f"Cannot stop {project}: missing {compose}.")
        return
    print(f"{'Would tear down' if dry_run else 'Tearing down'} {project}.", flush=True)
    if not dry_run:
        subprocess.run(["docker", "compose", "-p", project, "-f", str(compose), "down"], check=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true", help="list what would be stopped")
    args = parser.parse_args()
    actions = [lambda: stop_processes(args.dry_run)]
    actions.extend(lambda p=p, d=d: stop_stack(p, d, args.dry_run) for p, d in STACKS.items())
    failed = False
    for action in actions:
        try:
            action()
        except (OSError, RuntimeError, subprocess.CalledProcessError) as error:
            print(error, file=sys.stderr)
            failed = True
    if not failed:
        print("Preview only. Host Ollama is left running." if args.dry_run else
              "Demo stopped. Host Ollama is left running.")
    return int(failed)


if __name__ == "__main__":
    sys.exit(main())
