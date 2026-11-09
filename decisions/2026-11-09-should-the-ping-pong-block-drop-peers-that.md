---
id: 2026-11-09-should-the-ping-pong-block-drop-peers-that
title: Should the ping-pong block drop peers that have gone silent?
status: done
stakes: medium
confidence: 72
opened: 2026-11-09
ruled: 2026-11-09
repo: javablocks
by: eba163f7-4766-4239-b515-dcd76034fa77
recommends: drop-after-three-silent-timeouts-fixed
---

## Problem

One of the seven known defects ("warts") your goals say to clean up first. It is listed as "the map block's handler set never shrinks", but the map block itself is not where the gap is.

The map block sorts messages by key and creates one handler per new key. It already has `removeHandler(key)`, which removes a handler and tells the factory to destroy it; a test pins that. What is missing is anyone calling it. The ping-pong block (the library's connection keep-alive, built on a map block keyed by peer) never does: its map block is private, so a host cannot either. Every peer that ever sent one message keeps its handler forever, and for a peer that has gone away for good, the timer keeps sending it a ping, at most once per timeout, forever (in FirstTest's setup, every check). Over a long-running UDP server that is a slow memory leak plus a growing stream of pings to nobody.

The protocol interface already has `destroyHandler`, so removal was clearly intended; it was just never wired up.

Today the timer sends a ping when a peer has been quiet longer than the timeout and no ping went out within the timeout. The question is when, if ever, a quiet peer should be dropped, and who decides how long "too long" is. The ping-pong classes have no tests today, so the order is: first pin today's behaviour (a silent peer keeps its handler and keeps being pinged), then make the fix as a visible change to that test, proven able to fail, and a design-log entry first.

## Options

### Drop after three silent timeouts, fixed

**Pros:**
- Closes the leak and the endless pings with no new public API: the timer removes a peer silent for more than 3 times the timeout, through the map block's existing removeHandler, which calls the protocol's destroyHandler.
- If the peer comes back, its next message creates a fresh handler (already pinned map-block behaviour).
- Small, contained in one class.

**Cons:**
- 'Three' is a guess; a host cannot change it without a later API change.
- A peer silent for a while but still alive loses whatever state its handler held.
- A message arriving at the very moment of removal may reach the handler being destroyed.

### Drop after a host-chosen silence

**Pros:**
- Same fix, but the host sets how long silence lasts before a peer is dropped, through a new constructor argument (with the old constructor kept, meaning 'never drop').

**Cons:**
- A public API addition, which needs your design ruling anyway and adds a second constructor.
- Keeping the old constructor as 'never drop' keeps the leak as the default.

### Idle eviction in the map block itself

**Pros:**
- General: any map-block user (not only ping-pong) could drop handlers unused for a given time, through a new map-block method.

**Cons:**
- Widens the map block's public interface and makes it track time per handler, a feature rather than a cleanup.
- Ping-pong would still need to call it, so it does not save the ping-pong change.

### Leave it, document that hosts remove handlers

**Pros:**
- No behaviour change.

**Cons:**
- Ping-pong users cannot remove handlers at all, since the map block is private, so the leak and endless pings stay.
- Goal 1 asks every wart to be fixed or ruled left; this leaves the worst one.

## Recommendation

Drop after three silent timeouts, with the number fixed. It fixes the real harm (unbounded growth, pings to nobody) inside one class, with no API to design, and uses removal machinery that already exists and is already tested. Making the silence configurable is a fine follow-up, but it is public API and fits better when the declarative assembly gives blocks their parameters. I am less sure than on the other cleanups because I don't know how you meant the protocol's handlers to live — if a handler holds state that must survive a long silence, choose 'leave it' or the host-chosen option. Confidence 72.

## If nothing is decided

Ping-pong keeps every peer forever and keeps pinging the departed. Runs move on to the other cleanups; goal 1 cannot be called done until this is ruled.

## Ruling

**Drop after three silent timeouts, fixed** (`drop-after-three-silent-timeouts-fixed`). Tog, 2026-11-09, from the project pages.

## Done

2026-11-09, autopilot run 3: the ping-pong timer now removes a peer silent for more than three timeouts through the map block's `removeHandler` (J11); today's behaviour was pinned first and flipped to `aSilentPeerIsPingedThenDroppedAfterThreeTimeouts`, proven red against the old code and with the factor changed to 1 or 30.
