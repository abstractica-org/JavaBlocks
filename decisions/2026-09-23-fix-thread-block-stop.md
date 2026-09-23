---
id: 2026-09-23-fix-thread-block-stop
title: Fix ThreadBlock.stop() — it deadlocks or never returns unless the worker is parked in an interruptible get()
status: proposed
stakes: medium
confidence: 80
opened: 2026-09-23
repo: javablocks
by: edae1351
recommends: fix-stop-minimally
---

## Problem

The J3 characterization tests surfaced that `ThreadBlockImpl.stop()`
can only ever complete when the worker thread is parked inside an
interruptible `get()` (a buffer). `stop()` is `synchronized` and
`join()`s while still holding the block's monitor, which the worker
needs for its post-put `synchronized(this){ notifyAll(); }`. With any
input whose `get()` survives an interrupt — `ConstantBlock`,
`DelayFunction` on either side, the UDP socket — `stop()` either keeps
observing `doingOutput == true` in 5-second wait rounds, or wins the
microsecond window and deadlocks in `join()`; every later
`isRunning()` call then blocks too. The library's own timer idiom
(`FirstTest`: constant → thread → delay) is therefore unstoppable.
Two adjacent warts share the mechanism: a downstream RuntimeException
kills the worker while `isRunning()` stays true and `stop()` hangs,
and `DelayFunction` swallows interrupts, which is what turns timers
into the deadlock case.

This is the first candidate from the J3 wart list (cleanup is one
ruled step at a time, per J1). It matters now because the declarative
assembly (J1 step 4) will build pipelines whose lifecycle a host will
want to stop, and the viewer's second host vocabulary should not ship
an unstoppable timer as its canonical example.

## Options

### Do nothing

**Buys:** The v0.2.1 pin stays exactly the code proven in other
projects; zero risk to concurrency semantics; the assembly work can
proceed on the pinned base regardless.
**Costs:** Every pipeline with a non-buffer-fed ThreadBlock stays
unstoppable; hosts must kill the JVM or poison downstream to end a
timer; `isRunning()` can block forever, which any lifecycle-managing
assembly runtime will trip over.

### Fix stop() minimally

Rework only the stop path, keeping the loop's semantics: join outside
the block's monitor, replace the `doingOutput`/`notifyAll` handshake
so the worker never needs the stop-holder's lock after a put, and
reset state when the worker dies so `isRunning()` tells the truth.
Behaviour change is confined to shutdown: `stop()` returns after the
in-flight `put` completes for every input kind, and the three J3 tests
that pin the deadlock/hang flip to pin termination — changed in the
same commit, per the pin-before-change rule.

**Buys:** Every ThreadBlock becomes stoppable, including the timer
idiom and socket-fed threads; `isRunning()` never blocks and never
lies; smallest possible diff to handwritten code; the steady-state
data path (get → put hand-off, back-pressure) is untouched.
**Costs:** A behaviour change to the library's most delicate class;
a worker inside an interrupt-swallowing `get()` (DelayFunction,
socket) still finishes that get and one put before exiting — stop is
prompt, not instant, unless DelayFunction is also fixed (a separate
ruled step).

### Redesign the lifecycle

A larger step: e.g. a poison-pill/closeable protocol on `Input`, or
interrupt-honouring delay/socket blocks plus stop semantics defined
across all of `ThreadControl`.

**Buys:** A coherent shutdown story for every block at once, ready
for the assembly's lifecycle needs.
**Costs:** Violates the one-wart-per-step rule; redesigns the outside,
which is the owner's, before the assembly's needs are even known;
large test churn on a base whose value is stability.

## Recommendation

**Fix stop() minimally** — confidence 80. It removes the one wart
that makes lifecycle management impossible while leaving the data
path byte-for-byte pinned, and it is exactly the shape of step the
J1 charter prescribes: one wart, one ruled step, tests changed
visibly in the same commit. `DelayFunction`'s interrupt-swallowing
can be its own later step if prompt-vs-instant stop turns out to
matter.

## If nothing is decided

Nothing is blocked: the v0.2.1 pin stands and the declarative
assembly (the next scope) can be designed and built against it. The
deadlock remains pinned as documented behaviour, and any assembly or
viewer feature that wants to stop a running pipeline will surface
this record again.
