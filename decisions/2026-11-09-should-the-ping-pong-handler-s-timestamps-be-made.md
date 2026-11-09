---
id: 2026-11-09-should-the-ping-pong-handler-s-timestamps-be-made
title: "Should the ping-pong handler's timestamps be made safe to read across threads?"
status: proposed
stakes: low
confidence: 85
opened: 2026-11-09
repo: javablocks
by: eba163f7-4766-4239-b515-dcd76034fa77
recommends: mark-both-fields-volatile
---

## Problem

One of the seven known defects ("warts") your goals say to clean up first.

The ping-pong block keeps connections alive: one handler per peer records when a message last arrived from that peer and when a ping was last sent to it. A timer thread checks those two times regularly and sends a ping to any peer that has been quiet too long.

The two times are plain `long` fields. The arrival time is the one that crosses threads: it is written by the thread receiving messages and read by the timer. Java does not promise that the timer ever sees the receiver's latest write unless the field is marked `volatile` (Java's keyword for "always read the latest value"). A plain `long` may even be read half-old, half-new on some 32-bit JVMs. The practical effect is mild: the timer may think a peer is quiet when it is not and send an unneeded ping. It cannot lose messages. The ping time is today written and read by the timer thread only; it would be marked `volatile` too because the handler's public interface lets any thread read or set it (for example a host that drives the timer from more than one thread), and keeping both fields alike costs nothing.

The fix is to mark both fields `volatile`. Honest limit: this kind of visibility bug cannot be shown reliably by a test on a modern 64-bit JVM — it almost never shows up. The test that pins it would check the fields are declared volatile (a structural check, proven able to fail by removing the keyword), not observe a stale read. The ping-pong classes have no tests today.

## Options

### Mark both fields volatile

**Pros:**
- The standard Java fix; one keyword per field, no API change.
- The timer always sees the latest arrival time, so no spurious pings from stale reads.
- Also rules out half-written longs on 32-bit JVMs.

**Cons:**
- The test can only check the declaration, not observe the bug, so it is a weaker pin than the others.
- A tiny cost per read and write, irrelevant at message rates.

### Use AtomicLong fields

**Pros:**
- Equally correct, and allows atomic compare-and-set if the timer logic ever needs it.

**Cons:**
- More code and an object per field for no current need; volatile already gives everything the code uses.

### Leave it as it is

**Pros:**
- No change; the worst effect today is an extra ping.

**Cons:**
- A known thread-safety defect stays in a library sold on its thread behaviour.
- Goal 1 needs a ruling on it either way.

## Recommendation

Mark both fields volatile. It is the textbook fix, two keywords, and nothing in the code needs more than "read the latest value". AtomicLong adds weight for no use. The weakness is the test, which can only pin the declaration; I'd still add it so removing the keyword turns the build red. Confidence 85.

## If nothing is decided

The fields stay plain. Runs move on to the other cleanups; goal 1 cannot be called done until this is ruled.
