---
description: Linear project management workflow — always apply
---

# Linear Workflow

Linear is the single source of truth for project management. GitHub Issues are not used.

## Workspace & Team

- **Workspace**: Slumbr (`https://linear.app/slumbr`)
- **Team**: SLU (team key: `SLU`, issue prefix: `SLU-XX`)
- **NEVER** interact with any other workspace or team
- All Linear API calls that accept a `teamId` MUST target the SLU team
- If `list_teams` returns teams other than SLU, do NOT create/modify issues in them

## Hierarchy

```
Initiative (human-created only)
  └── Project (Claude creates from approved specs)
        ├── Milestone (optional, for phased delivery)
        └── Issue (SLU-XX)
```

## Statuses

| Status | Meaning |
|--------|---------|
| Backlog | Ideas, someday/maybe, not committed |
| Planning | Actively being specced/designed |
| Todo | Spec approved, ready to build |
| In Progress | Actively being coded |
| In Review | PR created, awaiting review/merge |
| Done | Merged |
| Canceled | Won't do |
| Duplicate | Duplicate of another issue |

## Labels

Four label groups (single-select within each):

**Type**: Feature, Bug, Improvement, Tech Debt, Docs, QA/Manual
**Scope**: Frontend, Backend, Multi-layer, Infra
**Security Risk**: Security: Low, Security: Medium, Security: High
**Technical Risk**: Tech Risk: Low, Tech Risk: Medium, Tech Risk: High

Standalone flag labels (additive):

**Breaking Change** — data model or API breaking change
**Spike** — research/exploration task, output is knowledge not code
**Needs Design** — blocked on human design/UX decision

Every issue MUST have a Type label and a Scope label. Risk labels required for non-trivial work.

## Estimates

| Size | Complexity |
|------|-----------|
| XS | Trivial — single location, obvious approach |
| S | Small — 1-2 files, clear approach |
| M | Moderate — several files, some design decisions |
| L | Large — cross-cutting, requires design |
| XL | Extra large — high interconnectedness, architectural |

## Priority & Due Dates

Claude does NOT set priority or due dates unless explicitly told. These are human decisions.

## Spec → Linear Flow

After a spec is approved via `spec-planner`:

1. Create **Project** with spec title, summary, and spec file path
2. Create **Milestones** if spec has phased deliverables
3. Create **Issues** for each task with labels, estimates, relationships
4. Do NOT set: priority, due dates, assignee (unless told)

## Starting Work

1. Search Linear for existing issues
2. If no issue exists, create one with appropriate labels
3. Move issue to **In Progress**
4. Fetch latest main and create a fresh feature branch

## After PR Merge

When a PR is merged (by the user or otherwise), move all related Linear issues to **Done**. The GitHub↔Linear auto-close integration is not currently active, so this must be done manually via `save_issue` with `state: "Done"`.

## Risk-Driven Reviews Before PR

| Security Risk | Technical Risk | Required Reviews |
|--------------|---------------|-----------------|
| High | Any | Security review + `/code-review` |
| Any | High | Oracle agent + `/code-review` |
| Any | Medium | `/code-review` |
| Low/None | Low/None | Recommended for non-trivial changes |

## Ad-hoc Issue Creation

| Situation | Type | Status |
|-----------|------|--------|
| Bug found during work | Bug | Backlog |
| Refactor opportunity | Tech Debt | Backlog |
| Feature idea | Feature | Backlog |
| Enhancement | Improvement | Backlog |
| Manual testing needed | QA/Manual | Backlog |
