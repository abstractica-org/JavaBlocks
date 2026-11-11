---
type: project-node
summary: "Open at the start of any JavaBlocks work — a plain-Java dataflow library where blocks wire by handing references, only `ThreadBlock` owns a thread and only `BufferBlock` holds back-pressure; the tree holds this orientation and the thread-model node."
verified: 2026-11-11
---

# JavaBlocks

A block programming approach for multithreaded pipelines — a plain Java library of push/pull/thread/buffer/map blocks wired into dataflow pipelines.

## Orientation

JavaBlocks is the owner's pet project (2022, `v0.2.0` 2025-01-17),
handwritten alone, and proven in other projects outside this checkout.
It was brought into the foundation on 2026-08-26 for two reasons that
arrived together: it is the **second host vocabulary** for
`webapp-foundation`'s workflow viewer (the proof that the viewer stays
domain-neutral — workflows and blocks on one island), and the owner
ruled that it gains a **data-driven assembly** — a declarative way to
build a pipeline — which is the step that lets the viewer design
JavaBlocks pipelines rather than only draw them.

**Order of work** (Tog, 2026-11-09, `tools/autopilot/GOALS.md`):
first the known warts, each fixed or ruled to stay, one ruling each
(`decisions/2026-11-09-*.md`); then the declarative assembly; then the
viewer adapter, which waits for `wiring-playground`'s graph contract.
The base is clean before any public API is built on it. Goal 1 is
done: every known wart was fixed under its own ruling (J4–J11, merged
2026-11-09). **Goal 2 is done too** (2026-11-11): the
declarative assembly's form (J12), names, catalogue and map slot (J13)
were ruled and built in `design/` — `Design`, `Catalogue`, `Designer`,
`Pipeline`, push/pull ports, every ruled kind, the map node as a nested
Design per key (J14) and the socket node (J17, after the socket wart's
fix, J16) — and FirstTest (J15) and the `examples/udp` pipelines (J18)
are built through it, with tests. Goal 3 waits for `wiring-playground`'s
graph contract. One wart is open, pinned and with the owner: a socket
block's `getPort()` always answers -1
(`decisions/2026-11-11-should-a-socket-block-s-getport-give-the.md`).

## The model in one paragraph

A block is a Java object. `Output<E>` is a sink (`put`), `Input<E>` is
a source (`get`) — named from the wire's point of view, not the node's.
Wiring is handing one block's reference to another through a setter or
constructor; there is no wire object and no registry. `ThreadBlock` is
the only thread owner: its loop is `output.put(input.get())`, so it is
the joint between a pull chain upstream and a push chain downstream.
`BufferBlock` is the inverse joint (push in, pull out) and the only
place back-pressure lives. `DistributorBlock` is the only broadcast.
`MapBlock` demultiplexes by key into handlers a factory creates on the
first message with an unseen key — one design, N instances, N a runtime
function of the stream. Everything else (`IfBlock`, `VariableBlock`,
`ConstantBlock`, the UDP blocks, the assemblies) composes those.

## Where things are

- `src/main/java/org/abstractica/javablocks/` — all the code, ~1,900
  lines: `blocks/basic/` (the interfaces and the `BasicBlocks`
  factory), `blocks/basic/impl/` (their implementations),
  `assemblies/`, `blocks/udp/`, `blocks/connection/` (the ping-pong
  protocol on top of `MapBlock`, and `firsttest/FirstTest`, the only
  runnable check), `examples/`, `reporter/`, and `design/` — the
  declarative assembly (J13).
- `src/test/` — 121 tests: the characterization tests of the blocks (49: 34 from J3, grown with each cleanup J4–J11, the socket fix J16 and the `getPort` pin), the `design/` tests (62: 29 core, 21 map node, 12 socket node), `FirstTestTest` (3, J15) and `UdpExamplesTest` (7, J18); see `design/` in the tree.
- `pom.xml` — `org.abstractica:javablocks:0.3.0-SNAPSHOT`, Java 25,
  JUnit 5 (J2).
- `design/decision-log.md` — this repo's record, entries **J1…**.
- `STARTUP.md` — the handoff between sessions.
- `knowledge/` — this tree.

The viewer-side reading of this library — its model, the map block in
depth, the mapping onto the viewer's vocabulary and its gaps — is
`wiring-playground/design/javablocks-map-block.md` with the steward's
assessment beside it. Neither is duplicated here.

## What this tree holds

- `thread-model/` — the settled thread-domain model and the
  `stop()` contract, crystallized from the J3 characterization tests
  and updated for the J4 fix.
- `design/` — the declarative assembly as ruled (J12–J14) and built (through J17):
  Design, Catalogue, Designer, Pipeline, the kind table and its checks,
  the map node.
