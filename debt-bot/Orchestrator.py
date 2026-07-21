#!/usr/bin/env python3
"""
Orchestrator for Phase 1 (manual trial).
Reads next pending task from Tasks.md, generates code using Claude, shows diff for human approval.
"""

import re
import sys
from datetime import datetime
from pathlib import Path

from AiClient import AiClient
from GitClient import GitClient
from TestRunner import TestRunner


class Phase1Orchestrator:
    def __init__(self, project_root: Path):
        self.project_root = Path(project_root)
        self.debt_bot_dir = self.project_root / "debt-bot"
        self.tasks_file = self.debt_bot_dir / "Tasks.md"
        self.status_file = self.debt_bot_dir / "Status.md"
        self.prompts_dir = self.debt_bot_dir / "prompts"

        self.test_runner = TestRunner(self.project_root)
        self.ai_client = AiClient()
        self.git_client = GitClient("markoniemi", "dynamic-form", self.project_root)

    def find_next_pending_task(self) -> dict:
        """
        Parse Tasks.md and find first task with [x] planned but no branch created.
        Returns dict with task_id, title, type, file, source_file, test_command, style_reference, instructions.
        """
        with open(self.tasks_file) as f:
            content = f.read()

        # Find all task blocks: TASK-NNN · [type] ...
        task_pattern = r"## TASK-(\d+) · \[([^\]]+)\] (.+?)\n(.+?)(?=## TASK-|\Z)"
        matches = re.finditer(task_pattern, content, re.DOTALL)

        for match in matches:
            task_id = f"TASK-{match.group(1)}"
            task_type = match.group(2)
            title = match.group(3)
            task_block = match.group(4)

            # Check if [x] planned
            if "- [x] planned" not in task_block:
                continue

            # Check if already has branch
            if "- [x] branch created" in task_block:
                continue

            # Parse metadata from block
            context = re.search(r"> Context: (.+?)(?:\n>|$)", task_block)
            file_match = re.search(r"> File: (.+?)(?:\n>|$)", task_block)
            source_file = re.search(r"> Source file: (.+?)(?:\n>|$)", task_block)
            test_cmd = re.search(r"> Test command: (.+?)(?:\n>|$)", task_block)
            style_ref = re.search(r"> Style reference: (.+?)(?:\n>|$)", task_block)

            return {
                "id": task_id,
                "type": task_type,
                "title": title,
                "context": context.group(1) if context else "",
                "file": file_match.group(1) if file_match else "",
                "source_file": source_file.group(1) if source_file else "",
                "test_command": test_cmd.group(1) if test_cmd else "",
                "style_reference": style_ref.group(1) if style_ref else "",
                "block": task_block,
            }

        return None

    def generate_code(self, task: dict) -> str:
        """
        Call Claude to generate code for the task.
        """
        if task["type"] in ("backend-test", "frontend-test", "e2e"):
            source_file = self.project_root / task["source_file"]
            style_file = self.project_root / task["style_reference"]

            with open(source_file) as f:
                source_content = f.read()
            with open(style_file) as f:
                style_content = f.read()

            task_card = f"{task['id']} · {task['type']}\n{task['context']}\n\nInstructions:\n{task['block']}"
            return self.ai_client.generate_test(task_card, source_content, style_content)

        else:  # refactor
            target_file = self.project_root / task["file"]
            with open(target_file) as f:
                file_content = f.read()

            task_card = f"{task['id']} · {task['type']}\n{task['context']}\n\nInstructions:\n{task['block']}"
            return self.ai_client.generate_refactor(task_card, file_content)

    def show_diff_and_ask_approval(self, task: dict, new_content: str) -> bool:
        """
        Show the generated code to the user and ask for approval.
        Returns True if approved, False if rejected.
        """
        print("\n" + "=" * 80)
        print(f"GENERATED CODE FOR {task['id']}")
        print("=" * 80)
        print(new_content)
        print("=" * 80)

        while True:
            response = input("\nApprove? (y/n/edit): ").strip().lower()
            if response == "y":
                return True
            elif response == "n":
                return False
            elif response == "edit":
                print("Edit the file manually before committing")
                return True
            else:
                print("Invalid response. Try again.")

    def run_task(self, task: dict):
        """
        Execute one task: generate code, test, optionally commit and open PR.
        """
        print(f"\n[Orchestrator] Running {task['id']}: {task['title']}")

        # Generate code
        print(f"[Claude] Generating {task['type']} code...")
        try:
            new_content = self.generate_code(task)
        except Exception as e:
            print(f"✗ Code generation failed: {e}")
            self._log_attempt(task["id"], "FAILED", str(e))
            return

        # Show diff and get approval
        if not self.show_diff_and_ask_approval(task, new_content):
            print("✗ Rejected by user")
            self._log_attempt(task["id"], "REJECTED_BY_USER")
            return

        # Write file
        target_file = self.project_root / task["file"]
        target_file.parent.mkdir(parents=True, exist_ok=True)
        with open(target_file, "w") as f:
            f.write(new_content)
        print(f"✓ Wrote {target_file}")

        # Run tests
        print(f"[Tests] Running: {task['test_command']}")
        if task["type"] == "backend-test":
            passed, msg = self.test_runner.run_backend_tests(
                re.search(r"Dtest=(\w+)", task["test_command"]).group(1)
                if "Dtest=" in task["test_command"]
                else None
            )
        elif task["type"] == "frontend-test":
            passed, msg = self.test_runner.run_frontend_tests()
        else:
            passed, msg = self.test_runner.run_all_tests()

        if not passed:
            print(f"✗ Tests failed:\n{msg}")
            self._log_attempt(task["id"], "TEST_FAILED", msg)
            return

        print("✓ Tests passed")

        # Commit and push
        branch_name = f"debt/{task['id'].lower()}-{task['title'].lower().replace(' ', '-')[:20]}"
        print(f"\n[Git] Creating branch: {branch_name}")

        try:
            self.git_client.checkout_main()
            if self.git_client.branch_exists(branch_name):
                print(f"✗ Branch already exists: {branch_name}")
                return

            self.git_client.create_branch(branch_name)
            self.git_client.commit_and_push(
                str(target_file.relative_to(self.project_root)),
                f"debt-bot: {task['id']} {task['title']}",
                branch_name,
            )

            # Open PR
            pr_title = f"debt-bot: {task['id']} {task['title']}"
            pr_body = (
                f"## {task['title']}\n\n{task['context']}\n\nGenerated by debt-bot Phase 1 trial."
            )
            pr_url = self.git_client.open_pr(branch_name, pr_title, pr_body)

            self._log_attempt(task["id"], "SUCCESS", pr_url)
            print(f"✓ PR opened: {pr_url}")

        except Exception as e:
            print(f"✗ Git operation failed: {e}")
            self._log_attempt(task["id"], "GIT_FAILED", str(e))

    def _log_attempt(self, task_id: str, status: str, detail: str = ""):
        """Append to Status.md."""
        with open(self.status_file, "a") as f:
            timestamp = datetime.now().isoformat()
            f.write(f"\n[{timestamp}] {task_id} {status}")
            if detail:
                f.write(f" — {detail[:100]}")


def main():
    project_root = Path.cwd()
    orchestrator = Phase1Orchestrator(project_root)

    print("[Phase 1 Orchestrator] Starting manual trial")
    print("=" * 80)

    task = orchestrator.find_next_pending_task()
    if not task:
        print("✗ No pending tasks found. Check Tasks.md — mark tasks with [x] planned")
        return

    print(f"Found pending task: {task['id']}")
    orchestrator.run_task(task)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n[Orchestrator] Interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n✗ Orchestrator error: {e}")
        import traceback

        traceback.print_exc()
        sys.exit(1)
