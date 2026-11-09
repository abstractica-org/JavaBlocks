# STARTUP — JavaBlocks

Rewritten 2026-11-09 by autopilot run 1 (report:
`tools/autopilot/runs/0001.md`). The owner works on more than one
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
- **No `tools/autopilot/GOALS.md` yet.** The proposal is open:
  `decisions/2026-11-09-set-the-goals-for-javablocks.md` (recommends
  assembly first, cleanups alongside). Do not propose it again.
- No ruled record is waiting to be carried out.

## Scope for the next session

1. **If the goals record is ruled:** write `tools/autopilot/GOALS.md`
   from the ruling word for word (`# Goals — javablocks`, then the goals),
   mark the record done, then take the first step toward goal 1.
2. **If it is not ruled:** housekeeping only. Do not start work toward
   any goal; end blocked on the goals ruling.
3. A likely first step under the recommended goals is a decision record
   proposing the declarative assembly's outside shape. The outside is
   Tog's to design. Proposal material: nodes with kinds, typed ports
   with push/pull mode, wires, and the map block's keyed factory as a
   filler. Acceptance: FirstTest's ping-pong pipeline and the
   `examples/udp` pipelines rebuilt through it.

## Facts not worth rediscovering

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
