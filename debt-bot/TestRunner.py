#!/usr/bin/env python3
"""
Test runner — detects pass/fail for Jest (frontend) and Maven/JUnit (backend).
Returns exit code 0 on success, 1 on failure.
"""

import subprocess
import sys
from pathlib import Path


class TestRunner:
    def __init__(self, project_root: Path):
        self.project_root = Path(project_root)
        self.backend_dir = self.project_root / "backend"
        self.frontend_dir = self.project_root / "frontend"

    def run_backend_tests(self, test_pattern: str = None) -> tuple[bool, str]:
        """
        Run Maven tests for backend. Returns (passed: bool, output: str).
        If test_pattern is provided, runs specific test class (e.g., "UserServiceTest").
        """
        try:
            cmd = ["mvn", "test"]
            if test_pattern:
                cmd.append(f"-Dtest={test_pattern}")

            print(f"[TestRunner] Running backend tests: {' '.join(cmd)}")
            result = subprocess.run(
                cmd, cwd=self.backend_dir, capture_output=True, text=True, timeout=300
            )

            output = result.stdout + result.stderr
            passed = result.returncode == 0

            if not passed:
                # Extract failure summary from Maven output
                failure_lines = [
                    line
                    for line in output.split("\n")
                    if "FAILURE" in line or "ERROR" in line or "Tests run:" in line
                ]
                error_msg = "\n".join(failure_lines[-10:]) if failure_lines else output[-500:]
                return False, error_msg

            return True, "Backend tests passed"

        except subprocess.TimeoutExpired:
            return False, "Backend tests timed out after 300s"
        except Exception as e:
            return False, f"Backend test error: {str(e)}"

    def run_frontend_tests(self) -> tuple[bool, str]:
        """
        Run Vitest for frontend. Returns (passed: bool, output: str).
        """
        try:
            cmd = ["npm", "test"]

            print(f"[TestRunner] Running frontend tests: {' '.join(cmd)}")
            result = subprocess.run(
                cmd,
                cwd=self.frontend_dir,
                capture_output=True,
                text=True,
                timeout=300,
                env={**dict(subprocess.os.environ), "CI": "true"},
            )

            output = result.stdout + result.stderr
            passed = result.returncode == 0

            if not passed:
                # Extract failure summary from Vitest output
                failure_lines = [
                    line
                    for line in output.split("\n")
                    if "FAIL" in line or "ERROR" in line or "failed" in line
                ]
                error_msg = "\n".join(failure_lines[-10:]) if failure_lines else output[-500:]
                return False, error_msg

            return True, "Frontend tests passed"

        except subprocess.TimeoutExpired:
            return False, "Frontend tests timed out after 300s"
        except Exception as e:
            return False, f"Frontend test error: {str(e)}"

    def run_all_tests(self) -> tuple[bool, str]:
        """
        Run all tests (backend + frontend). Returns (all_passed: bool, summary: str).
        """
        backend_passed, backend_msg = self.run_backend_tests()
        frontend_passed, frontend_msg = self.run_frontend_tests()

        summary = f"Backend: {'✓' if backend_passed else '✗'} | Frontend: {'✓' if frontend_passed else '✗'}\n"
        if not backend_passed:
            summary += f"\nBackend error:\n{backend_msg}"
        if not frontend_passed:
            summary += f"\nFrontend error:\n{frontend_msg}"

        return backend_passed and frontend_passed, summary


if __name__ == "__main__":
    runner = TestRunner(Path.cwd())

    if len(sys.argv) > 1 and sys.argv[1] == "backend":
        test_pattern = sys.argv[2] if len(sys.argv) > 2 else None
        passed, msg = runner.run_backend_tests(test_pattern)
    elif len(sys.argv) > 1 and sys.argv[1] == "frontend":
        passed, msg = runner.run_frontend_tests()
    else:
        passed, msg = runner.run_all_tests()

    print(msg)
    sys.exit(0 if passed else 1)
