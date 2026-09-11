---
id: 2026-09-11-the-diverged-working-copy
title: This repository's working copy and its trunk have each moved without the other
status: proposed
stakes: medium
confidence: 82
opened: 2026-09-11
repo: javablocks
by: 8f2bda13
recommends: read-the-two-local-commits-then-rebase-onto-main
---

## Problem

The checkout of this repository on the prodesk is on `main`, but it is
not the same `main` as the server's. Measured today: **two commits here
that the server does not have, and eight commits on the server that here
does not.** The branches have diverged.

The two local commits are real work — the newest is a characterisation
test suite ("34 tests, warts included, each proven able to fail"), which
is the kind of thing that took a session to write. Whatever is in the
eight on the server was done from somewhere else.

There is also a directory of session logs here that has never been
committed at all.

This has been true since 26 August. Nothing forces a resolution, which is
why it has not been resolved — but a diverged trunk is the state in which
work gets silently lost, because the next person pushes one side and
assumes the other never existed.

## Options

### Read the two local commits, then rebase onto main

**Buys:** both sides survive, and the history stays linear and readable.
The two local commits are small and self-contained (a test suite), which
is the best case for rebasing.
**Costs:** somebody has to read both sides first and be sure the eight
server commits do not already contain a different version of the same
work; if they do, the rebase produces a mess that has to be untangled by
hand.

### Merge the two together

**Buys:** nothing is rewritten, so nothing can be lost by a bad rebase;
it is the safe mechanical option.
**Costs:** a merge commit in a repository whose history is otherwise
linear, and the same reading of both sides is needed anyway to resolve
conflicts sensibly.

### Discard the local side

**Buys:** one command; the checkout matches the server immediately.
**Costs:** throws away a characterisation test suite that somebody built
deliberately. Unacceptable without reading it first, which is the
expensive part of every other option anyway.

### Leave it diverged

**Buys:** nothing is decided wrongly today.
**Costs:** the divergence grows. Every session that opens this repository
has to rediscover the situation, and the first one that pushes without
noticing loses one side or the other.

## Recommendation

Read the two local commits, then rebase onto `main`, confidence 82.

The arguments: the local side is two commits and the newer one is a test
suite — self-contained, easy to verify by running it, and the least
likely kind of change to conflict semantically with anything. Rebasing
keeps the history linear, which matters in a repository small enough that
its log is read rather than searched. And the reading step is required by
every option that does not throw work away, so it is not an extra cost of
this one.

Against, at 18 points: nobody has yet looked at what the eight server
commits contain. If they include their own version of the same
characterisation work — which is plausible, since both sides were working
on the same thing — then a rebase is the wrong shape and a merge with a
deliberate resolution is better. That is what the reading step is for,
and if it turns out that way, the ruling should be "merge" instead.

Whichever way it goes, the uncommitted session logs should be committed
in the same pass; they are repository content and the estate's
conventions expect them tracked.

## If nothing is decided

The two histories keep drifting. The characterisation test suite exists
on one machine only and is not backed up anywhere. The next session here
starts by spending fifteen minutes working out which `main` it is looking
at, or does not notice and loses one side.
