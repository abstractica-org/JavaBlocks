---
id: 2026-11-09-how-should-the-map-block-s-per-key-handler
title: "How should the map block's per-key handler slot be filled in a pipeline Design?"
status: done
stakes: medium
confidence: 80
opened: 2026-11-09
ruled: 2026-11-09
repo: javablocks
by: 184507c0-5a81-4fd6-9d06-8dee2a5ba320
recommends: a-nested-design-copied-once-per-key
---

## Problem

The map block is JavaBlocks' most distinctive piece: it sorts incoming messages by a key (for example, which peer sent them) and creates a separate handler the first time it sees a new key, so one design runs as N copies, with N decided by the traffic at run time. Today the handler comes from a Java factory object with three methods: read the key from a message, create a handler for a key, destroy a handler. Your goals ruling says the assembly has "the map block's per-key handler factory as a fill-in slot". Your words can be read two ways. Read literally, the slot is filled with a factory, and J1 (the charter) says the assembly builds "through the factories that already exist (… the handler factories)". But J1 also rejected reading pipelines back from running code because "the map block's handler wiring is buried in factory bodies", which a viewer cannot see. The form record you ruled already sketched the map as `a.map("peers", keyFn, perKey -> { ... })`, "a nested design, one copy per key", and left its details to this record; the factory reading is still open, so both are set out below. This record decides what fills that slot in a Design. (The names record proposes the words; the catalogue record decides how Java functions get names in the data.)

Two facts from the code. First, only the ping-pong block uses a map block today, and inside itself, so rebuilding FirstTest and the UDP examples (goal 2's "done") needs a `pingPong` kind but no map node; the slot is for hosts and for the viewer. Second, the map creates a handler while holding its lock, on the thread whose message first carried the key, and destroying a handler happens when someone calls removeHandler (as the ping-pong block now does for silent peers, J11).

What the recommended option looks like, a handler per peer that prints with its key:

  var peers = d.map("peers", (String msg) -> msg.split(":")[0], perKey -> {
      var show = perKey.hostSinkPerKey("show", key -> new SimpleHandler(key));  // one object per key
      perKey.wire(perKey.entry(), show.in());  // entry(): this key's messages
  });

In the data, the map node's setting holds a small nested Design with one boundary port, `entry`. The words `perKey`, `entry()` and `hostSinkPerKey` (a host object made from the key; a separate name, because a lambda could also be read as a plain `Output`; its kind in the data is `hostSinkPerKey` too) are this record's own proposals, ruled together with it. Anything inside it that depends on the key is given as a function of the key (a catalogue entry). For each new key the map builds a fresh copy of the nested Design and starts its thread blocks; when the handler is removed, the copy's threads are stopped and its sockets closed.

## Options

### A nested Design, copied once per key

**Pros:**
- The handler's wiring is data, so the viewer can draw and edit it: exactly what J1 says introspection cannot give.
- A handler can be a whole small pipeline (a buffer and a thread per peer), not only one object.
- Subsumes the factory form: a host with an existing handler class wraps it as one per-key host object (key -> new SimpleHandler(key)), as in the sketch.
- Builds on the same Design and catalogue as everything else; no second mechanism.

**Cons:**
- The largest of the options: copying, starting and tearing down sub-pipelines.
- A handler with threads is started under the map's lock on the putting thread, and stopping it on removal waits for its in-flight put (J4); both are visible costs that need tests.
- Stopping the whole pipeline must also stop every live per-key copy, and cope with a new key arriving while it stops, since the map creates handlers on whichever thread puts.
- No block combines two inputs, so the key reaches the inside only through functions of the key, not as a wire.

### A factory object from the catalogue

**Pros:**
- Smallest: the slot takes today's factory interface by name, unchanged.
- Nothing new to learn for anyone who has written a factory.

**Cons:**
- The viewer sees the map as a closed box: the handler wiring stays buried in Java, which is what J1 set out to fix.
- The slot holds Java code, so a viewer can choose a factory from the catalogue but not draw or edit what it builds.

### Factory object now, nested Design later

**Pros:**
- Goal 2's "done" (FirstTest, UDP examples) does not need a map node, so the big piece can wait for the viewer.
- Smaller first build.

**Cons:**
- Two public forms of the same slot, the first likely to be deprecated.
- The nested form gets designed under the viewer's pressure instead of now, while the core is being shaped.

### Rule nothing yet

**Pros:**
- The rest of the builder can be built without the map node.

**Cons:**
- The assembly ships without its most distinctive block; the goals ruling lists the slot as part of goal 2.

## Recommendation

A nested Design, copied once per key. Your goals ruling read literally ("handler factory as a fill-in slot") fits the factory option as well, and that option is the honest smaller alternative. I still recommend the nested Design because it is the only one that answers J1's objection: the per-key wiring becomes something the viewer can see and edit, not a factory body. It also covers the factory case, because an existing handler class becomes one per-key host object. The factory-only option is smaller but leaves the map a closed box, which is the reason the assembly exists. Factory-now-nested-later saves work in goal 2 only to add a second public form of the same slot later. The costs (starting under the lock, stopping waits) are the existing thread-model rules, and would be pinned by tests like every other behaviour. I would build the map node last within goal 2, after the kinds FirstTest and the UDP examples need. Confidence 80: the direction follows from J1's objection, but if by "factory" you meant exactly today's factory object, the second option is yours to pick. The detail I am least sure of is passing the key in only as functions of the key, and building it may show a better way, which would come back to you.

## If nothing is decided

The rest of the builder can still be built once the names and catalogue records are ruled; the map node waits, and goal 2 cannot be called done without it.

## Ruling

**A nested Design, copied once per key** (`a-nested-design-copied-once-per-key`). Tog, 2026-11-09, from the project pages.

## Done

2026-11-10, autopilot run 6: the map node is built as ruled (J14). `d.map(id, keyFn, perKey -> { ... })` with `perKey.entry()` and `perKey.hostSinkPerKey(...)`; the nested Design is checked with the outer one, copied and started once per key, and stopped with the pipeline or on `removeHandler`. 12 tests, each proven able to fail.
