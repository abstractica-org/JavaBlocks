# STARTUP — JavaBlocks

Rewritten 2026-11-09 by autopilot run 2 (report:
`tools/autopilot/runs/0002.md`). The owner works on more than one
machine, so `origin/main` may trail the checkout you are in: run
`git log origin/main..main` before trusting what follows.

You are in **JavaBlocks**, the owner's block-programming library for
multithreaded pipelines. Read `design/decision-log.md` first: **J1** is
the charter, **J2** the pom, **J3** the characterization tests, **J4**
the stop() fix. Driven runs work within `tools/autopilot/DELEGATION.md`.

## State

- `v0.2.1` (tag, `2bf6bb6`) is the `v0.2.0` code pinned by 34 tests.
  Since then `main` carries one ruled cleanup, **J4**: `ThreadBlock.stop()`
  returns for every input kind; it joins outside the monitor, stops only
  the run it was called on, and a dead worker reports not running.
  **36 tests**, green, about 5 s. No release since `v0.2.1`; releases
  are Tog's call.
- `knowledge/thread-model/` holds the current thread model and stop()
  contract.
- **Goals are set** (`tools/autopilot/GOALS.md`, Tog's ruling
  2026-11-09): 1. the seven remaining cleanups, one ruling each; 2. the
  declarative assembly; 3. the viewer adapter. Nothing toward goal 2
  starts before goal 1 is done (every wart fixed or ruled left).
- **Seven cleanup records are open** (`decisions/2026-11-09-*.md`,
  `status: proposed`), one per wart: map `getSize` lock, buffer size 0,
  ping-pong timestamps `volatile`, distributor lock, delay interrupts,
  reporter defaults, ping-pong silent peers. Do not propose them again.

## Scope for the next session

1. **For each cleanup record Tog has ruled** (wakeup lists them under
   "Ruled …, not yet carried out"): carry it out exactly as ruled, one
   wart per commit: a `J` entry in `design/decision-log.md` first, the
   pinning test changed visibly in the same commit, mutation-proved.
   Where the record says the behaviour has no test yet (random delay,
   ping-pong), pin today's behaviour first, then change it. Mark the
   record done.
2. **If none is ruled:** housekeeping only; end blocked on the records.
3. Delay fix note: the javadoc of
   `constantPlusPullSideDelayPlusThreadTicksOnceMoreAfterStopThenStops`
   says the delay "swallows the interrupt"; update it with that fix.

## Facts not worth rediscovering

- Two wart names mislead. "Handler set never shrinks": `MapBlock`
  already has `removeHandler`; the gap is that `PingPongBlockImpl`
  (whose map block is private) never calls it. "Reporter singletons":
  the gap is that a host cannot set the defaults; see the record.
- `Input`/`Output` are named from the wire's side: implementing
  `Output<E>` makes a block a *sink*. The viewer flips the words; the
  adapter does the flipping, and the library keeps its names.
- Only `ThreadBlock` owns a thread. See `knowledge/thread-model/`.
- Stop is prompt, not instant, for timers: `DelayFunction` still
  swallows interrupts (J3 wart 3, unfixed), so the worker finishes the
  delay and delivers one tick first.
- The default reporters print `INFO:`/`DEBUG:` on every put. Tests that
  must stay quiet use `setDebugReporter(getTrashcanBlock())`; do not
  silence the reporters globally (J3, rejected alternative).
- The stack traces in `mvn verify` output (InterruptedException,
  "downstream rejects 1") are expected: tests provoke them.
- Mutation drill for new pins: break one line, run the one test class,
  expect red, then `git checkout -- src/main`. A race is pinned by
  repeating it (see `aStartDuringAPendingStopDoesNotHangTheStop`).
- Two J4 guards have no test that would fail without them: the
  `workerThread == worker` check before stop()'s interrupt, and the
  rethrow during a pending stop. The second can't be observed without a
  global uncaught-exception handler.
- The viewer-side reading of this code is
  `wiring-playground/design/javablocks-map-block.md` plus its assessment.
  Runs never touch that repo.

## Protocol

House standard: log before build; one thing at a time; the owner
designs the outside, the steward builds the inside; prove every test
can fail.
