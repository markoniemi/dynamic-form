#!/usr/bin/env python3
"""
AI client — calls Claude API to generate code based on task cards and source files.
Requires ANTHROPIC_API_KEY environment variable.
"""

import os
from pathlib import Path

import anthropic


class AiClient:
    def __init__(self, model: str = "claude-opus-4-8"):
        api_key = os.environ.get("ANTHROPIC_API_KEY")
        if not api_key:
            raise ValueError("ANTHROPIC_API_KEY environment variable not set")

        self.client = anthropic.Anthropic(api_key=api_key)
        self.model = model

    def generate_test(
        self,
        task_card: str,
        source_file_content: str,
        style_reference_content: str,
        prompt_template_path: Path | None = None,
    ) -> str:
        """
        Generate a test file based on task card, source file, and style reference.
        Returns the complete test file content.
        """
        if prompt_template_path is None:
            prompt_template = """You are an automated test writer. Create a test file for the scenario
described. Match the style of the reference file exactly. Do not test anything not listed in
the task. Do not modify any existing files.

TASK:
{task_card}

SOURCE FILE CONTENT:
{source_file_content}

STYLE REFERENCE:
{style_reference_content}

Respond with ONLY the complete new test file content. Nothing else."""
        else:
            with open(prompt_template_path) as f:
                prompt_template = f.read()

        prompt = prompt_template.format(
            task_card=task_card,
            source_file_content=source_file_content,
            style_reference_content=style_reference_content,
        )

        message = self.client.messages.create(
            model=self.model, max_tokens=4096, messages=[{"role": "user", "content": prompt}]
        )

        return message.content[0].text

    def generate_refactor(
        self, task_card: str, file_content: str, prompt_template_path: Path | None = None
    ) -> str:
        """
        Generate refactored code based on task card and current file content.
        Returns the complete updated file content.
        """
        if prompt_template_path is None:
            prompt_template = """You are an automated refactoring agent. Complete this task exactly as
specified. Do not make any changes beyond the instructions. Do not add comments. Do not
reformat unrelated code.

TASK:
{task_card}

CURRENT FILE CONTENT:
{file_content}

Respond with ONLY the complete updated file content. Nothing else."""
        else:
            with open(prompt_template_path) as f:
                prompt_template = f.read()

        prompt = prompt_template.format(task_card=task_card, file_content=file_content)

        message = self.client.messages.create(
            model=self.model, max_tokens=4096, messages=[{"role": "user", "content": prompt}]
        )

        return message.content[0].text

    def analyze_coverage(self, analysis_prompt: str, report_content: str) -> str:
        """
        Analyze test coverage reports and generate findings.
        """
        prompt = f"""{analysis_prompt}

REPORT:
{report_content}

Respond with markdown findings, formatted as a bulleted list with scores."""

        message = self.client.messages.create(
            model=self.model, max_tokens=4096, messages=[{"role": "user", "content": prompt}]
        )

        return message.content[0].text


if __name__ == "__main__":
    client = AiClient()
    print("✓ Claude API client initialized successfully")
    print(f"  Model: {client.model}")
