---
id: 2026-11-09-should-the-map-block-lock-its-handler
title: Should the map block lock its handler count?
status: proposed
stakes: low
confidence: 90
opened: 2026-11-09
repo: javablocks
by: eba163f7-4766-4239-b515-dcd76034fa77
recommends: lock-the-read
---

## Problem

This is the first of the seven small defects ("warts") your goals say to clean up first, one ruling each.

The map block is the block that sorts incoming messages by a key (for example, by the sender's address) and keeps one handler per key, creating a handler the first time a key appears. It keeps those handlers in a plain Java HashMap and protects every access with a lock — except one: `getSize()`, which says how many handlers there are, reads the map without the lock.

What can go wrong: a thread calling `getSize()` while another thread is adding or removing a handler may see an out-of-date number. Java gives no guarantee about what an unlocked reader sees, and nothing forces the value to be refreshed. No code in the library calls `getSize()` today, so nothing visibly breaks; a host that uses it for monitoring or a "how many peers are connected" display could read a wrong number.

The fix is one line (`synchronized (map)` around the read). A test can show the change: the handler factory runs while the map lock is held, so a test can make the factory wait, call `getSize()` from another thread, and see that it now waits for the lock where today it returns at once. Per the project rules the change gets a design-log entry first and the test that shows it in the same commit.

## Options

### Lock the read

**Pros:**
- One line, the same lock every other map-block method already uses, so the count is always current.
- A test can pin it: getSize() waits while a handler is being created, where today it does not.
- No public API change; hosts see the same method.

**Cons:**
- getSize() now waits while a slow handler factory creates a handler — the same wait getAllHandlers() already has.
- Fixes a defect nobody has reported hitting.

### Leave it as it is

**Pros:**
- No change at all; nothing in the library calls getSize().

**Cons:**
- A known thread-safety defect stays in a library whose whole value is its thread behaviour.
- Goal 1 is only done when every wart is fixed or ruled left, so this needs a ruling either way.

## Recommendation

Lock the read. It costs one line, matches how the rest of the map block already works, and turns an invisible thread-safety hole into a pinned behaviour. The only cost, waiting during handler creation, is the same wait the snapshot method already has. Confidence 90.

## If nothing is decided

getSize() stays unlocked. Runs move on to the other cleanups; goal 1 cannot be called done until this is ruled.
