# Claude Code — JavaBlocks

A block programming approach for multithreaded pipelines — a plain Java library of push/pull/thread/buffer/map blocks wired into dataflow pipelines.

**Status: pinned at `v0.2.0` (`3076b2a`, 2025-01-17), no tests yet,
template pom.** Handwritten by the owner alone; proven in other
projects; onboarded 2026-08-26 to gain a data-driven assembly and to
serve as the second host vocabulary of `webapp-foundation`'s workflow
viewer. Its design record is `design/decision-log.md` (entries **J1…**).

## The rules of this codebase

- **Pin before you change.** The library's value is its concurrency
  semantics — who owns a thread, where `put`/`get` block, what `stop()`
  waits for. Nothing checks them today. Characterization tests that
  describe what `v0.2.0` does, warts included, come before any edit to
  a block; a behaviour change is then a visible change to a test in the
  same commit, with a log entry saying why.
- **Cleanup is its own logged step.** Known warts (handler set never
  shrinks, non-volatile handler timestamps, unsynchronized
  `MapBlock.getSize`, the reporter singletons) are fixed one at a time,
  each ruled by the owner — never "in passing" while building something
  else.
- **Owner designs the outside.** The shape of the declarative assembly
  is the one design decision here; the steward builds insides.
- **Independent of `wiring`.** JavaBlocks and `wiring` are two hosts of
  one viewer. They never depend on each other; that independence is the
  viewer's domain-neutrality proof.
- **The contract adapter comes last.** The JSON loader consumes the
  viewer's graph contract, which is written in `wiring-playground` with
  `wiring`. Until it exists, build the Java-side assembly it will target.
- ONE thing at a time with the owner; log before build.

## Profile note

`java-library` is declared from the pom (Maven, Java) and the ruled
plan: the pom has no JUnit yet — the first session adds it with the
characterization tests. Re-test the profile then.

## Session start and end

Run `/wakeup` at session start and `/hibernate` at the end. They
delegate to `/do wakeup` / `/do hibernate` against the foundation at
`$KNOWLEDGE_FOUNDATION_PATH`. Session transcripts are captured by the
hooks in `.claude/settings.json`.

If `/wakeup` says the env var is unset, add this to your shell
profile and restart your shell:

```bash
export KNOWLEDGE_FOUNDATION_PATH=~/Development/GitHub/knowledge-foundation
```

## Profile

`.profile.yml`: `archetype: link`; java-library, github-vcs; ai-collaboration, software-engineering.
