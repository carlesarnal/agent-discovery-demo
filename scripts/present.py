#!/usr/bin/env python3
"""Serve the Paris deck locally with the live briefing and two fixed demo commands."""

import argparse
from http.client import HTTPConnection, HTTPException
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
from pathlib import Path
import subprocess
import sys
from threading import Lock
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[1]
DECK = ROOT / "talks/cloud-native-paris-2026/presentation.html"
HOST = "127.0.0.1:8765"
ORIGIN = f"http://{HOST}"
RUN_LOCK = Lock()
RUNNERS = {"default": "workflow.py", "secured": "secured.py"}


class Presenter(BaseHTTPRequestHandler):
    def end_headers(self):
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("X-Frame-Options", "DENY")
        super().end_headers()

    def reply(self, content_type):
        self.send_response(200)
        self.send_header("Content-Type", content_type)
        self.end_headers()

    def do_GET(self):
        if self.headers.get("Host") != HOST:
            self.send_error(403)
            return
        path = urlsplit(self.path).path
        if path == "/":
            self.send_response(302)
            self.send_header("Location", f"/presentation.html?mode={self.server.mode}")
            self.end_headers()
        elif path == "/presentation.html":
            self.reply("text/html; charset=utf-8")
            self.wfile.write(DECK.read_bytes())
        elif path == f"/presenter/{self.server.mode}":
            self.reply("text/plain; charset=utf-8")
            self.wfile.write(b"ready")
        else:
            self.send_error(404)

    def do_POST(self):
        if (self.headers.get("Host") != HOST or self.headers.get("Origin") != ORIGIN
                or self.headers.get("X-Demo-Presenter") != "1"):
            self.send_error(403)
            return
        mode = self.server.mode
        actions = {f"/presenter/{mode}/{action}": action for action in ("briefing", "governance", "tools")}
        action = actions.get(self.path)
        if action is None:
            self.send_error(404)
            return
        if not RUN_LOCK.acquire(blocking=False):
            self.send_error(409, "Another demo command is running")
            return
        try:
            if action == "briefing":
                self.briefing()
            else:
                self.stream_command(mode, action)
        finally:
            RUN_LOCK.release()

    def briefing(self):
        # Only forward briefing input to the fixed, local workflow endpoint.
        try:
            size = int(self.headers.get("Content-Length", "0"))
            if not 0 < size <= 64 * 1024:
                raise ValueError()
            data = json.loads(self.rfile.read(size))
            city, context = data["city"], data["context"]
            if (city not in ("Amsterdam", "Paris", "Madrid") or not isinstance(context, str)
                    or not context.strip() or len(context) > 8000):
                raise ValueError()
        except (ValueError, KeyError, TypeError):
            self.send_error(400, "Expected a city and briefing context")
            return
        connection = HTTPConnection("localhost", 11020, timeout=120)
        try:
            connection.request("POST", "/workflow/briefing", json.dumps({"city": city, "context": context}),
                               {"Content-Type": "application/json"})
            upstream = connection.getresponse()
            status, body = upstream.status, upstream.read(1024 * 1024 + 1)
        except (OSError, HTTPException):
            self.send_error(502, "Briefing unavailable; check the running demo")
            return
        finally:
            connection.close()
        if len(body) > 1024 * 1024:
            self.send_error(502, "Briefing response too large")
            return
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.end_headers()
        self.wfile.write(body)

    def stream_command(self, mode, action):
        def emit(**event):
            self.wfile.write((json.dumps(event) + "\n").encode("utf-8"))
            self.wfile.flush()

        try:
            process = subprocess.Popen(
                [sys.executable, "-u", str(ROOT / "scripts" / RUNNERS[mode]), action],
                cwd=ROOT, stdin=subprocess.DEVNULL, stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace",
            )
        except OSError:
            self.send_error(500, "Could not start the demo command")
            return
        with process:
            try:
                self.reply("application/x-ndjson; charset=utf-8")
                for line in process.stdout:
                    emit(output=line)
                emit(exit=process.wait())
            except (BrokenPipeError, ConnectionResetError):
                # The runner has its own request timeouts; do not interrupt a Registry update
                # just because the presenter refreshed the page. Drain output before waiting.
                for _ in process.stdout:
                    pass


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mode", choices=RUNNERS, default="default")
    args = parser.parse_args()
    with ThreadingHTTPServer(("127.0.0.1", 8765), Presenter) as server:
        server.mode = args.mode
        print(f"Open {ORIGIN}/ for the {args.mode} deck. Ctrl+C stops the presenter.", flush=True)
        try:
            server.serve_forever()
        except KeyboardInterrupt:
            pass


if __name__ == "__main__":
    main()
