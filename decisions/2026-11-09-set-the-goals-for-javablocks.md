---
id: 2026-11-09-set-the-goals-for-javablocks
title: Set the goals for javablocks
status: done
stakes: medium
confidence: 82
opened: 2026-11-09
ruled: 2026-11-09
repo: javablocks
by: d60b5023-7996-4e1a-baac-b7fff965cdfb
recommends: assembly-first-cleanups-alongside
---

## Problem

The scheduled runs need a short, ordered list of what they are working toward in JavaBlocks, and what "done" means for each. Without one they can only do housekeeping and carry out rulings you have already made. Today (run 1) the one such ruling, the stop() fix, has been carried out: every thread block can now be stopped. Nothing else can start until you set goals.

Here is where the project stands. Your 2026-08-26 charter (J1 in design/decision-log.md) sets two jobs, in order. First, a declarative way to build a pipeline in Java. Second, a thin adapter so the workflow viewer can design JavaBlocks pipelines. The viewer is the project that draws pipelines; JavaBlocks is its second host, there to prove it is not tied to one domain. The base is pinned first and cleaned up one fix at a time. Six known warts are left. These are small defects that the tests pin as they behave today:
- the map block never removes old handlers;
- handler timestamps are not safe to read across threads;
- the map block's size count is unlocked;
- the reporters are global singletons;
- the delay function swallows interrupts;
- a buffer of size 0 blocks forever, and the distributor forwards while holding its lock.

Each fix needs your ruling. The shape of the declarative assembly is yours to design too: a run can only propose it. The adapter waits for the viewer's graph contract in wiring-playground, which runs may not touch.

Below are four candidate goal lists, all in priority order.

## Options

### Assembly first, cleanups alongside

**Pros:**
- Goal 1: the declarative assembly. A run proposes the builder's outside shape: blocks with kinds, typed connection points, wires, and the map block's per-key handler factory as a fill-in slot. You rule on it, then runs build it. Done when the FirstTest ping-pong pipeline and the examples/udp pipelines are rebuilt through it, with tests.
- Goal 2: the remaining cleanups, one at a time. For each wart a run writes a decision record; you rule; a run fixes it with the tests changed visibly. Done when every wart is fixed or you have ruled to leave it.
- Goal 3: the viewer adapter. It starts only once wiring-playground has the graph contract. Done when the viewer's graph for the ping-pong pipeline builds a running pipeline through the assembly.
- Follows your charter's order: the assembly is 'the next natural step'. Stopping now works, which is what an assembly that manages a pipeline's lifecycle needed most.

**Cons:**
- The assembly's shape waits on your ruling. Runs can do little toward goal 1 until you rule, so cleanup records would fill the gap.
- Building on a base that still has the map-block warts could mean reworking the assembly's map-block filler later.

### Cleanups first, then the assembly

**Pros:**
- The same three goals, with cleanups first: the base is fully clean before any public API is built on it.
- Each cleanup is small, so runs keep moving one ruling at a time.

**Cons:**
- Six separate rulings come before the step the charter calls next. The value, the viewer designing JavaBlocks pipelines, arrives much later.
- Some warts (the reporter singletons, the delay function) may not matter to the assembly at all.

### Assembly only

**Pros:**
- One goal: the declarative assembly, defined as in the first option. Cleanups happen only when the assembly needs one.
- The tightest focus. Nothing is spent on warts no one is hitting.

**Cons:**
- Leaves known concurrency warts in a library whose value is its concurrency behaviour.
- The adapter is not listed, so a later goals change is needed to reach the viewer.

### Do nothing

**Pros:**
- No direction is set by a run. You can write GOALS.md yourself whenever you like.

**Cons:**
- Runs stay limited to housekeeping. The project stalls, and every run spends money confirming there is nothing to do.

## Recommendation

Go with "Assembly first, cleanups alongside". It is your charter's order, and today's stop() fix removed the one wart that blocked an assembly that manages a pipeline's lifecycle. While the assembly's shape waits for your ruling, runs can still move: they write the shape proposal first, then one cleanup proposal at a time. You choose which to rule on. Order matters less than having a list, since you rule on every step anyway. Confidence 82.

## If nothing is decided

Runs keep to housekeeping and the rulings you have already made. With the stop() fix done, that leaves almost nothing, so the driver should hold the project until you rule.

## Ruling

**Cleanups first, then the assembly** (`cleanups-first-then-the-assembly`). Tog, 2026-11-09, from the project pages.

## Done

2026-11-09 (run 2): `tools/autopilot/GOALS.md` written from the ruling — cleanups first, then the assembly, then the viewer adapter, each with its "done" from this record.
