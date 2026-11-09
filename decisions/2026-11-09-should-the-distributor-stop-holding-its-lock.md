---
id: 2026-11-09-should-the-distributor-stop-holding-its-lock
title: Should the distributor stop holding its lock while it forwards a message?
status: ruled
stakes: medium
confidence: 80
opened: 2026-11-09
ruled: 2026-11-09
repo: javablocks
by: eba163f7-4766-4239-b515-dcd76034fa77
recommends: forward-from-a-snapshot-outside-the-lock
---

## Problem

One of the seven known defects ("warts") your goals say to clean up first.

The distributor block is the library's only broadcast: each message put into it is passed to every connected output, in the order they were added, on the caller's thread. Today `put` holds the block's lock for the whole forwarding loop. So while one output is slow (a full buffer downstream, a network send), nobody can add or remove an output, and a second producer cannot even start forwarding. If the thread that would drain that full buffer ever tries to add or remove an output, the two wait on each other forever. A characterization test (`putHoldsTheMonitorWhileForwarding`) pins today's behaviour.

One hang no option fixes: if the only thread draining a full output buffer itself puts into the distributor, it blocks on that same full buffer whatever the locking. That is a wiring cycle, not a locking defect.

The map block already avoids this exact pattern on purpose: it picks the handler under its lock and calls the handler outside it, and a test pins that ("other keys proceed").

Releasing the lock has two visible consequences, and they are the real decision:
1. Two producers putting at the same time would no longer be forced one after the other, so output A could see their messages in one order and output B in the other. Today every output sees the same order.
2. An output removed while a message is mid-forward may still receive that one message.

## Options

### Forward from a snapshot, outside the lock

**Pros:**
- A slow output no longer blocks adding or removing outputs, and the deadlock above becomes impossible.
- Matches the rule the map block already follows: never call out while holding your own lock.
- Simple: keep the outputs in a copy-on-write list and loop over it without locking.

**Cons:**
- With two producers at once, different outputs may see messages in different orders.
- An output being removed can receive one last in-flight message.
- Both are behaviour changes a host could notice; the pinning test changes visibly.

### Keep forwarding under the lock, document it

**Pros:**
- Keeps the guarantee that every output sees the same order, even with several producers.
- No behaviour change; only a comment and the log.

**Cons:**
- The slow-output blocking and the possible deadlock stay.
- Inconsistent with the map block's deliberate design.

### Separate locks for forwarding and for the output list

**Pros:**
- Keeps the same order across outputs (producers still forward one at a time) while add and remove no longer wait for a slow output.

**Cons:**
- Two locks to reason about in a library whose risk is exactly this kind of reasoning.
- Producers still queue behind a slow output, so one slow output still holds up every other producer.

## Recommendation

Forward from a snapshot, outside the lock. It removes a real deadlock and makes the distributor behave like the map block and the push block, which all run on the caller's thread without serializing callers. The lost guarantee (same order at every output with several producers) is one nothing in the library relies on; a host that needs it can put a buffer and one thread in front of the distributor, which is the library's normal way to serialize. If you know of a host that does rely on it, the separate-locks option keeps it. Confidence 80.

## If nothing is decided

The distributor keeps holding its lock while forwarding. Runs move on to the other cleanups; goal 1 cannot be called done until this is ruled.

## Ruling

**Forward from a snapshot, outside the lock** (`forward-from-a-snapshot-outside-the-lock`). Tog, 2026-11-09, from the project pages.
