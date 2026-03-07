---
description: Git and PR conventions — always apply
---

# Git Workflow

**Task management:** Linear (see `.claude/rules/linear-workflow.md`)

**Before starting work:**
- Search Linear for existing issues (`list_issues` with query)
- Create a Linear issue if one doesn't exist
- Move issue to **In Progress**
- Always start from latest main: `git fetch origin && git checkout main && git pull`
- Create a **new** feature branch from main: `git checkout -b {type}/{slu-XX-description}`
- Branch name MUST reference the issue being worked on (e.g. `feature/slu-12-pink-noise`)
- One branch per issue — never piggyback new issue work onto an existing branch
- If the branch already exists, **delete it and recreate** from main — stale branches cause merge conflicts
- Never reuse an existing feature branch from a previous session without rebasing on main first

**PR body format:**
```
## Summary
<1-3 bullet points>

## Test plan
- [ ] Test item 1
- [ ] Test item 2

Fixes SLU-XX, SLU-YY
https://linear.app/slumbr/issue/SLU-XX
https://linear.app/slumbr/issue/SLU-YY
```

**Commit & PR policy:**
- NEVER commit directly to `main` — always work on a feature branch
- NEVER push without asking — when work is done, ask "ready to commit and open a PR?" or similar
- Commits and PRs require explicit user approval before executing
- Default mindset: "should I open a PR?" not "let me push this"

**GitHub PR Labels:**

Every PR MUST have exactly one `type:` label and one `size:` label. Flag labels are additive.

| Group | Labels |
|-------|--------|
| **Type** (pick one) | `type: feature`, `type: bugfix`, `type: improvement`, `type: tech-debt`, `type: docs`, `type: infra` |
| **Size** (pick one) | `size: xs`, `size: s`, `size: m`, `size: l`, `size: xl` |
| **Flags** (as needed) | `breaking`, `security`, `dependencies` |

Size guide:
- **xs** — trivial (typo, config tweak)
- **s** — 1-2 files, straightforward
- **m** — several files, some thought needed
- **l** — cross-cutting changes
- **xl** — architectural, high review effort

Apply labels when creating the PR via `gh pr create --label "type: feature" --label "size: m"`.

**Always:**
- Apply `type:` and `size:` labels to every PR
- Reference **every** related Linear issue in PR body using `Fixes SLU-XX` magic words
- Include Linear issue URLs for human-clickable context
- Move issues to **In Review** after PR creation
- Wait for CI to pass before merging
- Use `gh` CLI for GitHub operations (PRs, not issues)
- Prefer `--json` flag with `gh` commands to avoid spurious errors

**Never:**
- Push directly to `main`
- Commit or push without explicit user approval
- Use `--force` or bypass flags without explicit approval
- Merge with failing CI
- Add Claude to attribution or as a contributor in PRs, commits, or descriptions
- Use GitHub Issues or `gh issue` commands — use Linear MCP tools instead
- Check out a stale feature branch without rebasing on latest main first
