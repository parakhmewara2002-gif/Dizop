# DIZOP Antigravity CLI Multi-Agent Setup

This package adds seven reusable custom subagents for the existing DIZOP Android project.

## Install

1. Extract this ZIP.
2. Copy the `.agents` folder into the ROOT of your existing DIZOP source project.
3. Keep any existing `.agents` folder. If one already exists, merge the `agents` subfolder; do not overwrite unrelated files.
4. Open Antigravity CLI from the DIZOP project root.
5. Run `/agents` and confirm the custom agents appear.
6. If they do not appear, check the exact path:
   `.agents/agents/<agent-name>.md`
   Then restart/reload the CLI session and run `/agents` again.

## Included agents

- `architecture-auditor` — read-only architecture and codebase audit
- `printer-protocol-engineer` — printer protocol implementation specialist
- `scanner-pdf-engineer` — CameraX, OCR, image and PDF specialist
- `security-auditor` — read-only Android security review
- `qa-testing-engineer` — tests, builds and regression verification
- `ui-performance-auditor` — read-only Compose, accessibility and performance audit
- `independent-code-reviewer` — read-only review of changes made by other agents

## Start the workflow

Open `DIZOP_MULTI_AGENT_ORCHESTRATOR_PROMPT.md` and paste its contents into the main Antigravity CLI agent.

The first run is AUDIT-ONLY. It must not modify application source files. After you review the consolidated findings, give the orchestrator approval to begin implementation.

## Monitor agents

- `/agents` — inspect active agents and their status
- `/tasks` — inspect background shell/build tasks
- `Alt+J` — jump to an agent waiting for approval (where supported)

## Safety

- Make a Git commit or backup before implementation.
- Do not allow multiple agents to edit the same files simultaneously.
- Keep auditors and the independent reviewer read-only by instruction.
- Review shell commands and approve destructive actions manually.
- Never let a developer agent approve its own critical change.
- A mock test is not hardware verification.
- A successful build is not proof that a printer works.

## Compatibility note

These files use Antigravity CLI custom-agent Markdown frontmatter (`name`, `description`, `subagent`). Tool names and available permissions can vary by CLI version and configuration. If your CLI rejects a field or agent file, consult `/agy-customizations` and the installed CLI's current documentation rather than guessing.
