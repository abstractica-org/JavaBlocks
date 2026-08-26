# JavaBlocks — decision log

Entries are numbered **J1, J2, …** and never renumbered.

## J1 — Charter: a data-driven assembly, built on a pinned base (owner, 2026-08-26)

Ruled in a `wiring-playground` session, on the viewer-side report
`wiring-playground/design/javablocks-map-block.md` and its assessment:

- **JavaBlocks is a target, not only a test.** The viewer must be able
  to design JavaBlocks pipelines, not merely draw them. That needs the
  wiring reified: today it is imperative code (constructors and setters
  handing references), so there is no Design object to read. The
  remedy is a **data-driven assembly** — a declarative way to build a
  pipeline through the factories that already exist (`BasicBlocks`,
  `UDPBlocks`, the handler factories). The owner calls it the next
  natural step for the project.
- **Two layers, in this order.** First a **Java-side declarative
  assembly** (a builder — the reified design, typed, standing on its
  own). Then a thin **adapter from the viewer's graph contract** to
  that builder. The contract is written in `wiring-playground` jointly
  with `wiring`; the adapter waits for it, the builder does not. This
  keeps the viewer's contract authored by its owner, not by its third
  consumer.
- **The base is pinned before it is changed.** `v0.2.0` (`3076b2a`) is
  the reference. Characterization tests describe what it does — warts
  included — before any block is edited; the pom fix is the sole thing
  that precedes them, because nothing runs without it. Every later
  behaviour change is a deliberate change to a test.
- **Cleanup is separate from the feature**, one logged step per fix.
- **Independent of `wiring`.** Two hosts of one viewer, never
  dependent on each other.

*Rejected:* rendering existing programs by **introspection** (reflection
over fields, or a tracing `Output`/`Input` wrapper). It recovers
instances, never the design — holes and exemplars are invisible, and the
map block's handler wiring is buried in factory bodies. **Named revival
trigger:** a need to visualise a running JavaBlocks program that was
*not* built through the assembly.

*Risk named:* the code has no tests, and its value is concurrency
semantics an agent can break while "tidying". The pin-first rule and
the separate-cleanup rule are the mitigation; they are in `CLAUDE.md`.
