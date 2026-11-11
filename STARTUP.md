# STARTUP — JavaBlocks

Rewritten 2026-11-11 by autopilot run 8 (report:
`tools/autopilot/runs/0008.md`). The owner works on more than one
machine, so `origin/main` may trail the checkout you are in: run
`git log origin/main..main` before trusting what follows.

You are in **JavaBlocks**, the owner's block-programming library for
multithreaded pipelines. Read `design/decision-log.md` first: **J1** is
the charter, **J2** the pom, **J3** the characterization tests, **J4**
the stop() fix, **J5–J11** the seven cleanups, **J12** the assembly's
ruled form, **J13** the Design core, **J14** the map node, **J15**
FirstTest through the Design, **J16** the socket read answers an
interrupt, **J17** the `udpSocket` node, **J18** the UDP examples
through the Design. Driven runs work within
`tools/autopilot/DELEGATION.md`; how a run ends is in the run brief.

## State

- `v0.2.1` (tag) is the `v0.2.0` code pinned by 34 tests. `main` carries
  the cleanups (J4–J11, J16) and the whole declarative assembly
  (J12–J18). **127 tests**, green, about 15 s: 49 characterization
  tests, 68 in `src/test/.../design/` (29 core, 21 `MapNodeTest`, 18
  `UdpSocketNodeTest`), 3 `FirstTestTest`, 7 `UdpExamplesTest`. No
  release since `v0.2.1`; releases are Tog's.
- **Goals 1 and 2 are done.** Goal 2's "done" (FirstTest and the
  `examples/udp` pipelines rebuilt through the assembly, with tests) is
  met. **Goal 3** (the viewer adapter) starts only once
  `wiring-playground` has the graph contract; runs never touch that
  repo.
- **Waiting on Tog (`proposed`; do not propose it again):**
  `decisions/2026-11-11-should-a-socket-block-s-getport-give-the.md`: a
  socket block's `getPort()` always answers -1 (it asks for the
  connected port). Pinned by two tests. Recommends answering the
  listening port. Blocks nothing.
- Run 8's report asks Tog to look at two consequences of his rulings:
  a pipeline whose sockets `stop()` closed refuses `start()`, and so
  `KeyboardToUDP` / `UDPToConsole` cannot be restarted after `stop()`.
  If he objects, that is a new ruling on `Pipeline.stop()`.

## Scope for the next session

1. If the `getPort()` record is ruled: carry it out as one wart (a J
   entry, the two pins flipped in the same commit, proven able to fail).
2. Otherwise nothing toward the goals is open to a run. Small items,
   each its own commit, and each behaviour change needs a decision
   record first, never a quiet fix:
   - `UDPSocketBlockImpl`'s constructor leaves its socket bound if
     `InetAddress.getLocalHost()` (or `setSoTimeout`) throws: a wart,
     needs a record.
   - `Pipeline.start()`/`stop()` are not locked against each other;
     `Pipeline.stop()` abandons the rest (maps, sockets) if interrupted:
     records, if worth asking.
   - `Kinds.isPlain` refuses Float/BigDecimal and `capacity`, `port`,
     `maxPacketSize` must be Integer (matters once Designs come from
     JSON, goal 3); `KindSpec.reporting` is unused.
   - In `MapNodeTest`, two tests lean on short sleeps and
     `aThreadFeedingTheMapStopsBeforeTheCopies` does not catch a swapped
     stop order (its neighbour does): changing an existing test needs
     Tog.
   - No test pins that the Pipeline copies the catalogue at build; a
     `PerKeyDesigner` kept after `map` returns is silently ignored.
   - `CLAUDE.md`'s status line still says 43 tests (not in the
     delegation's housekeeping list; say so to Tog or leave).
   If none is worth a run, end blocked on the `getPort()` record.

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
  25, probed); the socket block polls every 100 ms (J16). On a virtual
  thread Java closes the socket on interrupt instead.
- The Pipeline closes only sockets it opened for `udpSocket` nodes,
  tracked by node kind in `PipelineBuilder`, never by Java type: a
  host's own socket block wired in as a host source or sink (a
  `BufferedSocketBlock` is a `UDPSocketBlock` too) stays open. Run 8's
  first review rejected the change for getting this wrong.
- Socket tests find a free port by opening `new DatagramSocket(0)`,
  reading its port and closing it (`getPort()` cannot tell), and check
  "closed" by binding the port again.
- Designer gotcha: a node with no typed argument infers `<Object>`; write
  `d.<String>thread(...)` (ruled to stay, 2026-11-11). Design errors are
  `IllegalArgumentException`s listing every problem; nested ones are
  prefixed `in map '<id>':`; a port that cannot be opened is an
  `UncheckedIOException` naming the node.
- Changing an existing test needs Tog (delegation), except the pin a
  ruling flips, in the fix's own commit.
- The ping-pong keep-alive needs ticks at most about 2 × timeout apart.
- Stopping FirstTest's pipeline takes up to its 10 s tick: `stop()` waits
  for the in-flight delayed put (J4). Tests use `FirstTest.design(...)`
  with short timings; the UDP examples have `design(...)` methods the
  same way.
- The default reporters print `INFO:`/`DEBUG:` on every put; quiet tests
  use `setDebugReporter(getTrashcanBlock())`. The reporter test changes
  global defaults and restores them in `finally`; one example test swaps
  `System.out` the same way — no parallel tests.
- The stack traces in `mvn verify` output are expected: tests provoke
  them.
- Mutation drill: break one line, run the one test class, expect red,
  restore. A script applying a list of (file, old, new, test class)
  replacements is fast (run 8 drilled about 50 lines this way). With
  `-Dtest=` join classes with commas, not `+` (a `+` matches nothing and
  the build stays green: check the "Tests run" count). A race is pinned
  by forcing each step with latches and thread states, not sleeps.
- The viewer-side reading of this code is
  `wiring-playground/design/javablocks-map-block.md` (not on every
  machine). Runs never touch that repo.

## Protocol

House standard: log before build; one thing at a time; the owner
designs the outside, the steward builds the inside; prove every test
can fail.
