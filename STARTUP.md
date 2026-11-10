# STARTUP — JavaBlocks

Rewritten 2026-11-10 by autopilot run 5 (report:
`tools/autopilot/runs/0005.md`). The owner works on more than one
machine, so `origin/main` may trail the checkout you are in: run
`git log origin/main..main` before trusting what follows.

You are in **JavaBlocks**, the owner's block-programming library for
multithreaded pipelines. Read `design/decision-log.md` first: **J1** is
the charter, **J2** the pom, **J3** the characterization tests, **J4**
the stop() fix, **J5–J11** the seven cleanups, **J12** the assembly's
ruled form, **J13** the Design core. Driven runs work within
`tools/autopilot/DELEGATION.md`.

## State

- `v0.2.1` (tag) is the `v0.2.0` code pinned by 34 tests. `main` carries
  the eight ruled cleanups (J4–J11) and now the Design core (J13).
  **73 tests**, green, about 8 s: 44 characterization tests (the J3–J11
  ones plus `BufferedSocketBlockCharacterizationTest`) and 29 in
  `src/test/.../design/`. No release since `v0.2.1`; releases are Tog's.
- **Goal 1 is done.** **Goal 2 is under way:** package
  `org.abstractica.javablocks.design` has `Design` (plain data:
  `Node`/`Wire`/`Port`), `Catalogue`, the typed `Designer`, `Pipeline`,
  ports `PushOut`/`PushIn`/`PullOut`/`PullIn`, one `<Kind>Node` handle per
  kind, and 14 kinds: constant, variable, ifBlock, buffer, thread,
  distributor, pull, push, trashcan, keyboard, console, hostSink,
  hostSource, pingPong. The kind table (`Kinds.java`) is the one
  definition that checking (`PipelineBuilder.check`) and building read.
- **Not built yet:** the `map` node (ruled: a nested Design per key —
  `decisions/2026-11-09-how-should-the-map-block-s-per-key-handler.md`
  stays `ruled` until it is built) and the `udpSocket` node.
- **Waiting on Tog (both `proposed`; do not propose them again):**
  - `decisions/2026-11-10-should-a-thread-that-is-waiting-on.md`: an
    interrupt never wakes a UDP socket read, so a socket-reading thread
    never stops and `BufferedSocketBlock.stop()` hangs (pinned).
    Recommends the socket block polling every 100 ms. Blocks the
    `udpSocket` node and so the UDP examples.
  - `decisions/2026-11-10-how-should-a-host-write-the-item.md`: Java
    needs `d.<String>thread("driver")` for thread/buffer/distributor/
    trashcan. Recommends keeping that. Blocks nothing.

## Scope for the next session

1. If the socket record is ruled: carry it out as one wart (J entry,
   flip the pin, prove it fails), then add the `udpSocket` node (Pipeline
   closes sockets after stopping threads, as ruled) and rebuild the
   `examples/udp` pipelines through the Design, with tests.
2. Otherwise, within the rulings already made: rebuild FirstTest through
   the Design (a test with a host source standing in for the keyboard
   exists: `PipelineTest.aPingPongBlockAnswersPingsAndPingsAQuietPeer`),
   then build the map node (nested Design per key) test-first, a J entry
   first.
3. Small items found in review, any run: `Pipeline.stop()` abandons the
   remaining threads if interrupted; `Kinds.isPlain` refuses Float/
   BigDecimal and `capacity` must be Integer (matters for a Design read
   from JSON); `KindSpec.reporting` is unused.

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
  a ready-made composite); the builder lives in `design/` beside it.
- `DatagramSocket.receive` ignores interrupts on platform threads (Java
  25, probed); only closing the socket wakes it.
- Designer gotcha: a node with no typed argument infers `<Object>`; write
  `d.<String>thread(...)`. Design errors are `IllegalArgumentException`s
  listing every problem.
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
