# Goals — javablocks

Ruled by Tog, 2026-11-09, from the project pages: **Cleanups first, then
the assembly** (`cleanups-first-then-the-assembly`), in
`decisions/2026-11-09-set-the-goals-for-javablocks.md`. "The same three
goals, with cleanups first: the base is fully clean before any public API
is built on it." The goals, in that order, as the record states them:

1. **The remaining cleanups, one at a time.** For each wart a run writes a
   decision record; you rule; a run fixes it with the tests changed
   visibly. Done when every wart is fixed or you have ruled to leave it.
2. **The declarative assembly.** A run proposes the builder's outside
   shape: blocks with kinds, typed connection points, wires, and the map
   block's per-key handler factory as a fill-in slot. You rule on it, then
   runs build it. Done when the FirstTest ping-pong pipeline and the
   examples/udp pipelines are rebuilt through it, with tests.
3. **The viewer adapter.** It starts only once wiring-playground has the
   graph contract. Done when the viewer's graph for the ping-pong pipeline
   builds a running pipeline through the assembly.
