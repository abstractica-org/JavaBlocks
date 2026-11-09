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
