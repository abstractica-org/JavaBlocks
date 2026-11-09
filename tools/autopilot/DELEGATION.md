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
- Merge its own pull request into `main` once a fresh-context review
  approves it (Tog, 2026-11-09).

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

_Approved by Tog, 2026-11-09, as drafted in decisions/2026-11-09-approve-the-autopilot-delegation-for-javablocks.md._
