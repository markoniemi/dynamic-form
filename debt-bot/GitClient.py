#!/usr/bin/env python3
"""
Git client — GitHub PR operations via gh CLI.
"""
import subprocess
import json
import os
from typing import Optional, Dict, Tuple
from pathlib import Path


class GitClient:
    def __init__(self, repo_owner: str, repo_name: str, project_root: Path = Path.cwd()):
        self.repo_owner = repo_owner
        self.repo_name = repo_name
        self.project_root = Path(project_root)
        self.gh_repo = f"{repo_owner}/{repo_name}"
        self._verify_gh_auth()

    def _verify_gh_auth(self):
        """Verify gh CLI is installed and authenticated."""
        try:
            result = subprocess.run(
                ["gh", "auth", "status"],
                capture_output=True,
                text=True,
                timeout=5
            )
            if result.returncode != 0:
                raise RuntimeError("gh CLI not authenticated. Run: gh auth login")
        except FileNotFoundError:
            raise RuntimeError("gh CLI not found. Install from https://cli.github.com")

    def create_branch(self, branch_name: str):
        """Create and checkout a local branch."""
        try:
            subprocess.run(
                ["git", "checkout", "-b", branch_name],
                cwd=self.project_root,
                capture_output=True,
                check=True
            )
            print(f"✓ Created branch: {branch_name}")
        except subprocess.CalledProcessError as e:
            raise RuntimeError(f"Failed to create branch: {e.stderr.decode()}")

    def commit_and_push(self, file_path: str, commit_message: str, branch_name: str):
        """Add file, commit, and push to branch."""
        try:
            # Add file
            subprocess.run(
                ["git", "add", file_path],
                cwd=self.project_root,
                capture_output=True,
                check=True
            )

            # Commit
            subprocess.run(
                ["git", "commit", "-m", commit_message],
                cwd=self.project_root,
                capture_output=True,
                check=True
            )

            # Push
            subprocess.run(
                ["git", "push", "origin", branch_name],
                cwd=self.project_root,
                capture_output=True,
                check=True
            )

            print(f"✓ Committed and pushed: {file_path}")
        except subprocess.CalledProcessError as e:
            raise RuntimeError(f"Failed to commit/push: {e.stderr.decode()}")

    def open_pr(
        self,
        branch_name: str,
        title: str,
        body: str,
        base_branch: str = "master"
    ) -> str:
        """
        Open a PR from branch_name to base_branch.
        Returns the PR URL.
        """
        try:
            result = subprocess.run(
                [
                    "gh", "pr", "create",
                    "--repo", self.gh_repo,
                    "--base", base_branch,
                    "--head", branch_name,
                    "--title", title,
                    "--body", body
                ],
                cwd=self.project_root,
                capture_output=True,
                text=True,
                check=True
            )

            pr_url = result.stdout.strip()
            print(f"✓ Opened PR: {pr_url}")
            return pr_url

        except subprocess.CalledProcessError as e:
            raise RuntimeError(f"Failed to open PR: {e.stderr}")

    def get_pr_status(self, branch_name: str) -> Optional[Dict]:
        """
        Get PR status for a branch. Returns dict with state, url, or None if no PR.
        """
        try:
            result = subprocess.run(
                [
                    "gh", "pr", "view", branch_name,
                    "--repo", self.gh_repo,
                    "--json", "url,state,number"
                ],
                cwd=self.project_root,
                capture_output=True,
                text=True
            )

            if result.returncode != 0:
                return None  # No PR for this branch

            return json.loads(result.stdout)

        except Exception:
            return None

    def checkout_main(self, branch: str = "master"):
        """Checkout main branch and pull."""
        try:
            subprocess.run(
                ["git", "checkout", branch],
                cwd=self.project_root,
                capture_output=True,
                check=True
            )
            subprocess.run(
                ["git", "pull", "origin", branch],
                cwd=self.project_root,
                capture_output=True,
                check=True
            )
            print(f"✓ Checked out {branch} and pulled latest")
        except subprocess.CalledProcessError as e:
            raise RuntimeError(f"Failed to checkout/pull: {e.stderr.decode()}")

    def branch_exists(self, branch_name: str) -> bool:
        """Check if remote branch exists."""
        try:
            result = subprocess.run(
                ["git", "ls-remote", "--heads", "origin", branch_name],
                cwd=self.project_root,
                capture_output=True,
                text=True
            )
            return result.returncode == 0 and result.stdout.strip() != ""
        except Exception:
            return False

    def discard_changes(self):
        """Discard all local changes."""
        try:
            subprocess.run(
                ["git", "checkout", "."],
                cwd=self.project_root,
                capture_output=True,
                check=True
            )
            print("✓ Discarded all changes")
        except subprocess.CalledProcessError as e:
            raise RuntimeError(f"Failed to discard changes: {e.stderr.decode()}")


if __name__ == "__main__":
    # Example usage
    try:
        git = GitClient("markoniemi", "dynamic-form")
        print("✓ GitHub client initialized")
    except Exception as e:
        print(f"✗ Failed to initialize: {e}")
