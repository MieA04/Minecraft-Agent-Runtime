#!/usr/bin/env python3
"""Run repeatable MAR V0 acceptance checks against a live Minecraft Runtime."""

from __future__ import annotations

import argparse
import json
import os
import socket
import sys
import tempfile
from pathlib import Path
from typing import Any

import mar_rpc


class AcceptanceError(RuntimeError):
    pass


class LiveAcceptance:
    def __init__(self, stream, mod_class: str, third_party_class: str | None):
        self.stream = stream
        self.mod_class = mod_class
        self.third_party_class = third_party_class
        self.sequence = 0
        self.passed: list[str] = []

    def eval(self, code: str, session: str = "acceptance", method: str = "eval.raw") -> dict[str, Any]:
        self.sequence += 1
        return mar_rpc.request(
            self.stream, f"accept-{self.sequence}", session, method, code)

    @staticmethod
    def require(condition: bool, message: str) -> None:
        if not condition:
            raise AcceptanceError(message)

    def require_ok(self, response: dict[str, Any], message: str) -> Any:
        self.require(bool(response.get("ok")), f"{message}: {response}")
        return response.get("result")

    def repeat_eval(self) -> None:
        for index in range(100):
            result = self.require_ok(self.eval("1 + 2", "accept-repeat"), f"repeat eval {index}")
            self.require(result == 3, f"repeat eval {index} returned {result!r}")
        self.passed.append("A-003 repeat eval x100")

    def classloader(self) -> None:
        classes = [
            "java.lang.String",
            "net.minecraft.client.Minecraft",
            "net.neoforged.neoforge.common.NeoForge",
            self.mod_class,
        ]
        if self.third_party_class:
            classes.append(self.third_party_class)
        literals = ", ".join(json.dumps(name) for name in classes)
        code = (
            f"[{literals}].collect {{ Class.forName(it, false, "
            "Thread.currentThread().contextClassLoader).name }")
        result = self.require_ok(self.eval(code), "ClassLoader visibility")
        self.require(result == classes, f"ClassLoader result mismatch: {result!r}")
        self.passed.append("A-004 live ClassLoader")

    def sessions(self) -> None:
        self.require_ok(self.eval("vars.value = 41", "accept-a"), "set session A")
        value = self.require_ok(self.eval("vars.value + 1", "accept-a"), "read session A")
        self.require(value == 42, f"Session persistence returned {value!r}")
        isolated = self.require_ok(
            self.eval("vars.properties.containsKey('value')", "accept-b"), "read session B")
        self.require(isolated is False, f"Session B was polluted: {isolated!r}")
        self.passed.extend(("A-005 session persistence", "A-006 session isolation"))

    def handles_and_results(self) -> None:
        descriptor = self.require_ok(
            self.eval("vars.identity = new Object(); vars.identity", "accept-handle"), "create handle")
        self.require(isinstance(descriptor, dict) and descriptor.get("kind") == "handle",
                     f"Missing handle descriptor: {descriptor!r}")
        handle = descriptor["handle"]
        same = self.require_ok(
            self.eval(f"ref({json.dumps(handle)}).is(vars.identity)", "accept-handle"), "restore handle")
        self.require(same is True, "Handle did not restore the same identity")

        simple = self.require_ok(self.eval(
            "[nullValue:null, bool:true, number:42, text:'ok', list:[1,'x'], map:[a:1]]"),
            "simple result")
        self.require(simple == {
            "nullValue": None, "bool": True, "number": 42, "text": "ok",
            "list": [1, "x"], "map": {"a": 1},
        }, f"Simple result mismatch: {simple!r}")
        cycle = self.require_ok(self.eval("def cycle=[]; cycle << cycle; cycle"), "cycle result")
        self.require(isinstance(cycle, dict) and cycle.get("kind") == "handle",
                     f"Cycle was not bridged as handle: {cycle!r}")
        self.passed.extend(("A-007 handle identity", "A-008 simple result", "A-009 cycle safety"))

    def output_and_exception(self) -> None:
        output = self.eval("print 'out-ok'; System.err.print('err-ok'); 7")
        self.require_ok(output, "output eval")
        self.require(output.get("stdout") == "out-ok" and output.get("stderr") == "err-ok",
                     f"Output mismatch: {output}")
        failed = self.eval("print 'before-fail'; throw new IllegalStateException('accept-boom')")
        self.require(failed.get("ok") is False, f"Exception unexpectedly succeeded: {failed}")
        error = failed.get("error", {})
        self.require(error.get("type") == "java.lang.IllegalStateException"
                     and "accept-boom" in error.get("stack", "")
                     and failed.get("stdout") == "before-fail", f"Exception shape mismatch: {failed}")
        recovered = self.require_ok(self.eval("6 * 7"), "post-exception recovery")
        self.require(recovered == 42, f"Runtime did not recover: {recovered!r}")
        self.passed.extend(("A-010 output", "A-011 exception recovery"))

    def client_target(self) -> None:
        result = self.require_ok(self.eval(
            "[thread:Thread.currentThread().name, same:mc.isSameThread()]",
            method="eval.client"), "client target")
        self.require(result.get("thread") == "Render thread" and result.get("same") is True,
                     f"Client thread mismatch: {result!r}")
        self.passed.append("A-012 client thread")

    def server_target(self, expectation: str) -> None:
        response = self.eval(
            "def s=mc.getSingleplayerServer(); [thread:Thread.currentThread().name, same:s.isSameThread()]",
            method="eval.server")
        if expectation == "available":
            result = self.require_ok(response, "server target")
            self.require(result.get("thread") == "Server thread" and result.get("same") is True,
                         f"Server thread mismatch: {result!r}")
            self.passed.append("A-013 server thread")
        else:
            self.require(response.get("ok") is False
                         and response.get("error", {}).get("code") == "TARGET_UNAVAILABLE",
                         f"Server should be unavailable: {response}")
            self.passed.append("A-014 server unavailable")


def atomic_write(path: Path, source: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(prefix=f".{path.name}.", dir=path.parent)
    temporary = Path(temporary_name)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8", newline="") as stream:
            stream.write(source)
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temporary, path)
    finally:
        if temporary.exists():
            temporary.unlink()


def hot_reload(acceptance: LiveAcceptance, project_root: Path) -> None:
    path = project_root / ".minecraft-agent-runtime/tools/mod/mar-acceptance-hot.groovy"
    existed = path.exists()
    original = path.read_bytes() if existed else None
    try:
        atomic_write(path, "return [version: { -> 'v1' }]\n")
        first = acceptance.require_ok(acceptance.eval(
            "def r=runtime.tools.reloadPath('mod/mar-acceptance-hot.groovy'); "
            "[reload:r, value:vars.tools.mod.'mar-acceptance-hot'.version()]"), "tool v1")
        acceptance.require(first["reload"]["ok"] is True and first["value"] == "v1",
                           f"Tool v1 mismatch: {first!r}")

        atomic_write(path, "return [version: { -> 'v2' }]\n")
        second = acceptance.require_ok(acceptance.eval(
            "def r=runtime.tools.reloadPath('mod/mar-acceptance-hot.groovy'); "
            "[reload:r, value:vars.tools.mod.'mar-acceptance-hot'.version()]"), "tool v2")
        acceptance.require(second["reload"]["ok"] is True and second["value"] == "v2",
                           f"Tool v2 mismatch: {second!r}")

        atomic_write(path, "return [version:\n")
        broken = acceptance.require_ok(acceptance.eval(
            "def r=runtime.tools.reloadPath('mod/mar-acceptance-hot.groovy'); "
            "[ok:r.ok, phase:r.phase, retained:vars.tools.mod.'mar-acceptance-hot'.version()]"),
            "broken tool")
        acceptance.require(
            broken == {"ok": False, "phase": "compile", "retained": "v2"},
            f"Broken Tool atomicity mismatch: {broken!r}")
        acceptance.passed.extend(("A-015 hot reload", "A-016 broken Tool atomicity"))
    finally:
        if existed:
            path.write_bytes(original)
        elif path.exists():
            path.unlink()


def run(args: argparse.Namespace) -> dict[str, Any]:
    connection, state = mar_rpc.connect_ready(Path(args.state), args.wait)
    try:
        stream = connection.makefile("rwb")
        acceptance = LiveAcceptance(stream, args.mod_class, args.third_party_class)
        acceptance.repeat_eval()
        acceptance.classloader()
        acceptance.sessions()
        acceptance.handles_and_results()
        acceptance.output_and_exception()
        acceptance.client_target()
        acceptance.server_target(args.server)
        if not args.skip_tool_reload:
            hot_reload(acceptance, Path(args.project_root).resolve())
        return {
            "ok": True,
            "pid": state.get("pid"),
            "serverExpectation": args.server,
            "passed": acceptance.passed,
        }
    finally:
        connection.close()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project-root", required=True)
    parser.add_argument("--state", required=True)
    parser.add_argument("--mod-class", required=True)
    parser.add_argument("--third-party-class")
    parser.add_argument("--server", choices=("available", "unavailable"), required=True)
    parser.add_argument("--wait", type=float, default=60.0)
    parser.add_argument("--skip-tool-reload", action="store_true")
    args = parser.parse_args()
    try:
        print(json.dumps(run(args), ensure_ascii=False, indent=2))
        return 0
    except (AcceptanceError, mar_rpc.VerificationError, OSError, ValueError, json.JSONDecodeError) as error:
        print(json.dumps({
            "ok": False,
            "errorType": error.__class__.__name__,
            "message": str(error),
        }, ensure_ascii=False, indent=2), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
