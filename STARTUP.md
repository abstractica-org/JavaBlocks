# STARTUP — JavaBlocks

Written 2026-08-26 at onboarding; rewritten 2026-09-23 by the hibernate
of the J2/J3 session, which also resolved the diverged working copy
(local J2/J3 rebased onto the server's September ceremony commits and
pushed). The owner works on more than one machine, so `origin/main` may
trail the checkout you are in: run `git log origin/main..main` before
trusting State and Scope below.

You are in **JavaBlocks**, the owner's block-programming library for
multithreaded pipelines. Read `design/decision-log.md` **J1** (charter),
**J2** (pom), **J3** (characterization tests) first.

## State

- `main` = `72dd3ca`, tagged **`v0.2.1`**: the `v0.2.0` code
  (`3076b2a`) byte-for-byte, now pinned by **34 green characterization
  tests** (~9 s, three consecutive runs verified). Pom is real
  (`org.abstractica:javablocks:0.3.0-SNAPSHOT`, Java 25, JUnit 5).
- Tests live one class per block family under
  `src/test/java/org/abstractica/javablocks/blocks/basic/`, helpers in
  `testsupport/` (`Sink`, `BlockingSink`, `Async` — daemon threads +
  join-based blocking assertions). `junit-platform.properties` sets a
  30 s default timeout so a hang fails instead of stalling the build.
  Every test was proven able to fail via 13 one-line mutations (J3).
- The settled thread model and the v0.2.0 `stop()` contract are
  crystallized at `knowledge/thread-model/`.
- **Waiting on the owner:**
  `decisions/2026-09-11-the-diverged-working-copy.md` was carried out
  by the 2026-09-23 hibernate exactly as its recommendation says
  (local commits read, rebased onto main, session logs committed) but
  is still `status: proposed` — it needs the owner's ruling/close.
  And `decisions/2026-09-23-fix-thread-block-stop.md`
  — whether to fix `stop()` (it deadlocks or never returns unless the
  worker is parked in an interruptible `get()`; the timer idiom is
  unstoppable). Recommendation: fix minimally, as the first ruled
  cleanup step.

## Scope — in this order, ONE thing at a time

1. **If the stop() record is ruled:** carry out the ruling — one
   commit, the three deadlock/hang tests flipped in the same commit,
   log entry (J4) saying why. If deferred or refused, skip.
2. **Remaining ruled cleanups, one step each** (J3 list + CLAUDE.md):
   handler age-out; `volatile` handler timestamps; `getSize` under the
   lock; reporter singletons (a test-time reporter belongs there —
   rejected alternative in J3); `DelayFunction` interrupt-swallowing;
   capacity-0 buffer; distributor forwarding under its monitor. Each:
   propose, owner rules, do, log.
3. **The declarative assembly — owner designs the outside.** Propose
   the builder's shape (nodes with kinds, typed ports with push/pull
   mode, wires, the map block's keyed factory as a filler); owner
   rules; build against the existing factories. Acceptance: FirstTest's
   ping-pong pipeline and the `examples/udp` pipelines rebuilt through
   it.
4. **The contract adapter** — only once `wiring-playground` has the
   graph contract. Thin.

## Facts not worth rediscovering

- `Input`/`Output` are named from the wire's side: implementing
  `Output<E>` makes a block a *sink*. The viewer flips the words; the
  adapter does the flipping, the library keeps its names.
- Only `ThreadBlock` owns a thread; thread domains are derivable from
  node kinds plus wiring — see `knowledge/thread-model/`.
- `stop()` completes only when the worker is parked in an
  *interruptible* `get()`; otherwise 5 s wait-rounds or a join-holding-
  the-monitor deadlock, and `isRunning()` blocks behind it. A
  downstream RuntimeException is the only way to end a Constant-fed
  worker in v0.2.0. Details and tests: `knowledge/thread-model/`.
- The default reporters print `INFO:`/`DEBUG:` per put; tests that
  must stay quiet use `setDebugReporter(getTrashcanBlock())` — do not
  silence globally (J3, rejected alternative).
- Two J3 tests deliberately leave a dead/deadlocked non-daemon worker
  behind; surefire exits anyway. Don't "fix" the tests.
- Mutation drill for new pins: one-line break via script, run the one
  test class, expect red, `git checkout -- src/main`.
- The viewer-side reading of this code is
  `wiring-playground/design/javablocks-map-block.md` + assessment.

## Protocol

House standard: log before build, ONE thing at a time with the owner,
owner designs outsides / steward builds insides, prove the test can fail.
