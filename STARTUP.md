# STARTUP — JavaBlocks

Written 2026-08-26 at onboarding; revised 2026-09-10 by the foundation's
Phase 7 conventions pass (tree fields, `CLAUDE.md` shape — no code work).
It describes `origin/main`. The owner works on more than one machine
(laptop, prodesk), so `origin/main` may be behind the checkout you are
in: run `git log origin/main..main` and read `design/decision-log.md`
past **J1** before trusting State and Scope below.

You are in **JavaBlocks**, the owner's block-programming library for
multithreaded pipelines, onboarded 2026-08-26. Read `design/decision-log.md`
**J1** first — it is the charter for everything below.

## State

- `src/` is exactly `v0.2.0` (`3076b2a`, 2025-01-17), handwritten by the
  owner alone. ~1,900 lines, no tests, no dependencies, no CI.
- `pom.xml` is still the template (`artifactId:
  AbstracticaProjectTemplate`, Java 22). It has never been installed
  under its own name.
- `FirstTest` (`blocks/connection/firsttest/`) is the only runnable
  check — a `main`, run by hand.
- The viewer-side reading of this code is
  `wiring-playground/design/javablocks-map-block.md` (the model, the map
  block, eleven gaps against the viewer's vocabulary, eleven open
  questions) and `javablocks-map-block-assessment.md`. Read the report's
  §1–2 before touching the concurrency; it is the best description of
  the thread model that exists.

## Scope — in this order, ONE thing at a time

1. **Fix the pom.** Real `artifactId` (`javablocks`), group
   `org.abstractica`, version `0.3.0-SNAPSHOT`, Java pinned (owner
   chooses; 25 is the house standard), JUnit 5 for tests. No `src/`
   change. Build green. Then re-test the profile: `java-library`
   declares JUnit 5, which the template pom lacks (OPEN
   `7.javablocks.profile` in the 2026-09-10 conventions PR).
2. **Characterization tests** against `v0.2.0` behaviour, warts
   included. At least: bounded buffer blocks the producer when full and
   the consumer when empty; `ThreadBlock` hands off pull→push on its own
   thread and `stop()` returns only after an in-flight `put`;
   `DistributorBlock` forwards to every output in order on the caller's
   thread; `IfBlock` throws when half-connected; `MapBlock` creates one
   handler per new key on the caller's thread and never destroys one
   unasked; `ConstantBlock + ThreadBlock + delay` ticks. Prove each can
   fail. Tag the result (`v0.2.1`).
3. **Cleanup, one ruled step each** — propose, owner rules, do, log:
   handler age-out; `volatile` timestamps; `getSize` under the lock;
   reporter singletons; anything the tests surfaced.
4. **The declarative assembly — owner designs the outside.** Propose the
   builder's shape (what a pipeline looks like as data: nodes with
   kinds, typed ports with push/pull mode, wires, the map block's keyed
   factory as a filler); the owner rules; then build it against the
   factories. `FirstTest`'s ping-pong pipeline and the `examples/udp`
   pipelines rebuilt through it are the acceptance tests.
5. **The contract adapter** — only once `wiring-playground` has written
   the graph contract. Thin.

## Facts not worth rediscovering

- `Input`/`Output` are named from the wire's side: implementing
  `Output<E>` makes a block a *sink*. The viewer flips the words; the
  adapter does the flipping, the library keeps its names.
- Only `ThreadBlock` owns a thread; everything else runs on whoever
  calls it. Thread domains are derivable from node kinds plus wiring.
- Push chains are driven from upstream, pull chains from downstream; the
  same `Function` can be wrapped either way.
- `Keyboard` and `Console` are process-wide singletons.
- Nothing in the repo calls `MapBlock.removeHandler`.

## Protocol

House standard: log before build, ONE thing at a time with the owner,
owner designs outsides / steward builds insides, prove the test can fail.
