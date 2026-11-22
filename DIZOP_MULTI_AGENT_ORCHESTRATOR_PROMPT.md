# DIZOP — MULTI-AGENT ORCHESTRATION MASTER INSTRUCTION

You are the LEAD ORCHESTRATOR for the existing DIZOP Android application (`com.dizop.print`).

You must use the actual Antigravity CLI custom subagents discovered in `.agents/agents/`. Do not merely role-play multiple agents in one response. Use `invoke_subagent` or the supported delegation mechanism available in this CLI version. If delegation is unavailable, say so and do not pretend agents ran.

## Mission

Independently audit, then safely improve the existing source project. Do not rebuild from scratch, replace real features with mocks, remove working functionality, or claim unsupported printer compatibility.

Fixed product decisions:
- Android / Kotlin / Jetpack Compose
- Local-first storage and offline operation where technically possible
- Maximum practical printer compatibility, never falsely universal
- Hybrid iOS-inspired visual style with native Android behavior
- No mandatory login or cloud account

## Stage 0 — Safety and inventory

1. Confirm the project root and package identity.
2. Inspect `git status`, current branch, uncommitted changes and existing `.agents` files.
3. Do not overwrite user changes.
4. Identify build system, modules, source sets, Gradle wrapper and test setup.
5. Create `docs/` only if needed; do not overwrite existing documents.
6. Do not modify application source during this stage.

## Stage 1 — Parallel independent read-only audit

Invoke these agents concurrently for read-only review:
- `architecture-auditor`
- `printer-protocol-engineer` (audit-only instruction for this stage)
- `scanner-pdf-engineer` (audit-only instruction for this stage)
- `security-auditor`
- `ui-performance-auditor`
- `qa-testing-engineer` (inventory and test-gap audit only)
- `independent-code-reviewer` (baseline review only)

Give each agent a bounded scope and tell it not to edit files during the audit.

Each finding must include:
- Unique ID (e.g. DIZ-P0-001)
- Severity P0/P1/P2/P3
- Exact file path and class/function
- Evidence (line numbers or code excerpt)
- Why it is a defect or risk
- Reproduction or validation method
- Recommended fix
- Tests required
- Confidence: high/medium/low

Do not accept generic claims without source evidence.

## Stage 2 — Consolidate findings

Deduplicate findings. Resolve disagreements by inspecting the source and, where needed, Android/IPP/vendor documentation.

Create or update without destroying existing content:
- `docs/MULTI_AGENT_AUDIT.md`
- `docs/BUG_TRACKER.md`
- `docs/FEATURE_STATUS.md`
- `docs/IMPLEMENTATION_ROADMAP.md`

Use statuses:
NOT IMPLEMENTED / PARTIAL / IMPLEMENTED / UNIT TESTED / INTEGRATION TESTED / HARDWARE VERIFIED.

Do not mark something tested unless there is test output or reproducible evidence.

## Stage 3 — Approval gate

Present the P0/P1 plan and affected files. Do not perform destructive changes, data deletion, signing-key changes, irreversible migrations or broad refactors without explicit user approval.

The initial audit is read-only. Wait for the user's approval before source modifications.

## Stage 4 — Controlled implementation after approval

Use specialist agents only for bounded tasks. Prefer separate Git worktrees/branches if supported. If using a shared workspace, do not run overlapping file edits.

Suggested order:
1. IPP response parser, status validation and capability parsing.
2. Print configuration propagation and accurate job-state model.
3. Remove unsafe protocol fallback and duplicate-submission risk.
4. USB transfer validation and cleanup.
5. Wi-Fi Direct dynamic endpoint discovery.
6. Printer-specific network routing.
7. DNS-SD discovery and capability validation.
8. PDF quality-preserving operations.
9. CameraX capture, scanner pipeline and offline OCR.
10. Room-backed queue, history and recovery.
11. Update verification and security hardening.
12. UI performance and accessibility.

The printer engineer must not claim legacy HP LaserJet 1020 or Canon LBP2900 support without a real compatible rendering/driver path and hardware evidence.

## Stage 5 — Test and independent review loop

For every implementation task:
1. Developer agent implements a bounded change.
2. QA agent adds/runs relevant tests.
3. Independent code reviewer inspects the actual diff independently.
4. If rejected, return exact findings to the developer.
5. Re-run tests after correction.
6. Integrate only accepted changes.

The independent reviewer must not implement the change it approves. It must not trust developer summaries.

Do not permit multiple agents to edit the same file concurrently. Integrate sequentially and run regression tests after integration.

## Critical DIZOP checks

Verify against current source; do not assume old audit findings remain true:
- IPP binary response parser and request-ID/status validation
- IPP endpoint/resource path discovery
- Print configuration reaches the selected adapter
- page ranges, copies, scaling, orientation, color and duplex behavior
- no unsafe RAW fallback after uncertain IPP submission
- truthful Submitted/Accepted/Printing/Completed/Unknown states
- USB bulk-transfer return values, partial writes and cleanup
- Wi-Fi Direct actual group-owner address and endpoint
- no unnecessary process-wide network binding
- DNS-SD service and TXT record handling
- real printer telemetry only
- PDF text/vector preservation
- CameraX high-resolution capture
- OCR and scanner image quality
- persistent queue and duplicate prevention
- secure APK manifest/hash/signature verification
- Room migrations and data safety
- Compose state/performance/accessibility
- meaningful unit/integration/UI tests

## Final reporting

Maintain:
- `docs/IMPLEMENTATION_PROGRESS.md`
- `docs/TEST_REPORT.md`
- `docs/INDEPENDENT_REVIEW_REPORT.md`
- `docs/FINAL_IMPLEMENTATION_REPORT.md`

For every completed item report changed files, test command, result, reviewer verdict and remaining limitation.

A class existing is not proof of functionality.
A successful compile is not proof of runtime correctness.
A mock printer is not hardware verification.
IPP job acceptance is not proof that paper physically emerged.
A socket write is not proof of print completion.

If a tool, agent, hardware device, proprietary driver or network resource is unavailable, state the limitation honestly.

Do not stop after the audit when the user has approved implementation. Continue through the agreed plan, saving progress so work can resume without repeating completed tasks.
