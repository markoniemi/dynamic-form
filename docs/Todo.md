## Claude config sync

1. [x] Clean up dynamic-form `.claude/` first — it's the template: convert `skills/terraform.yaml` to `skills/terraform/SKILL.md`; remove dead copilot-instructions refs
2. [ ] oauth2-server: merge CLAUDE.md, adapt skills (backend-only, auth-server specific)
3. [ ] interface-log: fresh `.claude/` setup; fewer skills (backend + review + test?)
4. [ ] spring-boot-react-template: fresh `.claude/` setup; move copilot-instructions.md content into CLAUDE.md + skills (like dynamic-form d3056e8), then delete it; generic (non-project-named) skills

## Code

5. [ ] Port static frontend serving to spring-boot-react-template (see dynamic-form commit fc1dcca: classpath:/static/, drop webjars-locator-core, assembly plugin, WebConfig forward)
