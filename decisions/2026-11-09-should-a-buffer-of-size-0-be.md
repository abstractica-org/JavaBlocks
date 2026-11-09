---
id: 2026-11-09-should-a-buffer-of-size-0-be
title: Should a buffer of size 0 be refused instead of blocking forever?
status: proposed
stakes: low
confidence: 88
opened: 2026-11-09
repo: javablocks
by: eba163f7-4766-4239-b515-dcd76034fa77
recommends: refuse-capacity-below-1
---

## Problem

One of the seven known defects ("warts") your goals say to clean up first.

A buffer block is the queue between a producer and a consumer: put waits when it is full, get waits when it is empty. `BasicBlocks.getBufferBlock(capacity)` accepts any number. With capacity 0 the buffer is "full" from the start, so the first `put` waits forever (until its thread is interrupted) — the pipeline silently hangs. A negative capacity fails differently: Java throws `NegativeArraySizeException` when the buffer is built, a confusing message. A characterization test (`capacityZeroBufferBlocksEveryPut`) pins today's hang.

The buffered socket assembly passes its caller's buffer size straight through, so a host that passes 0 there hangs too.

The fix would check the number when the buffer is built. The test flips from "capacity 0 hangs on put" to "capacity below 1 is refused with a clear message", in the same commit, with a design-log entry first.

## Options

### Refuse capacity below 1

**Pros:**
- A host's mistake fails at once with a clear IllegalArgumentException, at the line that made it, instead of a silent hang later.
- One check in one place (the factory and the buffer constructor).
- No method signatures change.

**Cons:**
- A host that today builds a capacity-0 buffer (and never puts into it) now gets an exception — unlikely, but it is a behaviour change.
- Closes the door on a size-0 buffer meaning something useful, unless that is added later.

### Make size 0 a direct hand-off

**Pros:**
- Gives size 0 a real meaning: put waits until a consumer takes the item (what Java's SynchronousQueue does), a useful joint in a dataflow pipeline.

**Cons:**
- New behaviour and new concurrency code to get right, which is a feature, not a cleanup.
- Goes against your rule that cleanups stay small and separate from features.

### Leave it as it is

**Pros:**
- No change.

**Cons:**
- A one-character mistake keeps hanging a pipeline with no message.
- Goal 1 needs a ruling on it either way.

## Recommendation

Refuse capacity below 1. It turns a silent hang into a clear error at the line that caused it, is a tiny change, and keeps the cleanup a cleanup. A direct hand-off at size 0 is a good idea, but it is a feature and can be proposed separately later; refusing 0 now does not prevent it. Confidence 88.

## If nothing is decided

Size-0 buffers keep hanging on the first put. Runs move on to the other cleanups; goal 1 cannot be called done until this is ruled.
