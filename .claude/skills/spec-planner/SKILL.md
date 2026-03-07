---
name: spec-planner
description: Dialogue-driven spec development through skeptical questioning. Use when planning a feature, designing a system, or writing an implementation spec.
argument-hint: "[feature or problem description]"
user-invocable: true
allowed-tools: Read, Grep, Glob, Bash, WebFetch, WebSearch, Task
---

Develop an implementation-ready spec through iterative dialogue and skeptical questioning.

**User request:** $ARGUMENTS

## Core Principles

- Plans emerge from discussion, not assumptions
- Default skepticism toward requirements until verified
- Consider downstream maintenance and second-order effects
- Simplest viable solution first

## Workflow

Work through these phases in order. Do NOT skip CLARIFY.

### Phase 1: CLARIFY (mandatory)

Before any planning, ask at least one round of clarifying questions. Probe with 2+ questions targeting:

- **Scope** — What's in, what's explicitly out?
- **Motivation** — What user problem does this solve?
- **Constraints** — What existing systems does this touch?
- **Success metrics** — How do we know it works?

Challenge assumptions. If something seems obvious, question why.

### Phase 2: DISCOVER

Explore the existing codebase and external context:

1. Use Glob/Grep/Read to understand current architecture
2. Use the **librarian** agent to research unfamiliar libraries or patterns
3. Use the **oracle** agent for architecture decisions if trade-offs are non-obvious
4. Map what exists, what needs changing, what's new

### Phase 3: DRAFT

1. Define the problem in one sentence
2. Inventory hard constraints
3. Map solution space from simplest to most complex
4. Analyze trade-offs between approaches
5. Produce a clear recommendation with reasoning

Include effort estimate: **S** (<1h), **M** (1-3h), **L** (1-2d), **XL** (>2d)

### Phase 4: REFINE

Run a completeness checklist. ALL must pass:

- [ ] Scope is bounded — explicit "not included" list
- [ ] Ambiguities resolved — no "TBD" or "maybe"
- [ ] Acceptance criteria are testable
- [ ] Dependencies are ordered
- [ ] Types/interfaces are defined
- [ ] Effort estimated
- [ ] Risks identified with mitigations
- [ ] Open questions have owners

### Phase 5: DONE

Write the final spec to `specs/<feature-name>.md` (kebab-case). Include:

1. **Summary**
2. **Discovery notes**
3. **Recommendation**
4. **Key trade-offs**
5. **Deliverables** — ordered list with dependencies
6. **Open questions**
