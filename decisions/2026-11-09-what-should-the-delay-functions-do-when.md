---
id: 2026-11-09-what-should-the-delay-functions-do-when
title: What should the delay functions do when their thread is interrupted?
status: done
stakes: medium
confidence: 78
opened: 2026-11-09
ruled: 2026-11-09
repo: javablocks
by: eba163f7-4766-4239-b515-dcd76034fa77
recommends: return-early-and-keep-the-flag
---

## Problem

One of the seven known defects ("warts") your goals say to clean up first.

`BasicFunctions.DelayFunction(ms)` and `getRandomDelayFunction(min, max)` pass an item through after sleeping. They are how the library builds a timer: a constant block, a thread block and a delay make a "tick every N ms" (FirstTest's ping-pong timer uses a 10-second one). In Java, interrupting a thread is the standard way to tell it to stop what it is doing. The delay functions catch the interrupt, throw it away, and sleep the full time anyway; they also clear the thread's "interrupted" flag, so nothing after them knows it happened. A characterization test (`delayFunctionAbsorbsInterruptsAndDelaysTheFullTime`) pins this.

Since the J4 fix (your stop() ruling), stopping a timer always works, but when the delay sits on the thread's input side, stop() has to wait out the remaining delay, however long the delay is. When it sits on the output side — as in FirstTest — stop() waits for the in-flight tick by design (J4), so that case, FirstTest included, does not change whatever is chosen here.

There is a catch: these are plain Java `Function`s, which cannot throw `InterruptedException`. So "honouring" the interrupt has to take one of the shapes below.

## Options

### Return early and keep the flag

**Pros:**
- Java's standard convention for code that cannot throw InterruptedException: stop sleeping, set the flag again, return.
- stop() on an input-side timer returns at once instead of after up to the full delay.
- The thread block already clears the flag before its put (J4), so the tick in flight is still delivered exactly once; the input-side timer test stays green, only faster.

**Cons:**
- The item comes out early, so an interrupted delay did not really delay.
- If some other caller's thread is interrupted mid-delay, the item goes downstream with the flag set, and if a buffer there is full, its put refuses the item with InterruptedException instead of waiting (a buffer with room still accepts it).

### Abort with an exception

**Pros:**
- With the delay on the input side, no tick is delivered after stop() is called: the in-flight tick is dropped, the most literal meaning of 'stop'. (On the output side nothing changes, as above.)
- The thread block already swallows a failure caused by stop's own interrupt, so the worker exits quietly.

**Cons:**
- A RuntimeException out of a Function surprises any other caller (a push or pull block used without a thread block).
- The input-side timer test changes from 'exactly one more tick' to 'no more ticks', a bigger visible change.

### Sleep the full time but keep the flag

**Pros:**
- Smallest change: timing is exactly as today, only the interrupt is no longer lost for code after the delay.

**Cons:**
- stop() still waits out the remaining delay, which is the part hosts actually feel.
- Has the same risk as returning early (a full buffer downstream refuses the item), without the benefit.

### Leave it as it is

**Pros:**
- No change; since J4 stop() always returns eventually.

**Cons:**
- Stopping an input-side timer waits up to the full delay.
- Swallowing interrupts is a well-known Java anti-pattern in a library about threads.

## Recommendation

Return early and keep the flag. It is the standard Java answer for code that cannot throw, it makes stopping prompt where it is slow today, and with the thread block it keeps today's 'the tick in flight is delivered once' rule, so the change is small and visible in one test. Aborting is more literal but throws from a plain Function, which surprises callers outside a thread block. The same fix would apply to both delay functions, in one commit, since they share the defect; the random delay has no test today, so a test pinning its current interrupt-swallowing goes in first, and both tests then change visibly with the fix. Confidence 78 — the choice between early return and abort is a real one about what 'stop a timer' should mean to you.

## If nothing is decided

The delays keep swallowing interrupts; stopping an input-side timer keeps taking up to its full delay. Runs move on to the other cleanups; goal 1 cannot be called done until this is ruled.

## Ruling

**Return early and keep the flag** (`return-early-and-keep-the-flag`). Tog, 2026-11-09, from the project pages.

## Done

2026-11-09, autopilot run 3: both delay functions now return early on an interrupt and re-set the flag (J9); their two tests flipped to `…ReturnsEarlyOnAnInterruptAndKeepsTheFlag` (the random one pinned first), each proven red against the old code and against a fix that drops the flag; the pull-side timer test stays green, javadoc updated.
