# STARTUP — JavaBlocks

Rewritten 2026-11-09 by autopilot run 4 (report:
`tools/autopilot/runs/0004.md`). The owner works on more than one
machine, so `origin/main` may trail the checkout you are in: run
`git log origin/main..main` before trusting what follows.

You are in **JavaBlocks**, the owner's block-programming library for
multithreaded pipelines. Read `design/decision-log.md` first: **J1** is
the charter, **J2** the pom, **J3** the characterization tests, **J4**
the stop() fix, **J5–J11** the seven cleanups, **J12** the assembly's
ruled form. Driven runs work within
`tools/autopilot/DELEGATION.md`.

## State

- `v0.2.1` (tag) is the `v0.2.0` code pinned by 34 tests. Since then
  `main` carries the eight ruled cleanups (J4–J11): stop() always
  returns; map `getSize` locked; buffer capacity below 1 refused;
  ping-pong timestamps `volatile`; distributor forwards from a snapshot
  outside its lock; delays return early on interrupt and keep the flag;
  `Reporter.setDefault{Info,Debug,Error}Reporter` (null = silent);
  ping-pong drops a peer silent > 3 × timeout. **43 tests**, green,
  about 6 s. No release since `v0.2.1`; releases are Tog's call.
- **Goal 1 is done** (`tools/autopilot/GOALS.md`): every known wart is
  fixed. Every cleanup record is `status: done`.
- **Goal 2's form is ruled and logged (J12):** a Design as plain data
  (nodes with id, kind, settings; wires port to port), a typed Java
  builder on top, building through the existing factories with no block
  changed. The form record is `status: done`.
- **Goal 2 waits on three records (run 4), all `proposed`; do not
  propose them again:**
  - `decisions/2026-11-09-what-should-the-pipeline-builder-s-public-names.md`
    — recommends package `design`, `Design`/`Designer`/`Pipeline`, ports
    `PushOut`/`PushIn`/`PullOut`/`PullIn`, handles `<Kind>Node`, kind
    names after the factory methods.
  - `decisions/2026-11-09-how-should-a-pipeline-design-name-the.md` —
    recommends names in the Design, objects in a `Catalogue`, the
    builder auto-naming lambdas `<node>.function`.
  - `decisions/2026-11-09-how-should-the-map-block-s-per-key-handler.md`
    — recommends a nested Design copied per key (`perKey`, `entry()`,
    `hostSinkPerKey`); the factory-object option is the honest smaller
    alternative.

## Scope for the next session

1. **If the names and catalogue records are ruled:** write the next J
   entry, then build the core test-first (Design, builder, Pipeline),
   starting with the kinds FirstTest and the UDP examples need; use
   exactly the ruled names. The map node waits for its own ruling.
2. **If not ruled:** housekeeping only; end blocked on those records.

## Facts not worth rediscovering

- `Input`/`Output` are named from the wire's side: implementing
  `Output<E>` makes a block a *sink*. The viewer flips the words; the
  adapter does the flipping, and the library keeps its names.
- Only `ThreadBlock` owns a thread. See `knowledge/thread-model/`,
  which now also holds where map, distributor and ping-pong lock.
- Every JavaBlocks connection is a push (a slot holding an Output gets
  a block that is an Output) or a pull (the same for Input); today's
  setters already refuse other pairings at compile time. The ping-pong
  block takes its sender in its constructor, not a setter, so the
  builder must build the sender first. `keyboard` and `console` are
  program-wide singletons.
- An `assemblies/` package already exists (`Assemblies.getBufferedSocketBlock`,
  a ready-made composite); the new builder must not collide with it.
- The ping-pong keep-alive needs ticks at most about 2 × timeout apart:
  the drop check runs before the ping (J11).
- The default reporters print `INFO:`/`DEBUG:` on every put. Tests that
  must stay quiet use `setDebugReporter(getTrashcanBlock())`; the
  reporter test that changes the global defaults restores them in
  `finally` — don't turn on parallel test execution without fixing that.
- The stack traces in `mvn verify` output (InterruptedException,
  "downstream rejects 1") are expected: tests provoke them.
- Mutation drill for new pins: break one line, run the one test class,
  expect red, then `git checkout -- src/main`. A race is pinned by
  repeating it.
- Untestable guards, recorded as such: J4's `workerThread == worker`
  check and rethrow during a pending stop; J11's "-1 = not yet
  timestamped" guard.
- The viewer-side reading of this code is
  `wiring-playground/design/javablocks-map-block.md` (not on every
  machine). Runs never touch that repo.

## Protocol

House standard: log before build; one thing at a time; the owner
designs the outside, the steward builds the inside; prove every test
can fail.
