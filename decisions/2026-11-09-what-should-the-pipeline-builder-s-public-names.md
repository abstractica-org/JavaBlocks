---
id: 2026-11-09-what-should-the-pipeline-builder-s-public-names
title: "What should the pipeline builder's public names be?"
status: proposed
stakes: high
confidence: 82
opened: 2026-11-09
repo: javablocks
by: 184507c0-5a81-4fd6-9d06-8dee2a5ba320
recommends: design-designer-pipeline-with-push-and-pull-ports
---

## Problem

You ruled the declarative assembly's form: a pipeline is described as plain data (a "Design": blocks with an id, a kind and settings, plus wires between their connection points), and a typed Java builder on top writes that data for Java authors. Every public name is yours, so nothing is built until you rule on them. This record asks for the whole vocabulary at once: the package, the main types, the connection-point types, the builder's methods, and the kind names stored in the data. Two smaller records ask the other open questions (how a Design names Java functions; the map block's per-key slot).

One fact shapes the connection points. In JavaBlocks every connection is one of two things. A *push wire*: a block that holds a reference to an Output (a thread's output, a push block's output) is handed a block that IS an Output (a push block, a buffer, the console). A *pull wire*: a block that holds a reference to an Input (a thread's input, a pull block's input) is handed a block that IS an Input (a buffer, a pull block, the keyboard). Any other pairing is meaningless: wiring a buffer straight into a push block compiles today but nothing ever moves data. Four port types (push-out, push-in, pull-out, pull-in) let the Java compiler refuse those pairings.

FirstTest rebuilt with the recommended names (sketch; how functions and host objects get names in the data is the catalogue record):

  Designer d = new Designer();
  var keyboard = d.keyboard("keyboard");
  var driver   = d.thread("driver");
  var ping     = d.pingPong("ping", new SimplePingPongProtokol());
  var sender   = d.hostSink("sender", new SimpleSender());
  var freq     = d.constant("frequency", 5000L);
  var timer    = d.thread("timer");
  var delay    = d.push("delay", BasicFunctions.DelayFunction(10000L));
  d.wire(keyboard.out(), driver.in());     // pull wire
  d.wire(driver.out(), ping.in());         // push wire
  d.wire(ping.send(), sender.in());
  d.wire(freq.out(), timer.in());
  d.wire(timer.out(), delay.in());
  d.wire(delay.out(), ping.timer());
  Design design = d.design();              // plain data, readable
  Pipeline p = design.build(d.catalogue());
  p.start(); ... p.stop();

The recommended vocabulary in full:
- Package `org.abstractica.javablocks.design` (beside the existing `assemblies/`, no collision).
- `Design`: the plain data, with nested records `Design.Node(id, kind, settings)`, `Design.Wire(from, to)`, `Design.Port(node, name)`.
- `Designer`: the typed builder; `design()` returns the Design so far.
- `Pipeline`: what `build` returns; it is a `ThreadControl` (start, stop, isRunning: starts every thread block, stops them all, then closes sockets), and `pipeline.block(node)` gives a built block back, typed (e.g. to print a socket's address).
- Ports: `PushOut<E>`, `PushIn<E>`, `PullOut<E>`, `PullIn<E>`; `d.wire(...)` accepts only PushOut→PushIn and PullOut→PullIn.
- Builder handles: one per kind, named `<Kind>Node` (ThreadNode, PushNode, BufferNode, ...), each with named ports: `in()`, `out()`, and for the if block `whenTrue()`/`whenFalse()`, the ping-pong block `in()`/`timer()`/`send()`, the socket `in()`/`out()`, the distributor `out()` (the one port that takes many wires).
- Every node also gets three optional `PushOut<String>` ports `info()`, `debug()`, `error()`; left unwired they use the library's defaults (J10 said passing reporters in at build time belongs with the assembly).
- Kind names in the data and builder methods follow the existing factory methods: constant, variable, ifBlock, buffer, thread, distributor, pull, push, map, trashcan, keyboard, console, udpSocket, pingPong, plus hostSink/hostSource for a host's own Output/Input objects (`ifBlock` because `if` is a Java keyword).

## Options

### Design, Designer, Pipeline, with push and pull ports

**Pros:**
- Reads as what it is: you write a Design with a Designer and build a Pipeline from it; matches the word your ruling and J1 already use ("the reified design").
- Four port types make the compiler refuse the meaningless wirings (a buffer wired straight into a push block, a thread into a thread), which today compile and silently move nothing.
- Kind names mirror the BasicBlocks factory methods, so someone who knows the library finds every kind at once.
- The reporter ports finish what J10 left open, without any change to the blocks.

**Cons:**
- Four port types are more to learn than a plain in/out.
- "Design" is also a word the viewer uses; the adapter will have to keep the two apart (they are different repos, so no clash in code).
- About 15 small handle classes, one per kind.

### Blueprint vocabulary, same ports

**Pros:**
- Blueprint, BlueprintBuilder, Pipeline avoids the word "design", which the viewer side also uses.
- Same compile-time safety as the recommended option.

**Cons:**
- Departs from the word J1 and your ruling use, so the log and the code speak differently.
- Longer names in every line of host code.

### Design names, but only two port types (in and out)

**Pros:**
- Simplest to learn: every node has in() and out(), as in the form record's sketch.
- Fewer types in the public API.

**Cons:**
- Meaningless pairings (buffer into push block) compile and are only refused when the pipeline is built, not when it is written.
- The push/pull distinction, which is the heart of JavaBlocks' thread model, becomes invisible in the API.

### Rule nothing yet

**Pros:**
- Leaves time to look at the viewer side first.

**Cons:**
- Goal 2 cannot start: every class the builder needs has a public name.
- Runs do housekeeping only until you rule.

## Recommendation

Design, Designer, Pipeline, with push and pull ports. It keeps the word your ruling and J1 use. The four port types let the Java compiler enforce JavaBlocks' own rule that every connection is a push or a pull, and that rule is the library's whole thread model; the two-port variant gives that up for a slightly smaller API. Blueprint only renames, at the cost of speaking differently from the log. Kind names that mirror the factory methods cost nothing to learn. If you want any single name changed (say `Designer` → `DesignBuilder`), say so in the ruling and the run uses it. Confidence 82: the structure follows from the ruled form and the code. The names themselves are taste, and yours.

## If nothing is decided

Nothing of the assembly is built, because every class in it has a public name. Runs do housekeeping only.
