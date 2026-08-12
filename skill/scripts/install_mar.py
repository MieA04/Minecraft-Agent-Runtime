#!/usr/bin/env python3
"""Install the MAR V0 runtime into a supported NeoForge Gradle project."""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path


MAR_BEGIN = "// MAR-BEGIN: managed runtime integration"
MAR_END = "// MAR-END: managed runtime integration"
RUNTIME_SOURCE = ".minecraft-agent-runtime/runtime/bootstrap/src/main/java"


class InstallerError(RuntimeError):
    pass


@dataclass(frozen=True)
class ProjectInfo:
    root: Path
    build_file: Path
    entrypoint: Path
    entrypoint_class: str
    minecraft_version: str
    loader: str
    loader_version: str
    java_version: int
    run_task: str


def parse_properties(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.is_file():
        return values
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def detect_entrypoint(root: Path, explicit: str | None) -> tuple[Path, str]:
    source_root = root / "src" / "main" / "java"
    if explicit:
        candidate = (root / explicit).resolve()
        if not candidate.is_relative_to(root) or not candidate.is_file():
            raise InstallerError(f"Entrypoint is not a file inside the project: {explicit}")
        candidates = [candidate]
    else:
        candidates = [
            path for path in source_root.rglob("*.java")
            if re.search(r"(?m)^\s*@Mod\s*\(", path.read_text(encoding="utf-8"))
        ]
    if len(candidates) != 1:
        raise InstallerError(
            f"Expected exactly one @Mod entrypoint, found {len(candidates)}; use --entrypoint")

    entrypoint = candidates[0]
    source = entrypoint.read_text(encoding="utf-8")
    package = re.search(r"(?m)^\s*package\s+([\w.]+)\s*;", source)
    class_match = re.search(r"(?m)^\s*public\s+(?:final\s+)?class\s+(\w+)", source)
    if not class_match:
        raise InstallerError(f"Cannot detect public Mod class in {entrypoint}")
    simple_name = class_match.group(1)
    fqcn = f"{package.group(1)}.{simple_name}" if package else simple_name
    return entrypoint, fqcn


def detect_project(root: Path, explicit_entrypoint: str | None = None) -> ProjectInfo:
    root = root.resolve()
    if not root.is_dir():
        raise InstallerError(f"Project root does not exist: {root}")
    build_file = root / "build.gradle"
    if not build_file.is_file():
        if (root / "build.gradle.kts").exists():
            raise InstallerError("Gradle Kotlin DSL is not supported by the V0 installer adapter")
        raise InstallerError("Cannot find supported build.gradle")

    build = build_file.read_text(encoding="utf-8")
    if "net.neoforged.moddev" not in build or "neoForge" not in build:
        raise InstallerError("Only NeoForge ModDevGradle projects are supported by this V0 adapter")
    properties = parse_properties(root / "gradle.properties")
    minecraft_version = properties.get("minecraft_version")
    loader_version = properties.get("neo_version")
    java_match = re.search(r"JavaLanguageVersion\.of\((\d+)\)", build)
    if not minecraft_version or not loader_version or not java_match:
        raise InstallerError("Cannot confirm Minecraft, NeoForge, and Java versions from project files")
    java_version = int(java_match.group(1))

    entrypoint, entrypoint_class = detect_entrypoint(root, explicit_entrypoint)
    return ProjectInfo(
        root=root,
        build_file=build_file,
        entrypoint=entrypoint,
        entrypoint_class=entrypoint_class,
        minecraft_version=minecraft_version,
        loader="NeoForge",
        loader_version=loader_version,
        java_version=java_version,
        run_task="runClient",
    )


def atomic_write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(prefix=f".{path.name}.", dir=path.parent)
    temporary = Path(temporary_name)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8", newline="") as stream:
            stream.write(content)
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temporary, path)
    finally:
        if temporary.exists():
            temporary.unlink()


def copy_missing_tree(source: Path, destination: Path) -> list[str]:
    created: list[str] = []
    for path in sorted(source.rglob("*")):
        relative = path.relative_to(source)
        target = destination / relative
        if path.is_dir():
            target.mkdir(parents=True, exist_ok=True)
        elif not target.exists():
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(path, target)
            created.append(target.as_posix())
    return created


def copy_managed_runtime(source: Path, destination: Path) -> list[str]:
    copied: list[str] = []
    for path in sorted(source.rglob("*")):
        relative = path.relative_to(source)
        target = destination / relative
        if path.is_dir():
            target.mkdir(parents=True, exist_ok=True)
        else:
            target.parent.mkdir(parents=True, exist_ok=True)
            if not target.exists() or path.read_bytes() != target.read_bytes():
                shutil.copy2(path, target)
                copied.append(target.as_posix())
    return copied


def managed_gradle_block(dependencies: dict[str, str]) -> str:
    implementation = "\n".join(
        f"    implementation '{coordinate}:{version}'"
        for coordinate, version in dependencies.items()
    )
    runtime = "\n".join(
        "        additionalRuntimeClasspathConfiguration.dependencies.add(\n"
        f"                project.dependencies.create('{coordinate}:{version}'))"
        for coordinate, version in dependencies.items()
    )
    return f"""{MAR_BEGIN}
sourceSets.main.java.srcDir '{RUNTIME_SOURCE}'

dependencies {{
{implementation}
}}

neoForge {{
    runs.configureEach {{
{runtime}
        systemProperty 'mar.projectRoot', project.projectDir.absolutePath
    }}
}}
{MAR_END}"""


def update_build_file(path: Path, dependencies: dict[str, str], dry_run: bool) -> bool:
    source = path.read_text(encoding="utf-8")
    block = managed_gradle_block(dependencies)
    marker_pattern = re.compile(
        re.escape(MAR_BEGIN) + r".*?" + re.escape(MAR_END), re.DOTALL)
    if marker_pattern.search(source):
        updated = marker_pattern.sub(block, source)
    else:
        updated = source.rstrip() + "\n\n" + block + "\n"
    if updated == source:
        return False
    if not dry_run:
        atomic_write(path, updated)
    return True


def update_entrypoint(path: Path, fqcn: str, dry_run: bool) -> bool:
    source = path.read_text(encoding="utf-8")
    if "MinecraftAgentRuntime.start();" in source:
        return False

    import_line = "import mar.runtime.bootstrap.MinecraftAgentRuntime;"
    updated = source
    if import_line not in updated:
        package_match = re.search(r"(?m)^\s*package\s+[\w.]+\s*;\s*\r?\n", updated)
        if package_match:
            updated = updated[:package_match.end()] + "\n" + import_line + "\n" + updated[package_match.end():]
        else:
            updated = import_line + "\n\n" + updated

    simple_name = fqcn.rsplit(".", 1)[-1]
    constructor_start = re.search(rf"(?m)^([ \t]*)public\s+{re.escape(simple_name)}\s*\(", updated)
    if not constructor_start:
        raise InstallerError(f"Cannot find public constructor for {fqcn}")
    brace = updated.find("{", constructor_start.end())
    if brace < 0:
        raise InstallerError(f"Cannot find constructor body for {fqcn}")
    line_start = updated.rfind("\n", 0, constructor_start.start()) + 1
    indent = re.match(r"[ \t]*", updated[line_start:constructor_start.start()]).group(0) + "    "
    insertion = f"\n{indent}// MAR Runtime start hook\n{indent}MinecraftAgentRuntime.start();\n"
    updated = updated[:brace + 1] + insertion + updated[brace + 1:]
    if not dry_run:
        atomic_write(path, updated)
    return True


def install(args: argparse.Namespace) -> dict[str, object]:
    skill_root = Path(args.skill_root).resolve() if args.skill_root else Path(__file__).resolve().parents[1]
    project = detect_project(Path(args.project_root), args.entrypoint)
    manifest_path = skill_root / "assets" / "runtime-template" / "INSTALL-MANIFEST.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if project.java_version < int(manifest["minimumJava"]):
        raise InstallerError(
            f"Project Java {project.java_version} is below required {manifest['minimumJava']}")

    workspace_target = project.root / ".minecraft-agent-runtime"
    created_workspace: list[str] = []
    copied_runtime: list[str] = []
    if not args.dry_run:
        created_workspace = copy_missing_tree(
            skill_root / "assets" / "project-workspace", workspace_target)
        copied_runtime = copy_managed_runtime(
            skill_root / "assets" / "runtime-template",
            workspace_target / "runtime" / "bootstrap")

    build_changed = update_build_file(
        project.build_file, manifest["dependencies"], args.dry_run)
    entrypoint_changed = update_entrypoint(
        project.entrypoint, project.entrypoint_class, args.dry_run)
    return {
        "ok": True,
        "dryRun": bool(args.dry_run),
        "projectRoot": str(project.root),
        "buildSystem": "Gradle Groovy DSL",
        "minecraftVersion": project.minecraft_version,
        "loader": project.loader,
        "loaderVersion": project.loader_version,
        "javaVersion": project.java_version,
        "entrypoint": str(project.entrypoint),
        "entrypointClass": project.entrypoint_class,
        "runTask": project.run_task,
        "runtimeVersion": manifest["runtimeVersion"],
        "buildChanged": build_changed,
        "entrypointChanged": entrypoint_changed,
        "workspaceFilesCreated": len(created_workspace),
        "runtimeFilesCopied": len(copied_runtime),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project-root", required=True)
    parser.add_argument("--entrypoint", help="Project-relative Java entrypoint path")
    parser.add_argument("--skill-root", help=argparse.SUPPRESS)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    try:
        print(json.dumps(install(args), ensure_ascii=False, indent=2))
        return 0
    except (InstallerError, OSError, ValueError, json.JSONDecodeError) as error:
        print(json.dumps({
            "ok": False,
            "errorType": error.__class__.__name__,
            "message": str(error),
        }, ensure_ascii=False, indent=2), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
