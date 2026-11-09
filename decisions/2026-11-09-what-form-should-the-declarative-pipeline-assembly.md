---
id: 2026-11-09-what-form-should-the-declarative-pipeline-assembly
title: What form should the declarative pipeline assembly take from the outside?
status: proposed
stakes: high
confidence: 80
opened: 2026-11-09
repo: javablocks
by: 6ed5b789-0d17-49e0-954b-b0784f4df23f
recommends: design-as-data-with-a-typed-java-builder-on-top
---

## Problem

Goal 1 is finished: every known defect in JavaBlocks has been fixed under your rulings (log entries J4–J11, 43 tests). Goal 2 is the declarative assembly: a way to describe a pipeline (which blocks, how they connect) instead of writing the constructor-and-setter code by hand. Its outside shape is yours to design. Your charter (J1) says why it matters: the workflow viewer must be able to design JavaBlocks pipelines, not just draw them, and that needs the wiring to exist as a readable object, "the reified design". Your goals ruling says it has blocks with kinds, typed connection points, wires, and the map block's per-key handler factory as a fill-in slot. It is done when the FirstTest ping-pong pipeline and the UDP examples are rebuilt through it, with tests.

Today a pipeline exists only as Java code. For example, keyboard → add "!" → console means: build four blocks, then `push.setOutput(console); thread.setInput(keyboard); thread.setOutput(push); thread.start();`. Nothing can be read back afterwards.

This record decides only the overall form. Exact class and method names, how the viewer names Java functions, and the map block's sub-design details come back as smaller follow-up records once the form is chosen. Nothing is built before you rule.

Here is roughly what the recommended option would look like for the example (a sketch, not final names):

  Assembly a = new Assembly();
  var keyboard = a.keyboard("keyboard");
  var shout    = a.push("shout", (String s) -> s + "!");
  var thread   = a.thread("thread");
  var console  = a.console("console");
  a.wire(keyboard, thread.in());
  a.wire(thread.out(), shout.in());
  a.wire(shout.out(), console);
  Design design = a.design();   // plain data: nodes (id, kind, settings) and wires (port to port)
  Pipeline p = design.build();  // checks it, then builds the real blocks through BasicBlocks
  p.start(); ... p.stop();       // starts and stops every thread block in it

A map block would take a nested design, one copy per key: a.map("peers", keyFn, perKey -> { ... }).

One naming fact for later: the package `assemblies/` already exists, holding `Assemblies.getBufferedSocketBlock`, a ready-made composite of a socket, two buffers and threads. The new builder's name must not collide with it; names come in the follow-up record.

## Options

### Design as data, with a typed Java builder on top

**Pros:**
- A Design object can be read back, checked, and drawn. It is the 'reified design' J1 asks for, and the viewer adapter later only has to turn the viewer's graph into a Design.
- Java authors still get compile-time type checks: the typed builder stops a String output being wired to a DatagramPacket input.
- Follows the foundation's own pattern for APIs (one flat core, a thin typed layer over it), so the two layers cannot drift.
- Build-time checks can turn a wiring mistake that today fails inside a worker thread (an unwired push block) or silently never moves (a pull chain no thread drives) into an error when the pipeline is built.
- The existing blocks and factories are untouched. The assembly only calls them, so the 43 tests keep guarding the behaviour.

**Cons:**
- Two layers to build and test, the largest of the options.
- Java lambdas inside a Design (the push block's function, the map block's key function) are not plain data. The viewer will need them as named entries in a catalogue, which is a follow-up decision.
- Adds a sizeable public API that hosts will build on, so mistakes in names are costly later.

### A typed Java builder only

**Pros:**
- Smallest step: a fluent builder that wires blocks and returns a runnable pipeline, close to how code is written today.
- Full compile-time typing, nothing stringly.

**Cons:**
- Defined narrowly here on purpose: a builder that only builds, with no Design object to read back, so the viewer cannot read or draw a pipeline, against J1's reason for the assembly. (A builder that also records what it built is the recommended option.)
- The viewer adapter would have to drive the builder by method name, a second hidden data format.
- Would likely be redone when the viewer arrives.

### Design as data only

**Pros:**
- The format closest to what a viewer exchanges: nodes and wires as plain records, block kinds looked up by name.
- One layer, so less to build.

**Cons:**
- Java hosts write pipelines as string ids and get type errors only at build time, a step back from today's typed code.
- Rebuilding FirstTest and the UDP examples this way reads worse than the code it replaces.

### Wait for the viewer's graph contract and mirror it

**Pros:**
- Guarantees the shape matches what the viewer will send.

**Cons:**
- J1 ruled that the Java-side builder does not wait for the contract, and that the contract is authored in wiring-playground, not shaped by its third consumer.
- Goal 2 stalls for an unknown time.

## Recommendation

Design as data, with a typed Java builder on top. Only this option gives both things the charter and goals ask for: a Design that can be read back, which the viewer needs, and typed connection points, which Java authors need. It follows the foundation's own pattern for APIs (one flat core, a thin typed layer over it). The builder-only option fails J1's main reason for the assembly. Data-only gives up typing that Java hosts already have. Waiting goes against J1. Its main cost, Java functions not being plain data, has to be solved for any viewer integration anyway. I would bring it back as its own small record, together with the exact names and the map block's per-key sub-design. Confidence 80. The form follows almost directly from J1 and the goals. What I am less sure of is how much checking the first version should do at build time, which can be decided while building.

## If nothing is decided

Nothing toward goal 2 is built. The library stays as it is (all defects fixed, 43 tests), and runs do only housekeeping until you rule.
