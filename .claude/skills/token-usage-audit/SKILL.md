name: token-usage-audit description: Audit a codebase for unnecessary AI coding assistant token consumption. Evaluate CLAUDE.md, documentation, skills, MCP servers, Maven modules, build commands, test workflows, repository exploration, subagents, and context management. Produce a prioritized improvement report without modifying project files.
Token Usage Audit
Purpose

Audit the current project and identify changes that can reduce token consumption when using Claude Code or similar AI coding assistants.

The audit should focus on:

Reducing unnecessary context loading.
Reducing unnecessary repository exploration.
Reducing tool definitions and MCP overhead.
Reducing command output.
Limiting work to relevant Maven modules.
Improving instructions and documentation structure.
Avoiding unnecessary subagents and repeated work.
Improving prompt caching.
Using appropriate models and reasoning effort.
Making recurring workflows more efficient through skills.

This skill is an audit, not an implementation task.

Do not modify, delete, move, or create project files during the audit.

Only produce recommendations and an audit report.

1. Audit principles

Follow these principles:

Prefer evidence over assumptions.
Avoid scanning the entire repository unnecessarily.
Inspect configuration before inspecting source code.
Do not read large documentation files in full unless required.
Do not execute full builds or tests.
Do not install MCP servers or other tools.
Do not modify project configuration.
Do not expose secrets, credentials, or sensitive environment variables.
Distinguish confirmed problems from potential improvements.
Do not recommend changes that compromise build correctness, test coverage, or development quality.

The objective is to reduce unnecessary token consumption without reducing software quality.

2. Project discovery

Identify the project structure.

For Maven projects, inspect:

Root pom.xml.
Maven module declarations.
Module directory structure.
Maven wrapper availability.
Existing module-specific instructions.
Existing build and test configuration.

Use lightweight commands where possible:

bash
pwd
find . -maxdepth 2 -name pom.xml -not -path '*/target/*'
find . -name CLAUDE.md -not -path '*/target/*'
find . -maxdepth 3 -type d -name .claude


Avoid recursively reading every source file.

Determine:

Whether this is a single-module or multi-module project.
Number of Maven modules.
Whether modules have different responsibilities.
Whether module-level instructions exist.
Whether the project has generated or vendored directories that should be excluded from exploration.
3. CLAUDE.md audit

Inspect the root and relevant module-level CLAUDE.md files.

Evaluate:

Content
Are instructions unnecessarily long?
Is architecture documentation duplicated?
Are README contents duplicated?
Are detailed technical descriptions included unnecessarily?
Are instructions repeated across multiple files?
Are there outdated instructions?
Are there rules that apply only to specific modules?
Structure

Check whether responsibilities are separated appropriately:

Root CLAUDE.md: global project rules.
Module CLAUDE.md: module-specific rules.
docs/: detailed technical documentation.
Skills: repeatable workflows.
Rules: focused coding conventions.
Scope

Check whether instructions explicitly tell the assistant to:

Work only within the requested module.
Avoid unrelated repository exploration.
Avoid unnecessary refactoring.
Use targeted tests.
Keep command output short.
Request permission before modifying shared modules or parent POMs.
Recommendations

Identify instructions that should be:

Removed.
Shortened.
Consolidated.
Moved to documentation.
Moved to a skill.
Made module-specific.

Do not remove useful architectural knowledge merely to reduce file length.

4. README and documentation audit

Inspect README files and documentation organization.

Check:

Whether README contains excessively detailed technical information.
Whether multiple documents repeat the same information.
Whether build instructions are duplicated.
Whether module documentation is mixed with global documentation.
Whether documentation contains information irrelevant to most development tasks.

Recommend a documentation structure where appropriate:

text
docs/
├── architecture.md
├── development.md
├── testing.md
├── maven-build.md
├── coding-standards.md
└── modules/
├── module-a.md
└── module-b.md


Prefer linking to documentation rather than copying its entire contents into CLAUDE.md.

Evaluate whether documentation can be loaded selectively.

5. Skills audit

Inspect existing skills.

Evaluate:

Number of skills.
Size of skill instructions.
Duplicate instructions.
Skills that overlap in functionality.
Skills that are unnecessarily fragmented.
Skills that contain large reference documents.
Skills that are not reusable.
Skills that should be consolidated.

Recommend focused skills for recurring workflows.

Examples:

Maven module testing.
Java code review.
Debugging.
Refactoring.
Token usage audit.

Do not recommend creating a skill for every individual coding convention.

Keep specialized reference material separate when it is substantial and only occasionally needed.

6. MCP audit

Inspect available MCP configuration files where accessible.

Identify:

Configured MCP servers.
Number of configured tools.
Servers that appear redundant.
Servers that duplicate built-in Bash functionality.
Servers that are relevant only to certain projects.
Servers that provide genuine structured functionality.

Pay particular attention to whether MCP is being used unnecessarily for:

Maven builds.
Maven tests.
Git commands.
File searches.
Basic shell commands.

For local Maven projects, normally recommend built-in Bash for:

bash
./mvnw test
./mvnw package
./mvnw -pl module-a test


Consider MCP appropriate when it provides meaningful capabilities beyond shell execution, such as structured CI results, remote build management, issue tracking, or code-quality integrations.

Do not assume that every configured MCP server is loaded into every session. Distinguish configured servers from servers and tools actually available to the assistant.

7. Maven and multi-module audit

For Maven projects, this is a high-priority audit area.

Inspect:

Root POM module declarations.
Module POM files.
Dependency relationships.
Build plugins.
Test plugins.
Maven wrapper configuration.
Existing build scripts.

Evaluate whether developers can work independently within modules.

Check whether instructions encourage unnecessary commands such as:

bash
./mvnw clean install


for ordinary module changes.

Recommend targeted alternatives where appropriate:

Single module:

bash
./mvnw -pl module-a test


Single test class:

bash
./mvnw -pl module-a -Dtest=ExampleTest test


Module with required reactor dependencies:

bash
./mvnw -pl module-a -am test


Check whether:

Module names and artifact IDs are documented.
Module-specific build commands are available.
The assistant is instructed to start in the target module.
Cross-module changes require approval.
Full reactor builds are reserved for appropriate situations.

Important distinction:

Maven's -pl option limits Maven build selection. It does not restrict which files the assistant can inspect or modify.

Recommend separate permission controls or filesystem restrictions when stronger boundaries are needed.

8. Repository exploration audit

Evaluate how the assistant is instructed to discover code.

Look for:

Unrestricted recursive searches.
Full repository analysis for small tasks.
Reading all README files.
Reading every POM file unnecessarily.
Searching generated directories.
Inspecting unrelated modules.
Repeatedly opening the same files.
Large file dumps.

Recommend targeted exploration.

Example:

Instead of:

"Analyze the entire repository and fix the billing issue."

Recommend:

"Investigate the issue in module-billing. Start with the relevant service and its tests. Inspect only directly related classes and dependencies. Do not inspect or modify unrelated modules."

Identify directories that should normally be excluded from exploration:

target/
build/
node_modules/
generated/
vendor/

Adapt exclusions to the actual project.

9. Build and test output audit

Inspect build scripts and development instructions.

Evaluate:

Whether quiet Maven options are used for routine commands.
Whether test output is unnecessarily verbose.
Whether logs are pasted in full.
Whether test execution is unnecessarily broad.
Whether failure summaries are available.
Whether shell pipelines preserve exit codes.

Recommend:

bash
./mvnw -q -pl module-a test


For log filtering:

bash
grep -E "ERROR|FAILURE|Tests run:|BUILD SUCCESS|BUILD FAILURE" \
build.log | tail -30


Where shell pipelines are used, recommend preserving the Maven exit status.

Do not recommend suppressing diagnostic information required to investigate failures.

9A. Maven output volume and verbosity audit
Objective

Identify why Maven builds and tests generate excessive console output and recommend ways to reduce the amount of output returned to the AI assistant.

Treat this as a high-priority optimization area.

A build producing hundreds or thousands of lines may unnecessarily increase context consumption, particularly when the same build is executed repeatedly.

Step 1: Establish baseline output

Inspect the project's existing Maven build and test commands.

Do not execute a full-project build just to measure output.

If execution is appropriate and safe, select one representative module and one routine test command.

Capture output to a temporary file:

bash
./mvnw -pl MODULE test > /tmp/maven-audit.log 2>&1
status=$?

wc -l /tmp/maven-audit.log
du -h /tmp/maven-audit.log

exit $status


Record:

Total output lines.
Approximate output size.
Whether output is mostly informational or diagnostic.
Whether the output is repeated across modules.
Whether the command prints test details, application logs, or plugin diagnostics.

Do not publish or copy sensitive log contents into the audit report.

Step 2: Identify output sources

Investigate the likely source of verbosity.

Inspect:

Maven lifecycle output.
Maven plugin configuration.
Compiler output.
Surefire output.
Failsafe output.
Checkstyle output.
SpotBugs output.
JaCoCo output.
Spring Boot startup output.
Testcontainers output.
Hibernate SQL and parameter logging.
Application logging configuration.
Custom Maven plugins.
Shell scripts invoked during builds.

Where possible, identify which plugin or component generates the largest amount of output.

Do not assume that Maven itself is responsible for all output.

Step 3: Evaluate quiet mode

Check whether routine commands can use:

bash
./mvnw -q test


For a module:

bash
./mvnw -q -pl MODULE test


Determine whether quiet mode sufficiently reduces output.

Check whether plugins or test applications continue printing large amounts of information.

Do not recommend quiet mode as the only solution when most output originates from application logging or plugins.

Step 4: Inspect plugin verbosity

Review relevant plugin configurations in the root and module POM files.

Look for:

Verbose or debug options.
Excessive compiler diagnostics.
Repeated plugin banners.
Full test execution details.
Unnecessary reporting.
Repeated warnings.
Duplicate report generation.
Excessive integration-test logging.

Recommend configuration changes only where they preserve diagnostic usefulness.

Do not disable test execution, coverage verification, static analysis, or important quality checks merely to reduce output.

Step 5: Inspect application logging

For Spring and Java applications, inspect test-specific logging configurations.

Look for:

Root logging level set to DEBUG or TRACE.
Excessive Spring framework logging.
Hibernate SQL logging.
Hibernate parameter logging.
HTTP request and response dumps.
Testcontainers diagnostic messages.
Repeated application startup banners.
Verbose integration-test logging.

Recommend targeted test logging levels.

Keep ERROR and important WARN messages visible.

Do not suppress exceptions or failure diagnostics.

Step 6: Evaluate log redirection and summarization

Check whether Maven output can be redirected to a temporary log and only relevant results returned to the assistant.

Example:

bash
./mvnw -q test > /tmp/maven-build.log 2>&1
status=$?

grep -E "ERROR|FAILURE|Tests run:|BUILD SUCCESS|BUILD FAILURE" \
/tmp/maven-build.log | tail -40

exit $status


Evaluate whether this approach is appropriate for the project's normal workflow.

Check that:

Maven's exit status is preserved.
Failure details remain accessible.
Temporary logs are not committed.
Sensitive information is not printed unnecessarily.
Test reports remain available.
Step 7: Recommend a reusable build wrapper

If excessive output occurs repeatedly, consider a small project script.

For example:

bash
#!/usr/bin/env bash

set -u

MODULE="${1:?Module required}"
shift

LOG_FILE="$(mktemp "${TMPDIR:-/tmp}/maven-build.XXXXXX.log")"

./mvnw -q -pl "$MODULE" "$@" > "$LOG_FILE" 2>&1
STATUS=$?

echo "Maven exit status: $STATUS"
echo "Log file: $LOG_FILE"

if [ "$STATUS" -eq 0 ]; then
echo "Build completed successfully."
grep -E "Tests run:|BUILD SUCCESS" "$LOG_FILE" | tail -20
else
echo "Build failed. Relevant diagnostics:"
grep -n -E "ERROR|FAILURE|BUILD FAILURE|Tests run:" \
"$LOG_FILE" | tail -60
fi

exit "$STATUS"


This is an example, not an instruction to automatically create the script.

Evaluate whether a wrapper is justified by repeated usage.

Step 8: Compare before and after

For each recommended change, compare:

Metric	Before	After
Output lines	Measured	Measured
Output size	Measured	Measured
Test results	Measured	Measured
Build exit status	Recorded	Recorded
Important warnings visible	Yes/No	Yes/No
Failure diagnostics available	Yes/No	Yes/No

Do not invent results or percentage savings.

A reduction in output lines does not necessarily represent an equivalent reduction in model tokens. Output should be measured in actual token usage where possible.

Step 9: Report findings

For each identified problem, report:

Output source.
Evidence.
Approximate output volume.
Recommended configuration or command change.
Expected benefit.
Diagnostic risks.
Implementation effort.
Verification procedure.

Prioritize changes that reduce repeated output without compromising troubleshooting.

Important restrictions
Do not run full reactor builds during the audit.
Do not modify POM files.
Do not change logging levels automatically.
Do not suppress build failures.
Do not disable tests or quality checks.
Do not discard logs before extracting relevant diagnostics.
Do not claim token savings without measurement.

10. Model and reasoning audit

Check project instructions and development conventions for model usage guidance.

Recommend:

Smaller, less expensive models for routine edits.
More capable models for complex architectural decisions and difficult debugging.
Lower reasoning effort for simple mechanical changes where supported.
Avoiding repeated analysis when the task is already understood.

Do not assume that all coding assistants expose the same model or reasoning controls.

Treat model selection and reasoning effort as session-level optimizations, not project configuration requirements.

11. Prompt caching audit

Check whether instructions encourage stable session context.

Identify potential cache-disrupting habits:

Frequently editing CLAUDE.md during active sessions.
Repeatedly changing MCP configuration.
Unnecessary model switching.
Repeatedly restructuring initial session instructions.

Recommend:

Finalize global instructions before starting a session.
Keep stable project context.
Make configuration changes between sessions.
Avoid unnecessary changes to the beginning of the conversation.

Do not claim that every configuration change always resets every cache. Cache behavior depends on the assistant, provider, and implementation.

12. Subagent audit

Evaluate whether subagents are being used appropriately.

Check for:

Subagents used for trivial edits.
Multiple agents investigating identical files.
Excessive parallel exploration.
Large duplicated findings.
Unnecessary agent result handoffs.

Recommend subagents when:

Tasks are independently investigable.
Different modules can be analyzed separately.
Independent reviews provide meaningful value.
Parallel work reduces overall effort.

Do not recommend subagents merely to increase parallelism.

Account for the additional tokens and coordination overhead.

13. Measurement and observability audit

Identify whether the project has a process for measuring token usage.

Check for:

Availability of usage or cost commands.
Context inspection procedures.
Provider usage reports.
Local usage analysis tools.
Historical cost tracking.
Per-task cost comparisons.

Recommend measuring before and after optimization.

Where possible, compare equivalent tasks using:

Input tokens.
Output tokens.
Cache-read tokens.
Cache-write tokens.
Number of tool calls.
Number of files inspected.
Build and test execution scope.
Task completion quality.

Do not invent token savings or monetary estimates.

Distinguish token savings from execution-time savings.

14. Audit scoring and prioritization

Classify each finding:

Severity
Critical: Significant unnecessary context consumption or repeated whole-project work.
High: Major opportunities to reduce exploration, builds, or tool overhead.
Medium: Noticeable inefficiency in documentation, skills, or instructions.
Low: Minor improvements or optional refinements.
Confidence
Confirmed: Directly supported by inspected configuration or instructions.
Likely: Strong evidence but actual session behavior is unknown.
Potential: Worth investigating, but insufficient evidence.
Effort
Small: A straightforward instruction or configuration change.
Medium: Requires restructuring or coordination.
Large: Requires workflow or tooling changes.

For every recommendation, describe:

Finding.
Evidence.
Why it increases token consumption.
Proposed change.
Expected benefit.
Implementation effort.
Risks or trade-offs.
How to verify the improvement.

Do not assign fabricated percentage savings.

15. Required audit report

Produce the following report.

Token Usage Audit Report
A. Project overview
Project type.
Number of Maven modules.
Instruction files found.
Skills found.
MCP configuration found.
Main build and test approach.
B. Executive summary

Summarize the most important findings.

C. Findings by category

Organize findings into:

CLAUDE.md.
README and documentation.
Skills.
MCP.
Maven and modules.
Repository exploration.
Build and test output.
Model and reasoning.
Prompt caching.
Subagents.
Measurement.

For each finding, provide evidence and a recommendation.

D. Prioritized improvement plan

Use this table:

Priority	Finding	Proposed change	Effort	Confidence
E. Recommended target structure

Show a proposed directory structure for the project's instructions, skills, and documentation.

Do not assume every project needs the same structure.

F. Recommended CLAUDE.md changes

Identify:

Content to retain.
Content to shorten.
Content to move.
Content to remove.
Missing instructions.

Do not rewrite files during the audit.

G. Maven optimization plan

Provide exact module-scoped commands based on the actual project structure.

Identify situations where reactor dependencies require additional modules.

H. Quick wins

List improvements that can be implemented with minimal effort.

I. Longer-term improvements

Identify changes requiring more substantial work.

J. Measurement plan

Explain how to measure token consumption before and after the changes.

K. Conclusion

Summarize the recommended implementation order.

16. Final behavior
    Complete the audit without modifying project files.
    Do not automatically install tools.
    Do not automatically create skills or documentation.
    Do not run full-project builds.
    Do not expose secrets.
    Clearly identify missing evidence.
    Avoid speculative cost-saving claims.
    Keep the report concise but sufficiently detailed to be actionable.

At the end, ask whether the user wants the recommended changes implemented in a separate implementation phase.
