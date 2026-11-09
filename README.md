# JavaBlocks

JavaBlocks is a small, plain Java library for building multithreaded
dataflow pipelines out of blocks. A block is an ordinary Java object:
an `Output<E>` accepts items with `put`, an `Input<E>` supplies them
with `get`, and you wire blocks together by handing one block to
another through a setter. `ThreadBlock` is the only block that owns a
thread (its loop moves items from its input to its output),
`BufferBlock` is where back-pressure lives, `DistributorBlock`
broadcasts, and `MapBlock` routes items by key to handlers created on
first sight of each new key. On top of those sit if/constant/variable
blocks, UDP socket blocks, and a ping-pong connection protocol.

It is for Java developers who want explicit, inspectable thread
ownership in a pipeline without a framework: no dependencies at
runtime, everything built through static factories such as
`BasicBlocks`.

## Status

**Active** — version 0.2.1 is the 0.2.0 code pinned by
characterization tests that describe its concurrency behaviour. Since
then, on `main`, every known defect has been fixed one at a time: thread
blocks always stop, and the buffer, distributor, map, delay, reporter
and ping-pong cleanups are in (43 tests). Next comes a declarative way
to assemble pipelines: its form is chosen (a pipeline described as
plain data, written by a typed Java builder), and its names are being
decided. (2026-11-09)

## Quick start

Requires Java 25 and Maven. `mvn test` runs the characterization
suite. A minimal pipeline, keyboard to console through one thread:

```java
Input<String> keyboard = BasicBlocks.getKeyboardBlock();
Output<String> console = BasicBlocks.getConsoleBlock();
PushBlock<String, String> push = BasicBlocks.getPushBlock(s -> s + "!");
push.setOutput(console);
ThreadBlock<String> thread = BasicBlocks.getThreadBlock();
thread.setInput(keyboard);
thread.setOutput(push);
thread.start();
```

More in `src/main/java/org/abstractica/javablocks/examples/`.

## Documentation

- [Original README](docs/original-readme.md)
- [Design log](design/decision-log.md)
- [Knowledge tree](knowledge/) — including the thread model
- [Open decisions](decisions/)
