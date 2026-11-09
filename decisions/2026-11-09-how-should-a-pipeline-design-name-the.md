---
id: 2026-11-09-how-should-a-pipeline-design-name-the
title: How should a pipeline Design name the Java functions and host objects it uses?
status: ruled
stakes: medium
confidence: 82
opened: 2026-11-09
ruled: 2026-11-09
repo: javablocks
by: 184507c0-5a81-4fd6-9d06-8dee2a5ba320
recommends: names-in-the-design-objects-in-a-catalogue-the-builder-fills-both
---

## Problem

You ruled that a pipeline is described as plain data (a "Design") with a typed Java builder on top. Many blocks need a piece of Java code, not data: a push or pull block needs a function (add "!", turn a packet into a string), the if block needs a test, the ping-pong block needs its protocol, and FirstTest hands the pipeline the host's own objects (its `SimpleSender`, which prints what would be sent). A Java lambda cannot be written into plain data, read back, or offered to the viewer. This record decides how a Design refers to them. It matters now because the builder's methods and the Design's settings depend on the answer, and later because the viewer (goal 3) can only offer what has a name.

Host objects need a node kind of their own either way: the names record proposes `hostSink` (a host's own Output, something you push into) and `hostSource` (a host's own Input, something you pull from).

What the recommended option looks like:

  Designer d = new Designer();
  var shout = d.push("shout", s -> s + "!");          // stored as function "shout.function"
  var tidy  = d.push("tidy", "trim", String::trim);    // stored as function "trim"
  Design design = d.design();       // settings hold only names: {"function": "shout.function"}
  Catalogue c = d.catalogue();      // the names and the Java objects they stand for
  Pipeline p = design.build(c);     // a Design read from elsewhere is built with any catalogue that has its names

A Design whose names the catalogue lacks is refused at build time, naming the missing entries.

The value held by a constant or variable block is stored by name too when it is not plain data (a number or a string is stored as itself; a `DatagramPacket` is stored as a catalogue name). The words `Catalogue`, `catalogue()` and the named form `push(id, name, fn)` (the same for pull, if and ping-pong) are this record's own proposals, ruled together with it.

## Options

### Names in the Design, objects in a catalogue; the builder fills both

**Pros:**
- The Design stays plain data all the way down: it can be printed, compared, saved and drawn, as the ruling intends.
- Java authors still pass a lambda; the builder names it after its node ("shout.function") unless they choose a name.
- The viewer adapter later only needs a catalogue of named entries to offer; the same mechanism, nothing new.
- A missing entry is a clear error at build time, not a NullPointerException in a worker thread.

**Cons:**
- Two objects travel together (the Design and its catalogue), one more thing to explain.
- Library functions that take arguments (a delay of N ms, an address and port) are stored as ready objects under a generated name; a catalogue of parameterised functions the viewer can offer as a palette is left for goal 3.

### The Java objects sit inside the Design

**Pros:**
- Simplest: a node's settings simply hold the function object; no catalogue.
- Nothing extra for Java authors to learn.

**Cons:**
- The Design is no longer plain data: it cannot be printed meaningfully, saved or sent to the viewer.
- The viewer adapter would have to invent naming later, reworking the core it stands on.

### Every function registered by name first

**Pros:**
- Every name is chosen by a person, so the names the viewer shows are meaningful.
- No generated names.

**Cons:**
- Every pipeline in Java code grows a registration section before its wiring, longer than the code it replaces.
- The rebuilt FirstTest and UDP examples would read worse than today.

### Rule nothing yet

**Pros:**
- Decide when the viewer's needs are known.

**Cons:**
- Push, pull and if blocks, and the ping-pong block, cannot be built into a Design, so nothing useful can be built.

## Recommendation

Names in the Design, objects in a catalogue, with the builder filling both. It is the only option that keeps the Design plain data, which is why you chose that form, without making Java authors register every lambda by hand. Objects-inside-the-Design is simpler today but breaks the form at its root and would be redone for the viewer. Register-everything-first produces names a person chose, at the cost of every pipeline's readability, and the recommended option still allows a chosen name where it matters. The palette of parameterised library functions is a viewer concern and can wait for goal 3. Confidence 82.

## If nothing is decided

Nothing that needs a function (push, pull, if, ping-pong) can be put into a Design, so the builder is not started. Runs do housekeeping only.

## Ruling

**Names in the Design, objects in a catalogue; the builder fills both** (`names-in-the-design-objects-in-a-catalogue-the-builder-fills-both`). Tog, 2026-11-09, from the project pages.
