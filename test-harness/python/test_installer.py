from __future__ import annotations

import argparse
import json
import socket
import sys
import tempfile
import threading
import unittest
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPOSITORY_ROOT / "skill" / "scripts"))

import install_mar
import mar_rpc


BUILD_GRADLE = """plugins {
    id 'java-library'
    id 'net.neoforged.moddev' version '2.0.143'
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

dependencies {
}

neoForge {
    version = project.neo_version
    runs {
        client { client() }
    }
}
"""

ENTRYPOINT = """package example.mod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(ExampleMod.MODID)
public class ExampleMod {
    public static final String MODID = "example";

    public ExampleMod(IEventBus eventBus) {
        eventBus.toString();
    }
}
"""


class InstallerTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        (self.root / "src/main/java/example/mod").mkdir(parents=True)
        (self.root / "build.gradle").write_text(BUILD_GRADLE, encoding="utf-8")
        (self.root / "gradle.properties").write_text(
            "minecraft_version=1.21.1\nneo_version=21.1.248\n", encoding="utf-8")
        (self.root / "src/main/java/example/mod/ExampleMod.java").write_text(
            ENTRYPOINT, encoding="utf-8")

    def tearDown(self) -> None:
        self.temporary.cleanup()

    def arguments(self, **overrides):
        values = {
            "project_root": str(self.root),
            "entrypoint": None,
            "skill_root": str(REPOSITORY_ROOT / "skill"),
            "dry_run": False,
        }
        values.update(overrides)
        return argparse.Namespace(**values)

    def test_install_is_complete_idempotent_and_preserves_project_files(self) -> None:
        existing = self.root / ".minecraft-agent-runtime/knowledge/project.md"
        existing.parent.mkdir(parents=True)
        existing.write_text("USER KNOWLEDGE\n", encoding="utf-8")
        project_tool = self.root / ".minecraft-agent-runtime/tools/mod/existing.groovy"
        project_tool.parent.mkdir(parents=True)
        project_tool.write_text("return [kept: true]\n", encoding="utf-8")

        first = install_mar.install(self.arguments())
        second = install_mar.install(self.arguments())

        self.assertTrue(first["ok"])
        self.assertEqual("example.mod.ExampleMod", first["entrypointClass"])
        self.assertFalse(second["buildChanged"])
        self.assertFalse(second["entrypointChanged"])
        self.assertEqual(0, second["workspaceFilesCreated"])
        self.assertEqual(0, second["runtimeFilesCopied"])
        build = (self.root / "build.gradle").read_text(encoding="utf-8")
        entrypoint = (self.root / "src/main/java/example/mod/ExampleMod.java").read_text(
            encoding="utf-8")
        self.assertEqual(1, build.count(install_mar.MAR_BEGIN))
        self.assertEqual(1, build.count(install_mar.RUNTIME_SOURCE))
        self.assertEqual(1, entrypoint.count("MinecraftAgentRuntime.start();"))
        self.assertEqual("USER KNOWLEDGE\n", existing.read_text(encoding="utf-8"))
        self.assertEqual("return [kept: true]\n", project_tool.read_text(encoding="utf-8"))
        self.assertTrue((self.root / ".minecraft-agent-runtime/runtime/bootstrap/INSTALL-MANIFEST.json").is_file())
        self.assertTrue((self.root / ".minecraft-agent-runtime/runtime/config/runtime.json").is_file())

    def test_dry_run_detects_without_modifying_project(self) -> None:
        result = install_mar.install(self.arguments(dry_run=True))

        self.assertTrue(result["dryRun"])
        self.assertTrue(result["buildChanged"])
        self.assertTrue(result["entrypointChanged"])
        self.assertNotIn(install_mar.MAR_BEGIN, (self.root / "build.gradle").read_text(encoding="utf-8"))
        self.assertFalse((self.root / ".minecraft-agent-runtime").exists())

    def test_unsupported_kotlin_dsl_fails_without_writes(self) -> None:
        (self.root / "build.gradle").unlink()
        (self.root / "build.gradle.kts").write_text("plugins {}", encoding="utf-8")

        with self.assertRaises(install_mar.InstallerError):
            install_mar.install(self.arguments())

        self.assertFalse((self.root / ".minecraft-agent-runtime").exists())


class RpcSmokeTest(unittest.TestCase):
    def test_smoke_uses_state_and_checks_arithmetic_and_mod_class(self) -> None:
        expected_class = "example.mod.ExampleMod"
        listener = socket.socket()
        listener.bind(("127.0.0.1", 0))
        listener.listen(1)
        port = listener.getsockname()[1]
        requests: list[dict] = []

        def serve() -> None:
            connection, _ = listener.accept()
            with connection, connection.makefile("rwb") as stream:
                for result in (3, expected_class):
                    request = json.loads(stream.readline().decode("utf-8"))
                    requests.append(request)
                    response = {
                        "id": request["id"], "ok": True, "result": result,
                        "stdout": "", "stderr": "",
                    }
                    stream.write((json.dumps(response) + "\n").encode("utf-8"))
                    stream.flush()
            listener.close()

        worker = threading.Thread(target=serve)
        worker.start()
        with tempfile.TemporaryDirectory() as directory:
            state = Path(directory) / "runtime.json"
            state.write_text(json.dumps({
                "schema": 1, "status": "ready", "pid": 7,
                "host": "127.0.0.1", "port": port,
            }), encoding="utf-8")
            result = mar_rpc.execute(argparse.Namespace(
                state=str(state), wait=2.0, session="smoke",
                method="eval.raw", code="1 + 2", smoke_mod_class=expected_class))
        worker.join(timeout=2)

        self.assertTrue(result["ok"])
        self.assertEqual(3, result["arithmetic"])
        self.assertEqual(expected_class, result["modClass"])
        self.assertEqual(["smoke-arithmetic", "smoke-mod-class"],
                         [request["id"] for request in requests])

    def test_stale_state_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            state = Path(directory) / "runtime.json"
            state.write_text(json.dumps({
                "schema": 1, "status": "ready", "pid": 8,
                "host": "127.0.0.1", "port": 1,
            }), encoding="utf-8")
            with self.assertRaises(mar_rpc.VerificationError):
                mar_rpc.connect_ready(state, 0.1)


if __name__ == "__main__":
    unittest.main()
