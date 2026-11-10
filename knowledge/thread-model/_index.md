---
type: project-node
summary: "The settled thread model: only ThreadBlock owns a thread, blocking lives in get/put, stop() waits for the in-flight put then exits for every input kind (J4), and where map, distributor and ping-pong lock (J5–J11) — all pinned by tests, and how to pin a blocked thread without a race."
verified: 2026-11-10
related:
  - path: ..
    rationale: The orientation names this node as the thread-domain model, crystallized after J3.
---

# Thread model

Settled by the J3 characterization tests (2026-08-26), the stop()
contract updated by the J4 fix and the J5–J11 cleanups (2026-11-09); each fact below
is pinned by a test that has been proven able to fail. The tests are
the executable form of this node — change them and this text together.

## Thread domains

- **Only `ThreadBlock` owns a thread.** Every other block runs on
  whoever calls it. A pipeline's thread domains are derivable from its
  node kinds plus wiring: a ThreadBlock's thread drives the pull chain
  upstream of it (`get`) and the push chain downstream (`put`).
- **Blocking lives in `get`/`put`**, concretely in the buffers (full →
  put blocks, empty → get blocks, both interruptible), the keyboard,
  and the sockets. `BufferBlock` is the inverse joint of a ThreadBlock:
  push in, pull out, and the only back-pressure point. A buffer of
  capacity below 1 is refused when built (J6); in `v0.2.0` capacity 0
  was accepted and hung the first `put`, and a negative capacity threw
  `NegativeArraySizeException`.
- `DistributorBlock` and `MapBlock` forward on the caller's thread;
  the map creates each handler on the thread of the put that first saw
  the key, and `handler.put` runs outside the map lock, so handlers
  for different keys can execute concurrently.

## The stop() contract (J4, 2026-11-09)

`stop()` takes the block's monitor only to raise a `stopping` flag,
wait until no `put` is in flight, and interrupt the worker; it then
**joins outside the monitor**. The worker checks `stopping` after each
completed `put`, and resets `running` whenever it ends. Pinned in
`ThreadBlockCharacterizationTest` and the timer tests:

- `stop()` returns only after an in-flight `put` completes, and the
  item is delivered, not dropped (unchanged from v0.2.0).
- A worker parked in an *interruptible* `get()` (a buffer) exits at
  once. One inside a `get()` that survives the interrupt
  (`ConstantBlock`) finishes that get, delivers that one item, and
  exits — stop is prompt, not instant. The UDP socket's `get()` is
  worse: an interrupt never wakes `DatagramSocket.receive`, only a
  packet or closing the socket does, so while no packet arrives
  `stop()` waits forever (pinned in
  `BufferedSocketBlockCharacterizationTest`, found 2026-11-10; the fix
  is `decisions/2026-11-10-should-a-thread-that-is-waiting-on.md`). A delay function
  on the input side returns early on the interrupt and keeps the flag
  (J9), so the worker delivers the item in hand at once and exits. The
  timer idiom (`FirstTest`, delay on the output side) is stoppable;
  there stop() waits for the in-flight delayed put.
- The interrupt aimed at the `get()` is cleared before the item is put,
  so a stale interrupt never aborts that put.
- `stop()` stops the run it was called on; a `start()` during a
  pending stop is a no-op (and the setters throw), but one that wins
  the race after the old worker exits starts a new run that `stop()`
  leaves alone. `stop()` from the worker thread itself hangs. A
  `stop()` called by an already interrupted thread still marks the run
  stopping (and interrupts a worker that is not mid-put) before its
  wait or join throws: the worker ends on its own, only the waiting is
  skipped (seen building J14, whose pipeline keeps such a copy tracked).
- `isRunning()` never blocks behind `stop()`, and turns false when the
  worker ends for any reason. A downstream `RuntimeException` still
  kills the worker (rethrown, wrapped — also while a stop waits for that
  put; only a failure after stop's interrupt is swallowed), but the block then reports
  stopped and `stop()` returns at once.

## Where the other blocks lock (J5, J8, J11)

- `MapBlock` takes its lock for every access to the handler map,
  `getSize()` included (J5), and never while a handler's `put` runs.
- `DistributorBlock.put` forwards from a snapshot of its outputs,
  outside its lock (J8): a slow output blocks neither add/remove nor
  other producers; with several producers, outputs may see different
  orders; an output added mid-forward misses that item, one removed
  mid-forward still gets it.
- The ping-pong timer drops a peer silent for more than three timeouts
  through `MapBlock.removeHandler` (J11), before it would ping it, so
  ticks must come no further apart than about 2 × timeout or quiet
  peers are dropped unasked; the handler's timestamps are `volatile` (J7).

**History:** in v0.2.0 `stop()` joined while holding the monitor the
worker needed for its post-put `notifyAll()`, so it completed only when
the worker was parked in an interruptible `get()`; otherwise it
re-waited in 5 s rounds or deadlocked, and a dead worker kept reporting
running. Ruled fixed in `decisions/2026-09-23-fix-thread-block-stop.md`;
the change and its tests are J4 in `design/decision-log.md`.

## Pinning a blocked thread

A test that claims a thread stays blocked forces each step — wait until
the thread is parked in `put` (its state, a latch), then act — rather
than sleeping and hoping. J14's `aThreadPuttingIntoARemovedFullCopyStaysBlocked`
failed about half its runs until rewritten this way. Each pin is then
proven by breaking the code under test one line at a time and watching
a test go red.
