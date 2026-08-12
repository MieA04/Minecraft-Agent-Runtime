#!/usr/bin/env python3
"""Wait for MAR state and issue eval or installation smoke requests."""

from __future__ import annotations

import argparse
import json
import socket
import sys
import time
from pathlib import Path
from typing import Any


class VerificationError(RuntimeError):
    pass


def read_ready_state(path: Path) -> dict[str, Any]:
    state = json.loads(path.read_text(encoding="utf-8"))
    if state.get("schema") != 1 or state.get("status") != "ready":
        raise VerificationError("Runtime state is not schema=1 ready")
    if state.get("host") != "127.0.0.1" or not isinstance(state.get("port"), int):
        raise VerificationError("Runtime state has invalid loopback host/port")
    return state


def connect_ready(state_path: Path, wait_seconds: float) -> tuple[socket.socket, dict[str, Any]]:
    deadline = time.monotonic() + wait_seconds
    last_error: Exception | None = None
    while time.monotonic() <= deadline:
        try:
            state = read_ready_state(state_path)
            connection = socket.create_connection((state["host"], state["port"]), timeout=2)
            return connection, state
        except (OSError, ValueError, json.JSONDecodeError, VerificationError) as error:
            last_error = error
            time.sleep(0.25)
    raise VerificationError(f"Cannot connect to ready Runtime: {last_error}")


def request(stream, request_id: str, session: str, method: str, code: str) -> dict[str, Any]:
    payload = {"id": request_id, "session": session, "method": method, "code": code}
    stream.write((json.dumps(payload, ensure_ascii=False) + "\n").encode("utf-8"))
    stream.flush()
    line = stream.readline()
    if not line:
        raise VerificationError("Runtime closed the connection before responding")
    response = json.loads(line.decode("utf-8"))
    if response.get("id") != request_id:
        raise VerificationError("Runtime response id does not match request")
    return response


def execute(args: argparse.Namespace) -> dict[str, Any]:
    connection, state = connect_ready(Path(args.state), args.wait)
    try:
        stream = connection.makefile("rwb")
        if args.smoke_mod_class:
            arithmetic = request(stream, "smoke-arithmetic", args.session, "eval.raw", "1 + 2")
            class_literal = json.dumps(args.smoke_mod_class)
            class_code = (
                f"Class.forName({class_literal}, false, "
                "Thread.currentThread().contextClassLoader).name")
            mod_class = request(stream, "smoke-mod-class", args.session, "eval.raw", class_code)
            if not arithmetic.get("ok") or arithmetic.get("result") != 3:
                raise VerificationError(f"Arithmetic smoke failed: {arithmetic}")
            if not mod_class.get("ok") or mod_class.get("result") != args.smoke_mod_class:
                raise VerificationError(f"Current Mod class smoke failed: {mod_class}")
            return {
                "ok": True,
                "pid": state.get("pid"),
                "host": state["host"],
                "port": state["port"],
                "arithmetic": arithmetic["result"],
                "modClass": mod_class["result"],
            }
        response = request(stream, "eval-1", args.session, args.method, args.code)
        return {"ok": bool(response.get("ok")), "state": state, "response": response}
    finally:
        connection.close()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--state", required=True)
    parser.add_argument("--wait", type=float, default=60.0)
    parser.add_argument("--session", default="installer-smoke")
    parser.add_argument("--method", choices=("eval.raw", "eval.client", "eval.server"), default="eval.raw")
    parser.add_argument("--code", default="1 + 2")
    parser.add_argument("--smoke-mod-class")
    args = parser.parse_args()
    try:
        result = execute(args)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return 0 if result.get("ok") else 1
    except (VerificationError, OSError, ValueError, json.JSONDecodeError) as error:
        print(json.dumps({
            "ok": False,
            "errorType": error.__class__.__name__,
            "message": str(error),
        }, ensure_ascii=False, indent=2), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
