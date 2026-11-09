---
id: 2026-11-09-how-should-a-host-control-the-blocks
title: "How should a host control the blocks' log output?"
status: proposed
stakes: medium
confidence: 75
opened: 2026-11-09
repo: javablocks
by: eba163f7-4766-4239-b515-dcd76034fa77
recommends: settable-defaults-output-unchanged
---

## Problem

One of the seven known defects ("warts") your goals say to clean up first: "the reporter singletons".

Every block can report INFO, DEBUG and ERROR messages. When a block is built it takes three reporters from the `Reporter` class: one shared INFO printer and one shared DEBUG printer to standard output, and one ERROR printer to standard error. Three switches in that class (`infoOn`, `debugOn`, `errorOn`) are hard-coded to on, and the lookup methods take the block as an argument but ignore it. So every thread block prints four DEBUG lines per item it moves, every buffer prints INFO when full, and the only way to quiet a block is to call `setDebugReporter(...)` on each block one by one. A host cannot turn logging down, or send it to its own logger, for the whole library. The tests quiet blocks one at a time for this reason (J3 ruled out silencing them globally and named this cleanup as where a test-time reporter belongs).

The foundation's guidance for Java libraries (the `java-library` skill this project uses) is that a library is composed, not configured: the host owns configuration. Printing debug lines to stdout on every message, with no switch, works against that.

Any fix adds or changes public API (the Reporter class), so it is yours to rule.

## Options

### Settable defaults, output unchanged

**Pros:**
- Three static setters on Reporter (info, debug, error; null means silent) that new blocks pick up, so a host or a test quiets or redirects the whole library in one line.
- Nothing a current host sees changes: the defaults still print exactly as today.
- Smallest API addition that removes the wart.

**Cons:**
- Still global state: two hosts in one JVM share the defaults.
- Blocks built before the setter is called keep the old reporters; the setter is not retroactive.
- The noisy defaults stay noisy until a host acts.

### Settable defaults, debug off by default

**Pros:**
- Everything the first option gives, and a library used out of the box no longer prints four lines per message.

**Cons:**
- Changes what every existing host sees on its console; someone relying on the DEBUG lines to watch a pipeline loses them until they turn them on.
- Two changes in one step, against the one-thing-at-a-time rule; better as a follow-up ruling.

### No globals: reporters passed in when blocks are built

**Pros:**
- The cleanest design: each pipeline gets its own reporters, no shared state, fits 'composed, not configured'.

**Cons:**
- Changes every factory method or adds a context object: a large public API change, really a design for the declarative assembly, which your goals put after the cleanups.
- Breaks existing hosts' code.

### Leave it as it is

**Pros:**
- No change; per-block setters already work.

**Cons:**
- A host still cannot control the library's logging as a whole.
- Goal 1 needs a ruling on it either way.

## Recommendation

Settable defaults, output unchanged. It removes what makes the singletons a wart — that nobody can change them — with three setters and no visible change for anyone who does not call them. Whether debug should be off by default is a separate question you can rule on afterwards, and passing reporters in at build time belongs with the assembly's design, where blocks get their parameters anyway; settable defaults do not block either. Confidence 75: the trade-off between leaving global state in place now and redesigning later is a judgment call.

## If nothing is decided

The reporters stay fixed; hosts quiet blocks one at a time. Runs move on to the other cleanups; goal 1 cannot be called done until this is ruled.
