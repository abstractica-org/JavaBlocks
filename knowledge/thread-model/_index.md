---
type: project-node
summary: "The settled thread-domain model: only ThreadBlock owns a thread, blocking lives in get/put, and stop() completes only when the worker is parked in an interruptible get() — pinned by the J3 tests."
related:
  - path: ..
    rationale: The orientation names this node as the thread-domain model, crystallized after J3.
---

# Thread model

Settled by the J3 characterization tests (2026-08-26); each fact below
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

## The stop() contract (v0.2.0, warts pinned as-is)

`stop()` is `synchronized`, waits `while (doingOutput) wait(5000)`,
then interrupts and `join()`s **still holding the block's monitor** —
which the worker needs for its post-put `synchronized(this){notifyAll()}`.
Consequences, all pinned in `ThreadBlockCharacterizationTest` and the
timer tests:

- `stop()` completes **only** when the worker is parked inside an
  *interruptible* `get()` (a buffer): the interrupt makes the get
  throw and the loop exits without touching the monitor.
- With any input whose `get()` survives the interrupt (`ConstantBlock`
  — it never blocks; `DelayFunction` on either side; the UDP socket),
  `stop()` never returns: it either keeps observing
  `doingOutput == true` in 5 s rounds, or wins the microsecond window
  and deadlocks in `join()`. Every later `isRunning()` then blocks
  too. **The library's own timer idiom (`FirstTest`) is unstoppable.**
- A `RuntimeException` from downstream kills the worker while
  `isRunning()` stays true and `stop()` hangs (`doingOutput` never
  resets). In v0.2.0 a downstream throw is, in fact, the only way to
  end a Constant-fed worker.
- `stop()` does honour the in-flight item: it returns only after a
  blocked `put` completes, and the item is delivered, not dropped.

Whether and how to fix this is the owner's call:
`decisions/2026-09-23-fix-thread-block-stop.md`.
