# JavaBlocks — decision log

Entries are numbered **J1, J2, …** and never renumbered.

## J1 — Charter: a data-driven assembly, built on a pinned base (owner, 2026-08-26)

Ruled in a `wiring-playground` session, on the viewer-side report
`wiring-playground/design/javablocks-map-block.md` and its assessment:

- **JavaBlocks is a target, not only a test.** The viewer must be able
  to design JavaBlocks pipelines, not merely draw them. That needs the
  wiring reified: today it is imperative code (constructors and setters
  handing references), so there is no Design object to read. The
  remedy is a **data-driven assembly** — a declarative way to build a
  pipeline through the factories that already exist (`BasicBlocks`,
  `UDPBlocks`, the handler factories). The owner calls it the next
  natural step for the project.
- **Two layers, in this order.** First a **Java-side declarative
  assembly** (a builder — the reified design, typed, standing on its
  own). Then a thin **adapter from the viewer's graph contract** to
  that builder. The contract is written in `wiring-playground` jointly
  with `wiring`; the adapter waits for it, the builder does not. This
  keeps the viewer's contract authored by its owner, not by its third
  consumer.
- **The base is pinned before it is changed.** `v0.2.0` (`3076b2a`) is
  the reference. Characterization tests describe what it does — warts
  included — before any block is edited; the pom fix is the sole thing
  that precedes them, because nothing runs without it. Every later
  behaviour change is a deliberate change to a test.
- **Cleanup is separate from the feature**, one logged step per fix.
- **Independent of `wiring`.** Two hosts of one viewer, never
  dependent on each other.

*Rejected:* rendering existing programs by **introspection** (reflection
over fields, or a tracing `Output`/`Input` wrapper). It recovers
instances, never the design — holes and exemplars are invisible, and the
map block's handler wiring is buried in factory bodies. **Named revival
trigger:** a need to visualise a running JavaBlocks program that was
*not* built through the assembly.

*Risk named:* the code has no tests, and its value is concurrency
semantics an agent can break while "tidying". The pin-first rule and
the separate-cleanup rule are the mitigation; they are in `CLAUDE.md`.

## J2 — The pom: `org.abstractica:javablocks:0.3.0-SNAPSHOT`, Java 25, JUnit 5 (owner, 2026-08-26)

The template pom (`AbstracticaProjectTemplate`, Java 22 source/target,
no dependencies) replaced by the real one — the sole change J1 allows
before the characterization tests, because nothing runs without it.

- **Coordinates** `org.abstractica:javablocks`, version `0.3.0-SNAPSHOT`
  (the first version after the pinned `v0.2.0`; the tests will tag
  `v0.2.1` on the way).
- **Java 25**, pinned via `maven.compiler.release` — the house standard,
  ruled by the owner; the same form as `sandbox` and `ai-persona`.
- **JUnit 5** (`junit-bom` 5.11.4, `junit-jupiter` test-scoped) so the
  next step has a test runner. Surefire 3.5.2 and compiler 3.13.0
  pinned in `pluginManagement`.
- `distributionManagement` points at GitHub Packages for
  `abstractica-org/JavaBlocks`, as the siblings do. Nothing is
  published yet.

No `src/` change. `mvn clean verify` is green on JDK 25.0.4 / Maven
3.8.7; the jar is `javablocks-0.3.0-SNAPSHOT.jar`, class files at
major 69. The `java-library` profile claim is now verified against the
build (java, maven, junit all exercised).

## J3 — Characterization tests pin `v0.2.0`; tagged `v0.2.1` (steward, 2026-08-26)

34 JUnit 5 tests under `src/test/java/org/abstractica/javablocks/blocks/basic/`,
one class per block family, describing what `v0.2.0` does — warts
included, none fixed. `src/main` is byte-identical to `3076b2a`. Every
pinned behaviour was proven able to fail: thirteen one-line mutations
of the implementation (put never blocks, stop ignores the in-flight
put, the post-put `notifyAll` lock removed, distributor reversed and
unlocked, if-check removed, a new map handler per put, handler put
under the map lock, delay as a single sleep, …) each turned exactly
the tests that claim that behaviour red, and were reverted with git.
Three consecutive full runs green, ~9 s. Support classes live in
`testsupport/` (`Sink`, `BlockingSink`, `Async`); a
`junit-platform.properties` sets a 30 s default timeout so a hang
fails instead of stalling the build.

**What is pinned** (the list in `STARTUP.md` step 2, plus what the
tests surfaced):

- *Buffer:* bounded FIFO; put blocks when full, get when empty, both
  throw `InterruptedException` when interrupted; capacity 1 is
  `SingleBufferBlockImpl` with the same semantics; wraps the ring.
- *ThreadBlock:* `start` throws unless both sides are set; hand-off
  runs on one worker thread per `start`; restartable; setters throw
  while running; `stop` returns only after an in-flight `put`.
- *Distributor:* forwards in registration order on the caller's
  thread; `getOutputs` is a copy; `put` holds the monitor.
- *If:* routes by predicate; throws `"Block not fully connected!"`
  when half-connected, even if the connected side would be taken.
- *Map:* one handler per new key, created on the caller's thread and
  reused; `getAllHandlers` is a snapshot; nothing destroyed unasked;
  `removeHandler` destroys through the factory and a later put
  creates afresh; `handler.put` runs outside the map lock.
- *Function blocks:* push/pull apply on the caller's thread and throw
  `IllegalStateException` unconnected; variable is a lossy register;
  constant never changes; trashcan swallows; Keyboard/Console are
  singletons; `Constant + ThreadBlock + DelayFunction` ticks.

**Warts the tests surfaced, beyond the ones already listed in
`CLAUDE.md`** — each is now a candidate for a ruled cleanup step
(J1: one at a time, never in passing):

1. **`stop()` can complete only when the worker is parked inside an
   *interruptible* `get()`** (a buffer). `stop()` is `synchronized`
   and `join()`s while holding the monitor; the worker needs that
   monitor for its post-put `synchronized(this){ notifyAll(); }`.
   With any input whose `get()` returns instead of throwing on
   interrupt — `ConstantBlock`, a `DelayFunction` on either side, the
   UDP socket — `stop()` either keeps seeing `doingOutput == true`
   and re-waits in 5 s rounds, or wins the microsecond window and
   deadlocks in `join()`; every later `isRunning()` then blocks
   too. **The library's own timer idiom (`FirstTest`) is therefore
   unstoppable.** Found because the first test run hung; pinned by
   `stopDeadlocksWithAWorkerWhoseGetSurvivesTheInterrupt` and the two
   timer tests.
2. A `RuntimeException` from downstream kills the worker (rethrown,
   uncaught) while `isRunning()` stays true and `stop()` never
   returns (`doingOutput` is never reset).
3. `DelayFunction` swallows interrupts and always sleeps the full
   delay — which is what turns wart 1 into a deadlock for timers.
4. A capacity-0 buffer is constructible and blocks every `put`.
5. `DistributorBlock.put` holds the block's monitor while forwarding,
   so a slow output blocks `addOutput`/`removeOutput`.

The tests that pin warts 1–2 deliberately leave a deadlocked or dead
worker behind (non-daemon; surefire exits regardless). When a wart is
fixed, its test changes in the same commit — that is the point of the
pin.

*Rejected:* silencing the reporters globally for the test run. The
default reporters printing `INFO:`/`DEBUG:` to stdout on every put is
`v0.2.0` behaviour; the tests that must stay quiet use the block's own
`setDebugReporter`. **Named revival trigger:** the reporter-singleton
cleanup step (already on the list), which is where a test-time
reporter belongs.
