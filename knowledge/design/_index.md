---
type: project-node
summary: "The declarative assembly (J12–J14): a Design as plain data, a Catalogue of named Java objects, the typed Designer, a Pipeline built through the existing factories, and the map node as a nested Design copied per key; checked whole before building."
verified: 2026-11-10
related:
  - path: ..
    rationale: The orientation names the design package as goal 2's assembly.
  - path: ../thread-model
    rationale: A built Pipeline starts and stops thread blocks exactly as the thread model pins.
---

# Design — the declarative assembly

Package `org.abstractica.javablocks.design`; form ruled in J12, names,
catalogue and map slot in J13, the map node built in J14. The tests in `src/test/.../design/` are
the executable form of this node.

- **A Design is plain data**: `Node(id, kind, settings)`,
  `Wire(from, to)`, `Port(node, name)`. Settings hold only strings,
  numbers and booleans, except a map's `perKey`, a nested Design; a
  Java object is referred to by a catalogue
  name under a key that means "a catalogue name" (`function`,
  `predicate`, `protocol`, `object`, `valueName`).
- **The Catalogue** holds the Java objects by name. The Designer fills
  both the Design and the catalogue; a Design written by hand needs a
  catalogue with every name it uses.
- **One kind table** (`Kinds.java`) is the definition both the checks
  and the build read. A Design is refused with one
  `IllegalArgumentException` listing every problem: unknown kinds,
  nodes or ports, a wire that is neither push→push nor pull→pull, a
  single slot wired twice, a required slot unwired, a missing or
  wrongly typed catalogue entry, ping-pong senders in a cycle.
- **Building** goes through the existing factories (ping-pong blocks
  after their sender, which their constructor takes), then the wires
  through the existing setters. No existing block changed for it.
- **Java needs a type witness** for nodes whose methods take no typed
  argument: `d.<String>thread("driver")` (also buffer, distributor,
  trashcan). A method reference such as `String::toLowerCase` can be
  ambiguous; a lambda with a typed parameter is not. Whether to keep
  the witness is open:
  `decisions/2026-11-10-how-should-a-host-write-the-item.md`.
- **The map node** (J14): `d.map(id, keyFn, perKey -> { ... })`, with
  `perKey.entry()` (this key's messages) and
  `perKey.hostSinkPerKey(id, key -> output)`. The nested Design is
  checked with the outer one; on each new key the map builds a fresh
  copy, started if the pipeline runs. `Pipeline.stop()` stops its own
  threads first, then the copies; `removeHandler` stops one copy.
  A thread putting into a copy's full buffer stays blocked once that
  copy is removed (pinned) or the pipeline stopped, as wired by hand:
  remove when the copy is quiet. A copy cannot remove itself from its
  own thread (a thread cannot stop itself).
- **Refusals are early and leave nothing behind**: what the data can
  show wrong is refused at build, not at the first message (a capacity-0
  buffer inside a map's nested Design included). Host code the data
  cannot see fails at run time, in the put that runs it: a key function
  that throws, or a `hostSinkPerKey` function whose result is no Output
  or that throws (J14). A refused map merges nothing into the catalogue; the Pipeline copies the
  catalogue at build, so later edits to the caller's catalogue do not
  reach it. A removed copy stays tracked until its stop has finished,
  so an interrupted stopper loses no copy.
- **Not built:** `udpSocket`, which waits on
  `decisions/2026-11-10-should-a-thread-that-is-waiting-on.md` (a
  thread reading a UDP socket never stops).
