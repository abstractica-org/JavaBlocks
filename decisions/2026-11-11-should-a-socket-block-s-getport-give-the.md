---
id: 2026-11-11-should-a-socket-block-s-getport-give-the
title: "Should a socket block's getPort() give the port it listens on?"
status: ruled
stakes: low
confidence: 90
opened: 2026-11-11
ruled: 2026-11-11
repo: javablocks
by: 6311e91a-2878-4858-8a92-6b48e411cb6f
recommends: getport-gives-the-port-the-socket-listens-on
---

## Problem

While building the socket node for the pipeline builder, a run found one more wart in the UDP socket block (UDPSocketBlock, the block that sends and receives UDP packets). Its getPort() method always answers -1. The code asks Java's socket for the port it is "connected" to, and a socket block is never connected to one fixed peer, so Java answers -1. What a caller almost certainly wants is the port the block listens on (the "local" port). The ready-made buffered socket assembly (BufferedSocketBlock) passes the same -1 on.

It matters most when a socket is opened on port 0, which means "give me any free port": the only way to learn which port the system picked is getPort(), and it says -1. The pipeline builder allows port 0 (inside a map's per-key pipeline it is the only choice that works for more than one key), so a host that builds such a pipeline cannot find out where its socket is. Nothing inside the library calls getPort(), so nothing breaks today; the answer is just useless. Two new tests pin the -1 as it is. This is a behaviour change to a published block, so by the project's rules it is yours to rule, like the earlier warts.

## Options

### getPort() gives the port the socket listens on

**Pros:**
- The method does what its name says: a socket opened on 3337 answers 3337, and one opened on port 0 answers the port the system picked.
- Makes port 0 usable: a host (or a test) can open a socket on any free port and learn where it is.
- A one-line change in one class (ask Java for the local port); the buffered socket assembly gets the right answer with it.
- No caller can sensibly depend on the -1: it carries no information.

**Cons:**
- A changed answer from a published block (from -1 to the real port), even though -1 was never useful.
- One more small cleanup step for a run: a log entry, the two pinning tests flipped.

### Leave getPort() answering -1

**Pros:**
- No behaviour change at all.
- No work.

**Cons:**
- getPort() stays useless, and a socket on port 0 has no way to tell where it is.
- A host reading the builder's documentation ('port 0: any free port') walks into it.
- The viewer adapter (goal 3) may want to show a socket's real port and could not.

### Keep getPort() and add a new getLocalPort()

**Pros:**
- Nothing existing changes its answer.
- The real port becomes available.

**Cons:**
- Adds a method to a public interface, and leaves a method beside it that always says -1.
- Two methods where one honest one would do; more to explain.

## Recommendation

Make getPort() give the listening port. The -1 is plainly a slip (the constructor takes "port", the getter is called getPort, and the address getter beside it gives the local address), nobody can be relying on it, and the fix is one line with two tests flipped. Leaving it keeps a trap right where the new builder invites people to use port 0. Adding a second method keeps the trap and grows the public interface. A run would do it as its own logged step, as with the earlier warts.

## If nothing is decided

getPort() keeps answering -1, pinned by two tests. Everything built so far works: nothing in the library calls it. A host that opens a socket on port 0 cannot learn its port.

## Ruling

**getPort() gives the port the socket listens on** (`getport-gives-the-port-the-socket-listens-on`). Tog, 2026-11-11, from the project pages.
