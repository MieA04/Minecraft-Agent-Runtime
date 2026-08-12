from __future__ import annotations

import re
import unittest
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
SKILL_ROOT = REPOSITORY_ROOT / "skill"


class SkillAssetTest(unittest.TestCase):
    def test_skill_frontmatter_links_and_interface_are_complete(self) -> None:
        skill = (SKILL_ROOT / "SKILL.md").read_text(encoding="utf-8")
        self.assertTrue(skill.startswith("---\nname: minecraft-agent-runtime\n"))
        self.assertIn("description:", skill.split("---", 2)[1])
        for link in re.findall(r"\[[^]]+]\(([^)]+)\)", skill):
            self.assertTrue((SKILL_ROOT / link).is_file(), link)

        interface = (SKILL_ROOT / "agents/openai.yaml").read_text(encoding="utf-8")
        self.assertIn("$minecraft-agent-runtime", interface)
        self.assertIn('display_name: "Minecraft Agent Runtime"', interface)

    def test_workspace_has_all_domains_and_non_fabricating_templates(self) -> None:
        workspace = SKILL_ROOT / "assets/project-workspace"
        required = (
            "runtime/config/runtime.json",
            "tools/minecraft/.gitkeep", "tools/loader/.gitkeep",
            "tools/mod/.gitkeep", "tools/experimental/.gitkeep",
            "knowledge/project.md", "knowledge/minecraft.md", "knowledge/loader.md",
            "knowledge/discoveries/README.md", "knowledge/discoveries/TEMPLATE.md",
            "logs/sessions/.gitkeep", "logs/experiments/.gitkeep", "state/.gitkeep",
        )
        for relative in required:
            self.assertTrue((workspace / relative).is_file(), relative)

        combined = "\n".join(
            (workspace / relative).read_text(encoding="utf-8")
            for relative in ("knowledge/project.md", "knowledge/minecraft.md", "knowledge/loader.md"))
        self.assertNotIn("elementalrunes", combined.lower())
        self.assertNotIn("org.miea", combined.lower())
        self.assertIn("Last verified:", combined)

    def test_workflow_contains_discovery_tool_and_promotion_contracts(self) -> None:
        workflow = (SKILL_ROOT / "references/knowledge-workflow.md").read_text(encoding="utf-8")
        checklist = (SKILL_ROOT / "references/promotion-checklist.md").read_text(encoding="utf-8")
        for field in (
                "Path:", "Status: project-verified", "Minecraft:", "Loader:",
                "Depends-On-Project-Code:", "Evidence:"):
            self.assertIn(field, workflow)
        self.assertGreaterEqual(checklist.count("- [ ]"), 10)
        self.assertIn("independent second environment", checklist)
        self.assertIn("separate process", checklist)

    def test_public_tools_are_only_minecraft_or_loader_and_have_no_private_identifiers(self) -> None:
        public_root = SKILL_ROOT / "tools"
        tools = list(public_root.rglob("*.groovy"))
        self.assertTrue(tools)
        for tool in tools:
            self.assertIn(tool.relative_to(public_root).parts[0], {"minecraft", "loader"})
            source = tool.read_text(encoding="utf-8")
            self.assertIn("MAR Public Tool", source)
            self.assertIn("Project Dependencies: none", source)
            self.assertNotRegex(source.lower(), r"elementalrunes|org\.miea|rune")

    def test_markdown_assets_do_not_contain_known_mojibake_markers(self) -> None:
        bad = ("鈥", "涓", "榛", "锛", "銆", "歚", "鎭")
        for path in SKILL_ROOT.rglob("*.md"):
            source = path.read_text(encoding="utf-8")
            self.assertFalse(any(marker in source for marker in bad), str(path))


if __name__ == "__main__":
    unittest.main()
