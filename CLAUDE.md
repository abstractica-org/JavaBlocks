# Claude Code — JavaBlocks

A block programming approach for multithreaded pipelines — a plain Java
library of push/pull/thread/buffer/map blocks wired into dataflow
pipelines.

**Status (verified 2026-09-23):** `v0.2.1` — the `v0.2.0` code
(`3076b2a`, 2025-01-17), byte for byte, pinned by 34 characterization
tests (J3) on the real pom (J2:
`org.abstractica:javablocks:0.3.0-SNAPSHOT`, Java 25, JUnit 5). No CI.
The owner works on several machines, so `origin/main` may trail your
checkout — check `git log origin/main..main` first.

## Where things are

- `design/decision-log.md` — the design record, entries **J1…**; J1 is
  the charter.
- `src/main/java/org/abstractica/javablocks/` — the code:
  `blocks/basic/` interfaces, `blocks/basic/impl/` implementations,
  `blocks/connection/firsttest/FirstTest` the only runnable check.
- `knowledge/_index.md` — orientation; loads every session.
- `STARTUP.md` — the handoff.

## The rules of this codebase

- **Pin before you change.** The characterization tests under
  `src/test/` describe what `v0.2.0` does, warts included — the
  library's value is its concurrency semantics (who owns a thread,
  where `put`/`get` block, what `stop()` waits for), and nothing else
  checks them. A behaviour change is a visible test change in the same
  commit, with a log entry saying why. Every test was proven able to
  fail (J3) — keep it that way.
- **Cleanup is its own logged step.** The known warts — handler set
  never shrinks, non-volatile handler timestamps, unsynchronized
  `MapBlock.getSize`, the reporter singletons; and from J3: `stop()`
  deadlocks or never returns unless the worker is parked in an
  interruptible `get()` (the timer idiom is unstoppable), a downstream
  exception kills the worker while `isRunning()` stays true,
  `DelayFunction` swallows interrupts, capacity-0 buffers block every
  put, the distributor forwards under its monitor — are fixed one at a time,
  each ruled by the owner, never in passing; a fix buried in a feature
  commit cannot be reverted alone.
- **Owner designs the outside.** The declarative assembly's shape is the
  one design decision here; propose, the owner rules, then build the
  insides.
- **Stay independent of `wiring`.** Two hosts of one viewer, never
  depending on each other — that independence is the viewer's
  domain-neutrality proof.
- **The contract adapter comes last.** The viewer's graph contract is
  written in `wiring-playground` with `wiring`; until it exists, build
  the Java-side assembly it will target — the contract stays authored by
  its owner.
- **Log before you build.** Every step gets a `J` entry in
  `design/decision-log.md` before code — the only record of why `v0.2.0`
  changed.

## Session start and end

Run `/wakeup` at session start and `/hibernate` at the end. They
delegate to `/do wakeup` / `/do hibernate` against the foundation at
`$KNOWLEDGE_FOUNDATION_PATH`. The hooks in `.claude/settings.json`
inject the session pack (`kt wakeup`) at `SessionStart` and capture
the session transcript.

If `/wakeup` says the env var is unset, add this to your shell
profile and restart your shell:

```bash
export KNOWLEDGE_FOUNDATION_PATH=~/Development/GitHub/knowledge-foundation
```

## Profile

`.profile.yml`: `archetype: link`; java-library, github-vcs; ai-collaboration, software-engineering.
