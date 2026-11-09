---
type: project-node
summary: "The settled thread-domain model: only ThreadBlock owns a thread, blocking lives in get/put, and stop() waits for the in-flight put then exits the worker for every input kind (J4; v0.2.0 deadlocked) — pinned by the characterization tests."
verified: 2026-11-09
related:
  - path: ..
    rationale: The orientation names this node as the thread-domain model, crystallized after J3.
---

# Thread model

Settled by the J3 characterization tests (2026-08-26), the stop()
contract updated by the J4 fix (2026-11-09); each fact below
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
  push in, pull out, and the only back-pressure point.
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
  (`ConstantBlock`, `DelayFunction` on either side, the UDP socket)
  finishes that get, delivers that one item, and exits — stop is
  prompt, not instant. The timer idiom (`FirstTest`) is stoppable.
- The interrupt aimed at the `get()` is cleared before the item is put,
  so a stale interrupt never aborts that put.
- `isRunning()` never blocks behind `stop()`, and turns false when the
  worker ends for any reason. A downstream `RuntimeException` still
  kills the worker (rethrown, wrapped), but the block then reports
  stopped and `stop()` returns at once.

**Still open:** `DelayFunction` swallows interrupts and always sleeps
the full delay (J3 wart 3) — the reason stop is prompt rather than
instant for timers. Its own ruled step, if ever.

**History:** in v0.2.0 `stop()` joined while holding the monitor the
worker needed for its post-put `notifyAll()`, so it completed only when
the worker was parked in an interruptible `get()`; otherwise it
re-waited in 5 s rounds or deadlocked, and a dead worker kept reporting
running. Ruled fixed in `decisions/2026-09-23-fix-thread-block-stop.md`;
the change and its tests are J4 in `design/decision-log.md`.
