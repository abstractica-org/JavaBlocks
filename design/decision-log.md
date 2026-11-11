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

## J4 — `ThreadBlock.stop()` fixed minimally: every ThreadBlock is stoppable (owner ruling, 2026-11-09; carried out by autopilot run 1)

Carries out `decisions/2026-09-23-fix-thread-block-stop.md`, ruled
**fix stop() minimally** (Tog, 2026-11-09). J3 warts 1 and 2 are fixed;
wart 3 (`DelayFunction` swallows interrupts) is deliberately left for its
own step.

**What changed, and only in `ThreadBlockImpl`:**

- `stop()` no longer holds the block's monitor while it `join()`s. It
  takes the monitor only to raise a `stopping` flag, wait until no
  `put` is in flight, and interrupt; then it releases the monitor and
  joins. The worker's post-put handshake can therefore never deadlock
  against it, and `isRunning()` never blocks behind a stop.
- The worker's loop checks `stopping` after each completed `put`, so a
  worker fed by an input whose `get()` survives the interrupt
  (`ConstantBlock`, `DelayFunction`, the socket) finishes that get and
  delivers that one item, then exits. Stop is prompt, not instant.
- An interrupt aimed at the `get()` is cleared (under the monitor)
  before the item is put, so the item in hand is delivered rather than
  lost to a stale interrupt.
- When the worker ends for any reason — stopped, or killed by a
  downstream exception — it resets `running` (and the in-flight flag)
  before it dies. `isRunning()` tells the truth and `stop()` on a dead
  worker returns at once. A downstream exception still kills the worker
  and is still rethrown wrapped in a `RuntimeException`, as in v0.2.0.

- `stop()` stops the run it was called on: it notes the worker before
  it waits, and afterwards interrupts and joins only that worker. If
  `start()` begins a new run while `stop()` waits, the new run is left
  alone and `stop()` still returns (found by the fresh review: re-reading
  the worker after the wait made `stop()` join the new worker forever).
- As in v0.2.0, a downstream exception from the in-flight `put` while
  `stop()` waits is still rethrown; only a failure after `stop()`'s
  interrupt is swallowed. (Not pinned by a test: an uncaught exception
  on the internal worker thread is not observable without a global
  handler.)

**Side effects on shutdown, accepted:** while a stop is pending,
`running` stays true until the worker exits, so a `start()` in that
window is a no-op and `setInput`/`setOutput` throw
`IllegalStateException`; in v0.2.0 they queued behind the monitor
`stop()` held and then went through. Calling `stop()` from the worker
thread itself still hangs, as it did in v0.2.0.

**Unchanged:** the steady-state data path (`output.put(input.get())`
on one worker thread), back-pressure, restartability, the setters'
rules, and `stop()` returning only after an in-flight `put` completes
with the item delivered — all still pinned by the untouched J3 tests.

**Tests flipped in the same commit** (each now pins termination instead
of the hang; the old assertions are gone because the behaviour is):
`ThreadBlockCharacterizationTest.stopDeadlocksWithAWorkerWhoseGetSurvivesTheInterrupt`
→ `stopWaitsForAnUninterruptibleGetThenDeliversTheItemAndReturns`;
`downstreamExceptionKillsTheWorkerButTheBlockStillReportsRunningAndStopHangs`
→ `downstreamExceptionKillsTheWorkerAndTheBlockReportsStopped`; and the
two timer tests in `FunctionBlocksCharacterizationTest`
(`…PushSideDelayTicksAndCanBeStopped`,
`…PullSideDelay…TicksOnceMoreAfterStopThenStops`). The ruling's record
counted three; it is four because the downstream-exception test pins
wart 2, which the ruled option ("reset state when the worker dies")
also fixes. Each flipped test was proven able to fail: against the
v0.2.0 `ThreadBlockImpl`, exactly those four go red.

**Two tests added**: `theInterruptAimedAtTheGetDoesNotAbortThePutThatFollows`,
pinning the interrupt-clearing line (no existing test caught its
removal), and `aStartDuringAPendingStopDoesNotHangTheStop` (50 trials of
the race; it fails against the first draft of this fix). The suite is
now **36** tests. Four one-line mutations of the
new code each turned tests red and were reverted: interrupt not
cleared (the new test), `join()` back inside the monitor, `running`
not reset when the worker ends, the loop ignoring `stopping`.

## J5 — `MapBlock.getSize()` reads under the map lock (owner ruling, 2026-11-09; carried out by autopilot run 3)

Carries out `decisions/2026-11-09-should-the-map-block-lock-its-handler.md`,
ruled **lock the read**. Known wart "unsynchronized `MapBlock.getSize`":
every other access to the handler map holds `synchronized (map)`;
`getSize()` read the `HashMap` without it, so a reader on another thread
had no guarantee of seeing a current count.

**Change:** `getSize()` takes the same lock. Visible consequence: it now
waits while the factory creates a handler (creation runs under the lock),
as `getAllHandlers()` already did.

**Test:** new `MapBlockCharacterizationTest.getSizeWaitsForAHandlerBeingCreated`
— the factory blocks inside `createHandlerBlockFor`, and `getSize()` on
another thread stays blocked until it is released. No existing test pinned
the unlocked read (a stale read cannot be observed reliably), so the pin
is the wait; it fails with the lock removed.

## J6 — a buffer of capacity below 1 is refused (owner ruling, 2026-11-09; carried out by autopilot run 3)

Carries out `decisions/2026-11-09-should-a-buffer-of-size-0-be.md`,
ruled **refuse capacity below 1**. J3 wart: `getBufferBlock(0)` built a
buffer that was full from the start, so the first `put` hung until
interrupted; a negative capacity failed with `NegativeArraySizeException`.

**Change:** `BufferBlockImpl`'s constructor throws
`IllegalArgumentException("buffer capacity must be at least 1, was N")`
for any capacity below 1. `BasicBlocks.getBufferBlock` reaches it for
every capacity other than 1 (which builds the single-slot variant), so
one check covers the factory, and `BufferedSocketBlockImpl`, which passes
its caller's size through, now fails at construction too.

**Test flipped in the same commit:**
`BufferBlockCharacterizationTest.capacityZeroBufferBlocksEveryPut` →
`capacityBelowOneIsRefusedAtConstruction` (0 and -1 both throw
`IllegalArgumentException`). The old assertion is gone because the
behaviour is. Proven able to fail: without the check, the new test is red.

## J7 — the ping-pong handler's timestamps are `volatile` (owner ruling, 2026-11-09; carried out by autopilot run 3)

Carries out `decisions/2026-11-09-should-the-ping-pong-handler-s-timestamps-be-made.md`,
ruled **mark both fields volatile**. Known wart "non-volatile handler
timestamps": `PingPongHandlerImpl.lastMessageReceivedTime` is written by
the thread delivering messages and read by the timer thread;
`lastPingSentTime` is public through the handler interface. As plain
`long`s, Java promised neither visibility of the latest write nor an
untorn read on a 32-bit JVM.

**Change:** both fields are `volatile`. Nothing else.

**Test:** new `PingPongCharacterizationTest.handlerTimestampsAreVolatile`,
a structural check by reflection — a stale read cannot be provoked
reliably on a 64-bit JVM, so the record ruled that the declaration is
what gets pinned. Proven able to fail: removing either keyword turns it red.

## J8 — the distributor forwards from a snapshot, outside its lock (owner ruling, 2026-11-09; carried out by autopilot run 3)

Carries out `decisions/2026-11-09-should-the-distributor-stop-holding-its-lock.md`,
ruled **forward from a snapshot, outside the lock**. J3 wart: `put` held
the block's monitor for the whole forwarding loop, so one slow output
blocked `addOutput`/`removeOutput` and every other producer, and a thread
draining that slow output deadlocked if it touched the output list.

**Change:** the outputs live in a `CopyOnWriteArrayList`; `put` is no
longer `synchronized` and loops over the list's snapshot as of the moment
it starts. `addOutput`, `removeOutput` and `getOutputs` are unchanged.
This is the rule `MapBlock` already follows: never call out while
holding your own lock.

**Accepted consequences (the record named them):** two producers putting
at once are no longer serialized, so different outputs may see their
messages in different orders; an output added during a forward does not
get that message; an output removed during a forward still gets that one
message. A host that needs one order everywhere puts a buffer and one
thread in front of the distributor. Single-producer order is unchanged
(still pinned by `forwardsToEveryOutputInRegistrationOrderOnTheCallersThread`).

**Test flipped in the same commit:**
`DistributorBlockCharacterizationTest.putHoldsTheMonitorWhileForwarding` →
`addAndRemoveProceedWhileAPutForwardsFromItsSnapshot`: with a put blocked
in a slow output, `addOutput` and `removeOutput` return at once; the
added output does not receive the in-flight message, the removed one
does. Proven able to fail against the v0.2.0 distributor.

## J9 — the delay functions return early on an interrupt and keep the flag (owner ruling, 2026-11-09; carried out by autopilot run 3)

Carries out `decisions/2026-11-09-what-should-the-delay-functions-do-when.md`,
ruled **return early and keep the flag**. J3 wart 3: `DelayFunction` and
`getRandomDelayFunction` caught `InterruptedException`, threw it away
(clearing the thread's interrupted flag) and slept out the full delay.

**Change, in both `DelayFunctionImpl` and `RandomDelayFunctionImpl`:**
on an interrupt they re-set the thread's interrupted flag and return the
item at once. This is Java's convention for code that cannot throw
`InterruptedException` (they are plain `Function`s). A delay called with
the flag already set therefore returns at once.

**Effect on the timer idiom:** with the delay on the thread block's
input side, `stop()` no longer waits out the remaining delay. The thread
block clears the flag before its put (J4), so the tick in hand is still
delivered exactly once and
`constantPlusPullSideDelayPlusThreadTicksOnceMoreAfterStopThenStops`
stays green unchanged except its javadoc. With the delay on the output
side (FirstTest), stop() already waits for the in-flight put by design
(J4) and the interrupt never reaches the delay; nothing changes there.

**Accepted consequence (the record named it):** if some other caller's
thread is interrupted mid-delay, the item goes downstream early with the
flag set, and a full buffer there refuses it with `InterruptedException`
instead of waiting.

**Tests flipped in the same commit:**
`FunctionBlocksCharacterizationTest.delayFunctionAbsorbsInterruptsAndDelaysTheFullTime`
→ `delayFunctionReturnsEarlyOnAnInterruptAndKeepsTheFlag`, and
`randomDelayFunctionAbsorbsInterruptsAndDelaysTheFullTime` (pinned in the
commit before this one, since the random delay had no test) →
`randomDelayFunctionReturnsEarlyOnAnInterruptAndKeepsTheFlag`. Each
proven able to fail: against the old code (late return), and against a
fix that returns early but drops the flag.

## J10 — a host can set the default reporters; output unchanged (owner ruling, 2026-11-09; carried out by autopilot run 3)

Carries out `decisions/2026-11-09-how-should-a-host-control-the-blocks.md`,
ruled **settable defaults, output unchanged**. Known wart "the reporter
singletons": every block takes its INFO, DEBUG and ERROR reporters from
`Reporter`'s hard-wired defaults when it is built, and the only way to
quiet or redirect the library was block by block.

**Change — public API addition, as ruled:** three static setters on
`Reporter`: `setDefaultInfoReporter`, `setDefaultDebugReporter`,
`setDefaultErrorReporter`, each taking an `Output<String>`; `null` means
silent (blocks already skip a null reporter). Blocks built after the call
pick up the new default; blocks built before keep what they have (not
retroactive, as the record said). The default fields are `volatile`, so
a setter called on one thread is seen by blocks built on another.
Nothing a host sees changes unless it calls a setter: the defaults still
print exactly as before. The names are the run's choice within the
ruling (they mirror the existing `defaultInfoReporter` fields and the
existing `getInfoReporter(Block)` lookups, which are unchanged).

Still open, as the record says, for later rulings: whether debug should
be off by default, and passing reporters in at build time (belongs with
the declarative assembly).

**Tests:** `ReporterCharacterizationTest.everyNewBlockStartsWithTheSharedDefaultReporters`
pinned today's defaults in the commit before this one and stays green;
new `aSetDefaultReachesBlocksBuiltAfterItAndNotBefore` (a recording
default, then `null` for silent, restoring the originals after). Proven
able to fail with a setter that does not store its argument.

*Rejected:* using the setters to quiet the whole test run (J3's rejected
alternative). Tests that must stay quiet still use the block's own
setter; the new test restores the defaults it changes.

## J11 — the ping-pong block drops a peer silent for more than three timeouts (owner ruling, 2026-11-09; carried out by autopilot run 3)

Carries out `decisions/2026-11-09-should-the-ping-pong-block-drop-peers-that.md`,
ruled **drop after three silent timeouts, fixed**. Known wart "handler
set never shrinks": `MapBlock` already had `removeHandler`, but
`PingPongBlockImpl` (whose map block is private) never called it, so
every peer that ever sent a message kept its handler forever and a
departed peer was pinged forever.

**Change, only in `PingPongBlockImpl`'s timer connector:** on each tick,
a handler whose last message arrived more than `3 × timeout` ago is
removed through `mapBlock.removeHandler(key)`, which calls the factory's
`destroyHandlerBlock` and so the protocol's `destroyHandler`; it is not
pinged on that tick. Below that, the ping rule is unchanged (pinged when
quiet longer than the timeout and no ping went out within it). The
factor 3 is fixed, as ruled; making it a host's choice is public API,
left for the declarative assembly. A peer that comes back gets a fresh
handler from its next message (pinned map-block behaviour).

A handler whose arrival time is still unset (`-1`: created by `MapBlock`
under its lock, its first `put` not yet run) is never treated as silent,
so a tick racing a new peer's first message cannot drop it. (Without
this guard, `curTime - (-1)` reads as decades of silence.) Today such a
handler can draw one spurious ping; that is unchanged.

**Accepted consequences (the record named them):** a peer silent that
long but still alive loses its handler's state; a message arriving at the
moment of removal may reach the handler being destroyed. Found in review:
the drop is checked before the ping, so a host whose ticks come more than
3 × timeout apart drops a quiet peer without ever pinging it; the
keep-alive needs ticks no further apart than about 2 × timeout (FirstTest
ticks every 2 × timeout).

**Test flipped in the same commit:**
`PingPongCharacterizationTest.aSilentPeerKeepsItsHandlerAndIsPingedForever`
(pinned in the commit before, since ping-pong had no behaviour tests) →
`aSilentPeerIsPingedThenDroppedAfterThreeTimeouts`: the silent peer gets
at least one ping, then its handler is destroyed exactly once and the
pings stop; a peer that keeps talking is never pinged or dropped; the
silent peer's next message creates a fresh handler. Proven able to fail
against the old timer connector. That timing test cannot tell a factor of
3 from 2 or 4, so `theDropFactorIsThree` pins the constant structurally
(found in review). The `-1` guard has no test that fails
without it: the window lies between `MapBlock` releasing its lock and the
new handler's first `put`, and nothing outside the block can hold a
thread there (the same limit J4 recorded for two of its guards).

## J12 — the declarative assembly's form: a Design as data, a typed Java builder on top (owner ruling, 2026-11-09; recorded by autopilot run 4)

Carries out `decisions/2026-11-09-what-form-should-the-declarative-pipeline-assembly.md`,
ruled **design as data, with a typed Java builder on top**. Goal 2 has
its outside form:

- **The core is plain data.** A Design is nodes (id, kind, settings)
  and wires (port to port). It can be read back, checked and drawn; it
  is the "reified design" J1 asks for, and the viewer adapter (goal 3)
  only has to turn the viewer's graph into one.
- **A typed Java builder sits on top** and does nothing the core cannot:
  every builder call records nodes and wires in a Design (the
  foundation's plumbing-and-porcelain pattern, so the layers cannot
  drift). Java authors keep compile-time types on their connections.
- **Building goes through the existing factories** (`BasicBlocks`,
  `UDPBlocks`, the ping-pong block). No block changes; the 43 tests keep
  guarding the behaviour, and a pipeline built through the assembly
  behaves as the same pipeline wired by hand.
- **Build-time checks** (an unwired slot, a wire no thread drives) are
  the builder's to decide while building, as the record said; each one
  turns a failure inside a worker thread into an error at build time.

**Not yet ruled, so nothing is built:** the record names three
follow-ups, each a decision record of its own, proposed by run 4 —
the public names (package, types, methods, the kinds' names); how a
Design names the Java functions and host objects it uses (the
catalogue); and the map block's per-key handler slot. Every public name
is the owner's (delegation). Code starts with the next J entry, after
the names and the catalogue are ruled; the map node waits for its own
ruling.

## J13 — the Design core: names, catalogue and map slot ruled; the core built without the map and socket nodes (owner rulings, 2026-11-09; carried out by autopilot run 5)

Carries out three rulings on J12's follow-ups:

- `decisions/2026-11-09-what-should-the-pipeline-builder-s-public-names.md`,
  ruled **Design, Designer, Pipeline, with push and pull ports**: package
  `org.abstractica.javablocks.design`; `Design` (records `Design.Node(id,
  kind, settings)`, `Design.Wire(from, to)`, `Design.Port(node, name)`);
  the typed builder `Designer`; `Pipeline`, a `ThreadControl` with a typed
  `block(node)`; ports `PushOut`/`PushIn`/`PullOut`/`PullIn`, and
  `wire` accepts only push→push and pull→pull; one handle per kind named
  `<Kind>Node`; optional `info()`/`debug()`/`error()` reporter ports on
  every node that is a block, except the program-wide keyboard and console;
  kind names after the factory methods, plus `pingPong`, `hostSink`,
  `hostSource`.
- `decisions/2026-11-09-how-should-a-pipeline-design-name-the.md`, ruled
  **names in the Design, objects in a catalogue; the builder fills both**:
  `Catalogue`, `Designer.catalogue()`, auto-names `<node>.function` and so
  on, the named form `push(id, name, fn)` (and pull, ifBlock, pingPong);
  a constant or variable value that is not plain data is stored by name.
  A Design whose names the catalogue lacks is refused at build time,
  naming every missing entry.
- `decisions/2026-11-09-how-should-the-map-block-s-per-key-handler.md`,
  ruled **a nested Design, copied once per key** (`perKey`, `entry()`,
  `hostSinkPerKey`). Built last within goal 2, as the record said; not in
  this step.

**This step builds the core** (Design, Catalogue, Designer, Pipeline, the
four port types, the handles) and every kind except two: `map` (its own
step, as ruled) and `udpSocket`, held back by a wart found while
building: interrupting a thread does not wake a `DatagramSocket.receive`
(probed on Java 25), only closing the socket does. So a `ThreadBlock`
reading a socket never stops, and the ruled pipeline order "stop the
threads, then close the sockets" would hang; the existing
`BufferedSocketBlock.stop()` hangs the same way whenever no packet
arrives. That is a newly found wart, so it goes to its own decision record
and nothing in the blocks changes here.

**Inside decisions** (the steward's, the outside being ruled):

- Settings hold only plain data: strings, numbers, booleans. A catalogue
  reference is a string under a key that means "a catalogue name":
  `function`, `predicate`, `protocol`, `object`, and `valueName` for a
  constant or variable whose value is not plain (a plain one is `value`);
  `capacity` for a buffer. No extra public type for references.
- Port names in the data are the handle methods' names: `in`, `out`,
  `whenTrue`, `whenFalse`, `timer`, `send`, `info`, `debug`, `error`. A
  wire runs in the direction data flows: a push wire from the slot that
  holds an Output to the block that is one, a pull wire from the block that
  is an Input to the slot that holds one.
- The handles share one public base, `NodeHandle<B>`, where `B` is the
  built object's type; `Pipeline.block(NodeHandle<B>)` returns a `B`, and
  `Pipeline.block(String id)` serves a Design read from elsewhere.
- Errors in a Design are `IllegalArgumentException`s that list every
  problem at once: duplicate ids, an unknown kind, node or port, a wire
  that is neither push→push nor pull→pull (data from elsewhere has no
  compiler), a single slot wired twice (only the distributor's `out` takes
  many), a missing catalogue name or one of the wrong type, a required slot
  left unwired (a thread's `in` and `out`, a push block's `out`, a pull
  block's `in`, an if block's `whenTrue` and `whenFalse`, a ping-pong
  block's `send`), and ping-pong blocks whose senders form a cycle. Each
  of these failed today inside a worker thread or as a null pointer; "a
  wire no thread drives" is not checked yet.
- Building constructs every node through the existing factories, ping-pong
  blocks after whatever their `send` is wired to (its constructor takes the
  sender), then applies the wires through the existing setters. Unwired
  reporter ports keep the library defaults (J10).
- `Pipeline.start()` starts every thread block in node order; `stop()`
  stops each in node order (each stop as J4 pins it); `isRunning()` is
  true while any of them runs.

**Public details the run chose, open to the owner** (none was in a
ruling's sketch; each is small to change before a release, and the
first two shape what the viewer adapter will read and write):

- the settings keys above (`value`, `valueName`, `function`, `predicate`,
  `protocol`, `object`, `capacity`); `capacity` must be an `Integer`, so a
  Design parsed from JSON as `Long` is refused today;
- `Catalogue`'s methods: `put(name, object)` (chainable; refuses a name
  that already stands for another object), `contains`, `get` (refuses an
  absent name), `names`, and a copy constructor;
- the base class `NodeHandle<B>`, `Pipeline.block(String id)`, and
  `port()` on the four port types.

**Found in review: a type has to be written out.** The ruled sketch's
`var driver = d.thread("driver"); d.wire(driver.out(), ping.in());` does
not compile: Java infers `ThreadNode<Object>`, which cannot push into a
`PushIn<String>`. Nodes whose methods take no typed argument (`thread`,
`buffer`, `distributor`, `trashcan`) need `d.<String>thread("driver")`.
Accepting the sketch as written would mean giving up the push/pull type
safety the names ruling chose the four port types for, so the run kept
the explicit type and put the alternatives to the owner in
`decisions/2026-11-10-how-should-a-host-write-the-item.md`.

**Tests:** a new `design` test package: the Designer writes the data the
rulings describe; pipelines built from a Design behave as the same
pipelines wired by hand (functions, buffers, if, distributor, variable,
pull chains, ping-pong, reporter ports); a Design written as plain data
builds with a catalogue; every build-time refusal above. No existing test
changes, and `src/main` outside the new package is untouched.

## J14 — the map node: a nested Design, copied once per key (owner ruling, 2026-11-09; carried out by autopilot run 6)

Carries out `decisions/2026-11-09-how-should-the-map-block-s-per-key-handler.md`,
ruled **a nested Design, copied once per key**, with the record's own
words: `d.map(id, keyFn, perKey -> { ... })`, `perKey.entry()` (this
key's messages) and `perKey.hostSinkPerKey(id, key -> output)` (a host
object made from the key; kind `hostSinkPerKey` in the data). The map is
built through the existing `BasicBlocks.getMapBlock` with a handler
factory of the design package's own; `MapBlock` and every other block
stay as they are.

**Inside decisions** (the outside being ruled):

- **In the data**, a `map` node has two settings: `function`, the
  catalogue name of the key function (as for push and pull), and
  `perKey`, the nested `Design` itself — the one setting that is not a
  string, number or boolean, since it is data too. Inside it, `entry()`
  is a node `entry` of kind `entry` with one push port `out`, which must
  be wired; `entry` and `hostSinkPerKey` are allowed only inside a map's
  nested Design, and refused anywhere else.
- **Names.** Node ids inside a nested Design are its own: `show` inside
  may also be an id outside. The nested Designer writes into a copy of
  the outer catalogue, merged back only once the whole map is accepted
  (a refused map adds none of its own names; what its lambda wrote
  through the outer Designer stays), and prefixes its
  automatic names with the map's id (`peers.show.object`); a name given
  explicitly is used as given. A map whose lambda takes the map's own id,
  or a catalogue name, from the outer Designer is refused before anything
  merges. An outer id that itself contains a dot
  (`peers.show`) can collide with such a name; the collision is refused
  at once, when the map is written.
- **Checking.** A nested Design is checked in full when the outer one is
  built — every problem listed together, prefixed `in map 'peers':` —
  so a broken handler design is refused before anything runs, not when
  the first message arrives. For that, a buffer's capacity below 1
  inside a nested Design is refused by the check, as the buffer itself
  refuses it (J6); outside a map the buffer's own refusal at build time
  stays as it was. The other kinds have no setting their factory
  refuses. Maps may nest. The pipeline keeps its own copy of the
  catalogue, since a map reads it on every new key.
- **A copy per key.** On the first message with an unseen key, the map
  (under its lock, on the putting thread, as `MapBlock` does today)
  builds a fresh copy of the nested Design with that key, and starts its
  thread blocks if the pipeline is running. Its handler forwards each
  message to whatever `entry` is wired to. When a host calls
  `removeHandler(key)`, the copy's threads are stopped (each as J4 pins
  it) and a later message with that key builds a new copy. The copy
  stays tracked until its stop has finished, so a removal by an
  interrupted thread is finished by the pipeline's next `stop()`, and a
  removed copy never starts again. The ruling also says a removed copy's
  sockets are closed: there is no socket node yet (it waits on the
  socket record), so that comes with it.
- **Starting and stopping the pipeline.** `Pipeline.start()` starts the
  live copies, then its own thread blocks; `stop()` stops its own thread
  blocks first (so they stop feeding the maps), then every live copy. A
  copy made while the pipeline is stopped — a host putting straight into
  the map — is built but not started, and starts with the next
  `start()`; copies that ran start again. `isRunning()` counts the
  copies' threads too. A copy is stopped outside the factory's lock.
- **Errors at copy time.** A `hostSinkPerKey` function that throws, or
  returns something that is not an `Output`, fails the `put` that
  carried the new key, naming the node; the map keeps no handler for it.
  When that put comes from one of the pipeline's thread blocks, the
  thread dies of the exception, as it does today for any runtime
  exception thrown downstream of it.
- **Known limits, from the thread model, not fixed here.** Removing a
  handler stops its copy's threads as `ThreadBlock.stop()` does, so, as
  with blocks wired by hand: a removal from one of the copy's own
  threads hangs (a thread cannot stop itself); a thread blocked putting
  into the removed copy's full buffer stays blocked, since nothing
  drains it (pinned), and so does a later `Pipeline.stop()` if that
  thread is the pipeline's; and after the pipeline stops, a host thread
  blocked putting into a copy's full buffer stays blocked. The `map`
  javadoc says so: remove a handler, or stop, when nothing is putting
  into its copy. Draining or discarding a removed copy's buffers would
  change `BufferBlock` and would be a ruling of its own.

**Public details the run chose, open to the owner:** the data shape
above (`perKey` setting, `entry` node and kind, port `entry.out`); the
handle `MapNode<K, E>` (`in()` and the reporter ports); the named form
`map(id, name, keyFn, perKey)`, like push and pull; the nested builder
type `PerKeyDesigner<K, E>`, a `Designer` with `entry()` and
`hostSinkPerKey` added — so `Designer` is no longer `final` but
`sealed`, permitting only `PerKeyDesigner`;
`Pipeline.block(map)` returns the `MapBlock`, whose `getAllHandlers()`
and `removeHandler` give a host each copy's key and its removal, but not
the copy's blocks: nothing inside a copy is reachable through
`Pipeline.block`.

**Tests** (21, each proven able to fail by a one-line mutation): the
Designer writes the nested data, ids inside are their own, a given name
is used as given, a refused map leaves the catalogue as it was, a dotted
id's collision is refused; per-key copies are made once per key, run on
their own threads, are stopped with the pipeline (after the thread
feeding them) and on `removeHandler`, a removal by an interrupted
thread is finished by the pipeline's stop, a copy made while stopped
starts with the pipeline and copies that ran start again; maps nest; a
per-key function returning no Output or throwing fails the put; the
pinned limit above; the build-time refusals for nested Designs,
including a capacity-0 buffer inside a map.
Existing tests unchanged.

## J15 — FirstTest rebuilt through the Design (goal 2; autopilot run 7)

Goal 2 is done when the FirstTest ping-pong pipeline and the
`examples/udp` pipelines are rebuilt through the assembly, with tests
(`tools/autopilot/GOALS.md`). The UDP ones wait on the socket record
(`decisions/2026-11-10-should-a-thread-that-is-waiting-on.md`); FirstTest
needs no socket, so it goes first.

- **What changes.** `FirstTest.main` builds the same pipeline as before —
  keyboard → driver thread → ping-pong block sending through
  `SimpleSender`; constant 5000 ms timeout → timer thread → 10 s delay →
  the ping-pong block's timer — but as a Design: nodes `keyboard`,
  `driver`, `ping`, `sender`, `timeout`, `timer`, `tick`. The wiring is a
  package-private `FirstTest.design(Designer, PullOut<String> commands,
  Output<String> sender, long timeoutMillis, long tickMillis)`, so a test
  can stand a host source in for the keyboard (a program-wide
  singleton) and a sink in for the sender, and tick fast. `main` passes
  the keyboard, a `SimpleSender`, 5000 and 10000, then builds and
  starts.
- **What does not change.** No library code, no public API, no existing
  test. The timings and every block are the ones FirstTest used; the
  ping-pong block still sends through `SimpleSender`, now as a host sink
  wired to its `send` port instead of a constructor argument (the
  builder hands it to the constructor the same way).
- **Stopping it is slow, as the thread model says.** `stop()` waits for
  a thread's in-flight put (J4), and the timer thread spends nearly all
  its time inside the delayed put, so stopping FirstTest's own pipeline
  takes up to its 10 s tick. `main` never stops it; the tests use a
  short tick.
- **Tests.** `FirstTestTest` (3) builds `FirstTest.design` with a host
  source and a sink: a ping from a peer is answered with a pong through
  the sender; with a short timeout and tick, the timer pings a peer
  quiet for longer than the timeout and not before; the pipeline stops
  promptly with a short tick. Proven able to fail by one-line mutations of
  `FirstTest.design`.

## J16 — a socket read answers an interrupt, by checking every 100 ms (owner ruling, 2026-11-11; carried out by autopilot run 8)

Carries out `decisions/2026-11-10-should-a-thread-that-is-waiting-on.md`,
ruled **the socket block wakes up on interrupt, by checking every
100 ms**. The wart (found in J13, pinned then): Java never wakes a
platform thread waiting in `DatagramSocket.receive` when the thread is
interrupted, so a `ThreadBlock` reading a `UDPSocketBlock` never stopped
while no packet arrived, and `BufferedSocketBlock.stop()` hung.

- **What changes.** `UDPSocketBlockImpl` only. The socket gets a 100 ms
  receive timeout (a fixed constant, as the record says, not a setting).
  `get()` checks the calling thread's interrupt flag before each wait
  and throws `InterruptedException` (clearing the flag, as every other
  blocking `get()` does); a wait that times out is simply repeated. So an
  interrupted `get()` ends within about 100 ms, and a packet that
  arrives is returned as before, however long the wait was.
- **What does not change.** The socket is not closed by an interrupt,
  so a stopped reader can be started again. A packet that arrives in the
  same moment as the interrupt is returned, not dropped; the thread block
  delivers it and then stops (J4). `put`, `close`, `getPort`,
  `getAddress`, and a `get()` on a closed socket (a `RuntimeException`
  wrapping the `SocketException`) are as they were.
- **Rejected by the ruling:** closing the socket to stop (a socket error
  in the reader unless timed with a helper thread, no restart, hand-wired
  readers still hang); a `DatagramChannel` that an interrupt closes (no
  restart, a larger rewrite); leaving it.
- **Tests.** The pin in `BufferedSocketBlockCharacterizationTest` is
  flipped in this commit: `stop()` used to be still waiting after half a
  second until another thread closed the socket; now it returns on its
  own, with no packet and no outside close. New
  `UDPSocketBlockCharacterizationTest`: an interrupt ends a waiting
  `get()` with `InterruptedException` and leaves the socket open (the
  next `get()` receives a packet); a `get()` already interrupted when
  called throws at once; a packet arriving after several 100 ms checks is
  still received whole. Each proven able to fail by a one-line mutation.

## J17 — the `udpSocket` node: the Design's last ruled kind (goal 2; autopilot run 8)

The names ruling (J13) lists `udpSocket` among the kinds, gives the
socket handle `in()`/`out()`, and says the Pipeline "starts every thread
block, stops them all, then closes sockets"; the map ruling (J14) says a
removed copy's sockets are closed. The node waited on the socket wart,
fixed in J16. This step builds it.

- **In the data:** kind `udpSocket`, settings `port` and `maxPacketSize`
  (both whole numbers, the arguments of `UDPBlocks.getUDPSocketBlock`),
  ports `in` (packets pushed in are sent) and `out` (pull received
  packets), and the reporter ports. `port` 0 asks the system for a free
  port, as `DatagramSocket` does.
- **In the Designer:** `d.udpSocket(id, port, maxPacketSize)` returns a
  `UdpSocketNode` (`<Kind>Node`, as ruled), a
  `NodeHandle<UDPSocketBlock<DatagramPacket>>`, so
  `pipeline.block(socket).getAddress()` works, the ruling's own example.
- **Building opens the socket**, through the existing factory. What the
  data can show wrong is refused with the other problems before
  anything is opened: a `port` outside 0–65535, a negative
  `maxPacketSize`. A socket that cannot be opened (the port is taken)
  is not a mistake in the Design, so it is an `UncheckedIOException`
  naming the node, not an `IllegalArgumentException`. Whenever building
  fails after a socket was opened, every socket opened so far is closed:
  a refused build leaves no port bound.
- **Stopping closes.** `Pipeline.stop()` stops its thread blocks, then
  the maps' copies, then closes its sockets, as ruled. It closes them
  also when the pipeline was never started (they were opened at build).
  A `stop()` that is interrupted while waiting closes nothing, as it
  already abandons the threads it has not reached; calling `stop()`
  again finishes the job.
- **A closed socket cannot be opened again**, so a pipeline whose
  sockets `stop()` has closed refuses `start()` with an
  `IllegalStateException`, before starting anything; the host builds the
  Design again. (Starting it would only make each socket thread fail
  with a socket error inside its worker.) A pipeline without sockets
  restarts as before. This follows from the ruled "stop closes the
  sockets"; the refusal instead of a failing worker is this step's
  choice.
- **In a map's nested Design** each copy opens its own socket when its
  key first arrives (so a fixed `port` suits one key only; `port` 0 suits
  any number), and the socket is closed when the copy is stopped: on
  `removeHandler`, as the map ruling says, and on the pipeline's `stop()`.
  A copy whose socket cannot be opened fails the put that brought the
  key, like any per-key failure (J14), and leaves no handler. After a
  pipeline `stop()`, a live copy with a closed socket makes `start()`
  refuse like the pipeline's own sockets; removing that copy's handler
  lifts the refusal, and the key's next message builds a fresh copy.
- **No block changes.** `UDPSocketBlock` has no way to ask whether it is
  closed, so the Pipeline remembers that it closed its sockets.
- **Public details the run chose, open to the owner:** the handle's
  name `UdpSocketNode` (by the ruled `<Kind>Node` pattern from the kind
  `udpSocket`; the block's own type is spelled `UDPSocketBlock`); the
  settings' names `port` and `maxPacketSize`; the exception types above;
  the refused restart.
- **Tests** (`UdpSocketNodeTest`, each proven able to fail by a one-line
  mutation): the Designer writes the node as data; a pipeline sends
  through its socket and receives from it; `stop()` closes the socket
  only after the reader's in-flight put, and frees the port; a never
  started pipeline's `stop()` frees the port; a stopped pipeline with a
  socket refuses to start and starts nothing; a port in use is refused
  naming the node and the sockets already opened are closed; a bad
  `port` or `maxPacketSize` is listed with the other problems and opens
  nothing; a map copy's socket is closed on `removeHandler` and on the
  pipeline's stop, a fresh copy opens it again, and a closed live copy
  refuses the restart until its handler is removed; a copy whose socket
  cannot open fails the put and leaves no handler. Existing tests
  unchanged.
