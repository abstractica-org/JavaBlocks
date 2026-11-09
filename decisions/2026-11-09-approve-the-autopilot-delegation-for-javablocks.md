---
id: 2026-11-09-approve-the-autopilot-delegation-for-javablocks
title: Approve the autopilot delegation for javablocks
status: proposed
stakes: medium
confidence: 80
opened: 2026-11-09
repo: javablocks
by: d6bac86a
recommends: approve-as-drafted
---

## Problem

The foundation can run unattended ("driven") sessions in a project, but only within a written delegation that says what such a run may decide by itself and what must always come back to you. JavaBlocks has none yet, so it stays parked: no driven run will touch it. The migration has drafted one, below, grounded in what this project is — a small library whose value is its tested concurrency behaviour, with the cleanups and the declarative assembly still ahead. Approving it as drafted writes it to tools/autopilot/DELEGATION.md; nothing runs until you do.

### The draft

```markdown
# Delegation — javablocks

JavaBlocks is a plain Java library: other projects compose its jar, and
its worth is its concurrency behaviour — which block owns a thread,
where `put` and `get` block, what `stop()` waits for. The 34
characterization tests are the only thing that checks that behaviour.
What is expensive to undo here is a silent behaviour change that ships
in a published version, or a public API shape that hosts start
building against.

## A driven run may decide on its own

- Housekeeping: the knowledge tree, `README.md` Status, `STARTUP.md`,
  crystallizing session logs, decision-record shape, `kt check` fixes.
- Carry out a ruling already made, exactly as ruled: one wart per
  commit, the tests that pin it changed visibly in the same commit, a
  `J` entry in `design/decision-log.md` first.
- Add characterization tests for behaviour that is not yet pinned,
  each proven able to fail by a one-line mutation, without changing
  `src/main`.
- Write decision records proposing the next cleanup or design step.
- Work on a branch and open a pull request for Tog to review.

## Always goes to Tog

- Any change to what the library does that no ruling covers —
  including every item on the known-wart list.
- The outside shape of the declarative assembly, and any other public
  API (interfaces, factory methods, names).
- Deleting or weakening a test, or marking one as skipped.
- Releases: version numbers, tags, publishing to GitHub Packages.
- The dependency list, the Java version, the build.
- Anything touching `wiring` or `wiring-playground`, or the viewer's
  graph contract.
- Merging to `main`.
```

## Options

### Approve as drafted

**Pros:**
- Driven runs can start on the queued housekeeping and on carrying out rulings as you make them, such as the stop() fix once ruled
- Every behaviour change, API shape, release and merge still comes to you, so nothing expensive to undo happens without you
- Pinning more behaviour with tests is allowed, which only adds safety

**Cons:**
- Runs cost money from your pool
- A run carrying out a ruling still edits the library's most delicate code; you review it only at the pull request

### Keep the project parked

**Pros:**
- No spend and no unattended changes
- Nothing in this project is urgent: the open questions block nothing

**Cons:**
- Rulings you make from the pages (for example on stop()) wait until you open an interactive session
- Housekeeping and session-log crystallizing keep piling up

### Approve with changes

**Pros:**
- You can narrow or widen the list — for example forbid touching src/main at all, or allow tagging snapshot builds

**Cons:**
- One more round before any driven run can start
- The changes have to be written back into the draft by a later session

## Recommendation

Approve as drafted. The draft lets runs do only what is cheap to undo (housekeeping, extra tests, pull requests) or what you have already ruled, and keeps with you everything that would change the library's behaviour or its public face. Parking costs little in a project with nothing urgent, but it means your rulings sit idle. If you would rather runs never touch src/main until the first cleanup has been done with you, choose Approve with changes and say so.

## If nothing is decided

The project stays parked. Nothing breaks: the v0.2.1 code and its tests stand, and interactive sessions work as before. Rulings made from the pages wait for one.
