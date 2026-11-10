---
type: project-node
summary: "The declarative assembly (J12, J13): a Design as plain data, a Catalogue of named Java objects, the typed Designer, and a Pipeline built through the existing factories; a Design is checked whole and every problem reported at once."
verified: 2026-11-10
related:
  - path: ..
    rationale: The orientation names the design package as goal 2's assembly.
  - path: ../thread-model
    rationale: A built Pipeline starts and stops thread blocks exactly as the thread model pins.
---

# Design — the declarative assembly

Package `org.abstractica.javablocks.design`; form ruled in J12, names,
catalogue and map slot in J13. The tests in `src/test/.../design/` are
the executable form of this node.

- **A Design is plain data**: `Node(id, kind, settings)`,
  `Wire(from, to)`, `Port(node, name)`. Settings hold only strings,
  numbers and booleans; a Java object is referred to by a catalogue
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
- **Not built:** `udpSocket`, which waits on
  `decisions/2026-11-10-should-a-thread-that-is-waiting-on.md` (a
  thread reading a UDP socket never stops).
