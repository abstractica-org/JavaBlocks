---
id: 2026-11-10-should-a-thread-that-is-waiting-on
title: Should a thread that is waiting on a UDP socket stop when it is asked to?
status: done
stakes: medium
confidence: 82
opened: 2026-11-10
ruled: 2026-11-11
repo: javablocks
by: d50818be-0fda-494c-b996-7f3746f145f0
recommends: the-socket-block-wakes-up-on-interrupt-by-checking-every-100-ms
---

## Problem

While building the pipeline builder (the "Design", goal 2), a run found a new wart. In JavaBlocks a thread block stops by interrupting its own thread. Every input in the library wakes up when its thread is interrupted, except the UDP socket block. Java never wakes a thread that is waiting in a UDP socket's receive when the thread is interrupted (checked on Java 25); only closing the socket wakes it. So a thread block reading from a socket never stops while no packet arrives: its stop() waits forever.

This is already live in the library. The ready-made socket assembly, BufferedSocketBlock (Assemblies.getBufferedSocketBlock), stops its two threads and then closes its socket, so its stop() hangs forever whenever no packet is arriving. A new test now pins exactly that: stop() is still waiting after half a second, and closing the socket from another thread releases it.

It matters now for two reasons. Goal 2 is done only when the UDP examples are rebuilt through the Design, and they need a socket node. And the Pipeline's stop was ruled as "stop every thread, then close the sockets", which would hang the same way. So the socket node waits for this ruling. The rest of the builder is built and merged without it.

## Options

### The socket block wakes up on interrupt, by checking every 100 ms

**Pros:**
- The socket becomes like every other input: an interrupt makes get() throw InterruptedException, so thread stop() works with no special case.
- BufferedSocketBlock.stop() works as written, and the Pipeline keeps its ruled order (stop the threads, then close the sockets).
- The change stays inside one class (UDPSocketBlockImpl): it waits for at most 100 ms at a time, checks whether it was interrupted, then waits again. The socket stays open, so a stopped pipeline can be started again.
- Small, and pinned by flipping the new test (stop returns without close).

**Cons:**
- An idle reader wakes ten times a second (negligible, but not zero).
- stop() can take up to 100 ms longer on a socket reader.
- A changed behaviour of a published block (get() now throws InterruptedException where it used to keep waiting), even though that waiting was the bug.

### Close the socket to stop, in BufferedSocketBlock and the Pipeline

**Pros:**
- No change to the socket block itself.
- Stopping is immediate.

**Cons:**
- Closing first makes the reading thread fail with a socket error, which it prints as a stack trace, unless the close is timed to land after stop() has started. That timing needs a helper thread in every place that stops a socket reader.
- A socket closed to stop it cannot be restarted.
- Every host that wires a socket by hand still hangs exactly as today, because the trap stays in the socket block.

### Make the socket block a channel that an interrupt closes

**Pros:**
- Java's own mechanism: an interrupt closes a DatagramChannel and wakes the reader immediately.

**Cons:**
- The interrupt closes the socket, so a stopped pipeline cannot be started again.
- A larger rewrite of the socket block (channels instead of DatagramSocket).

### Leave it as it is

**Pros:**
- No behaviour change.

**Cons:**
- BufferedSocketBlock.stop() keeps hanging.
- The Design gets no socket node, so goal 2 cannot be done: the UDP examples cannot be rebuilt through it.

## Recommendation

Wake on interrupt by checking every 100 ms. It fixes the cause in the one block that breaks the library's rule that every blocking get() answers an interrupt. Everything above it then works unchanged: BufferedSocketBlock, the ruled Pipeline stop order, and hosts' own pipelines. The cost is a 100 ms wake-up on idle readers and a little more time to stop. Closing to stop only treats the symptom where we remember to, adds a timing-sensitive helper, and leaves every hand-wired socket reader hanging. The channel rewrite is bigger and loses restart. A run would do it as one wart, as always: a J entry, the new pin flipped (stop returns without close), proven able to fail, and then the Design's udpSocket node. Confidence 82. The 100 ms is a judgement call; anything from 50 to 500 ms works, and I'd keep it a fixed constant, not a setting.

## If nothing is decided

BufferedSocketBlock.stop() keeps hanging when no packets arrive. The Design gets no udpSocket node, so goal 2 stops short of rebuilding the UDP examples. Runs can still rebuild FirstTest through the Design and build the map node.

## Ruling

**The socket block wakes up on interrupt, by checking every 100 ms** (`the-socket-block-wakes-up-on-interrupt-by-checking-every-100-ms`). Tog, 2026-11-11, from the project pages.

## Done

2026-11-11, autopilot run 8: fixed as ruled (J16 in `design/decision-log.md`). `UDPSocketBlockImpl.get()` waits 100 ms at a time and throws `InterruptedException` when its thread is interrupted; the socket stays open. The pinning test is flipped (`BufferedSocketBlock.stop()` now returns with no packet and no outside close) and three new tests pin the socket block itself; each was proven able to fail.
