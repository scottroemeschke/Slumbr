---
name: code-review
description: Review current branch changes for bugs, quality, and best practices
argument-hint: "[base-branch]"
user-invocable: true
allowed-tools: Read, Grep, Glob, Bash
---

Review the code changes on the current branch.

## Steps

1. Determine the base branch: use `$ARGUMENTS` if provided, otherwise default to `main`
2. Run `git diff $(git merge-base HEAD <base>)..HEAD` to see all changes
3. For each modified file, read the full file for context — don't review diffs in isolation
4. Delegate the actual review to the **code-reviewer** agent

## Focus Areas (priority order)

1. **Bugs** — logic errors, missing guards, edge cases, security issues
2. **Structure** — does it fit existing patterns? unnecessary complexity?
3. **Performance** — only if obviously problematic

## Output

Provide a summary with:
- File path and line numbers for each finding
- Severity: bug / suggestion / nit
- Concrete fix recommendation where appropriate
