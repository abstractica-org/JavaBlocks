# STARTUP — JavaBlocks

Rewritten 2026-11-10 by autopilot run 6 (report:
`tools/autopilot/runs/0006.md`). The owner works on more than one
machine, so `origin/main` may trail the checkout you are in: run
`git log origin/main..main` before trusting what follows.

You are in **JavaBlocks**, the owner's block-programming library for
multithreaded pipelines. Read `design/decision-log.md` first: **J1** is
the charter, **J2** the pom, **J3** the characterization tests, **J4**
the stop() fix, **J5–J11** the seven cleanups, **J12** the assembly's
ruled form, **J13** the Design core, **J14** the map node. Driven runs
work within `tools/autopilot/DELEGATION.md`.

## State

- `v0.2.1` (tag) is the `v0.2.0` code pinned by 34 tests. `main` carries
  the eight ruled cleanups (J4–J11), the Design core (J13) and the map
  node (J14). **94 tests**, green, about 10 s: 44 characterization tests
  and 50 in `src/test/.../design/` (29 core, 21 `MapNodeTest`). No
  release since `v0.2.1`; releases are Tog's.
- **Goal 1 is done.** **Goal 2 is under way:** package
  `org.abstractica.javablocks.design` (see `knowledge/design/`) has every
  ruled kind but `udpSocket`. The map node is `d.map(id, keyFn, perKey ->
  {...})` with `perKey.entry()` and `perKey.hostSinkPerKey(...)`; the
  nested Design is checked with the outer one and copied per key by
  `PerKeyCopies` (the map's handler factory). Its known limits (a putter
  into a removed full copy stays blocked; a copy cannot remove itself)
  are in J14 and the `map` javadoc.
- **Waiting on Tog (both `proposed`; do not propose them again):**
  - `decisions/2026-11-10-should-a-thread-that-is-waiting-on.md`: an
    interrupt never wakes a UDP socket read, so a socket-reading thread
    never stops and `BufferedSocketBlock.stop()` hangs (pinned).
    Recommends polling every 100 ms. Blocks the `udpSocket` node, the UDP
    examples, and closing a removed map copy's sockets.
  - `decisions/2026-11-10-how-should-a-host-write-the-item.md`: Java
    needs `d.<String>thread("driver")`. Recommends keeping that. Blocks
    nothing.

## Scope for the next session

1. If the socket record is ruled: carry it out as one wart (J entry,
   flip the pin, prove it fails), then the `udpSocket` node (Pipeline
   closes sockets after stopping threads; a map copy's sockets close on
   removal, as the map ruling says), then the `examples/udp` pipelines
   through the Design, with tests.
2. Otherwise: rebuild FirstTest through the Design (a J entry first; a
   test with a host source standing in for the keyboard exists:
   `PipelineTest.aPingPongBlockAnswersPingsAndPingsAQuietPeer`).
3. Small items found in review, any run: `Pipeline.stop()` abandons the
   rest (maps included) if interrupted; `Kinds.isPlain` refuses Float/
   BigDecimal and `capacity` must be Integer (matters for JSON);
   `KindSpec.reporting` is unused; in `MapNodeTest`, two tests lean on
   short sleeps (500 ms, 100 ms) and `aThreadFeedingTheMapStopsBeforeTheCopies`
   does not catch a swapped stop order (its neighbour does); a
   `PerKeyDesigner` kept after `map` returns is silently ignored.

## Facts not worth rediscovering

- `Input`/`Output` are named from the wire's side: implementing
  `Output<E>` makes a block a *sink*. The viewer flips the words; the
  adapter does the flipping, and the library keeps its names.
- Only `ThreadBlock` owns a thread. See `knowledge/thread-model/`.
- Every JavaBlocks connection is a push or a pull; the setters refuse
  other pairings at compile time. The ping-pong block takes its sender
  in its constructor, so the builder builds the sender first. `keyboard`
  and `console` are program-wide singletons.
- `DatagramSocket.receive` ignores interrupts on platform threads (Java
  25, probed); only closing the socket wakes it.
- Designer gotcha: a node with no typed argument infers `<Object>`; write
  `d.<String>thread(...)`. Design errors are `IllegalArgumentException`s
  listing every problem; nested ones are prefixed `in map '<id>':`.
- Changing an existing test needs Tog (delegation). Run 6 kept
  `DesignCheckTest.aFactoryRefusalNamesTheNode` intact by refusing a
  capacity-0 buffer early only inside a map's nested Design.
- The ping-pong keep-alive needs ticks at most about 2 × timeout apart.
- The default reporters print `INFO:`/`DEBUG:` on every put; quiet tests
  use `setDebugReporter(getTrashcanBlock())`. The reporter test changes
  global defaults and restores them in `finally` — no parallel tests.
- The stack traces in `mvn verify` output are expected: tests provoke
  them.
- Mutation drill: break one line, run the one test class, expect red,
  restore. A script applying a list of (file, old, new) replacements and
  restoring each file is fast (run 6 drilled 19 lines this way). A race
  is pinned by forcing each step with latches and thread states, not
  sleeps.
- The viewer-side reading of this code is
  `wiring-playground/design/javablocks-map-block.md` (not on every
  machine). Runs never touch that repo.

## Protocol

House standard: log before build; one thing at a time; the owner
designs the outside, the steward builds the inside; prove every test
can fail.
