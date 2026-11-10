package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.MapBlock;
import org.abstractica.javablocks.blocks.basic.Output;
import org.abstractica.javablocks.testsupport.Async;
import org.abstractica.javablocks.testsupport.Sink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** The map node (J14): a nested Design, copied once per key. */
@Timeout(10)
class MapNodeTest
{
	private Pipeline pipeline;
	/** One recording sink per key, made by the key function the tests give hostSinkPerKey. */
	private final Map<String, Sink<String>> sinks = new ConcurrentHashMap<>();
	private final AtomicInteger made = new AtomicInteger();

	@AfterEach
	void stop() throws InterruptedException
	{
		if (pipeline != null) pipeline.stop();
	}

	private Output<String> sinkFor(String key)
	{
		made.incrementAndGet();
		Sink<String> s = new Sink<>();
		sinks.put(key, s);
		return s;
	}

	private static String keyOf(String message)
	{
		return message.split(":")[0];
	}

	/** A map whose per-key copy is only a host object made from the key: runs on the putting thread. */
	private MapNode<String, String> directMap(Designer d)
	{
		return d.map("peers", (String m) -> keyOf(m), perKey ->
		{
			var show = perKey.hostSinkPerKey("show", this::sinkFor);
			perKey.wire(perKey.entry(), show.in());
		});
	}

	/** A map whose per-key copy has its own buffer and thread. */
	private MapNode<String, String> threadedMap(Designer d)
	{
		return d.map("peers", (String m) -> keyOf(m), perKey ->
		{
			var buffer = perKey.<String>buffer("buffer", 4);
			var worker = perKey.<String>thread("worker");
			var show = perKey.hostSinkPerKey("show", this::sinkFor);
			perKey.wire(perKey.entry(), buffer.in());
			perKey.wire(buffer.out(), worker.in());
			perKey.wire(worker.out(), show.in());
		});
	}

	@Test
	void theDesignerWritesTheNestedDesignAsData()
	{
		Designer d = new Designer();
		directMap(d);
		Design design = d.design();

		Design.Node map = design.nodes().get(0);
		assertEquals("map", map.kind());
		assertEquals("peers.function", map.settings().get("function"));
		Design perKey = (Design) map.settings().get("perKey");
		assertEquals(List.of(new Design.Node("entry", "entry", Map.of()),
						new Design.Node("show", "hostSinkPerKey", Map.of("object", "peers.show.object"))),
				perKey.nodes());
		assertEquals(List.of(new Design.Wire(new Design.Port("entry", "out"), new Design.Port("show", "in"))),
				perKey.wires());
		assertTrue(d.catalogue().contains("peers.function"));
		assertTrue(d.catalogue().contains("peers.show.object"));
	}

	@Test
	void eachKeyGetsItsOwnCopyMadeOnce() throws InterruptedException
	{
		Designer d = new Designer();
		var peers = directMap(d);
		pipeline = d.design().build(d.catalogue());
		MapBlock<String, String, ?> map = pipeline.block(peers);

		map.put("a:1");
		map.put("b:1");
		map.put("a:2");

		assertEquals(2, made.get());
		assertEquals(2, map.getSize());
		assertEquals(List.of("a:1", "a:2"), sinks.get("a").items());
		assertEquals(List.of("b:1"), sinks.get("b").items());
		assertSame(Thread.currentThread(), sinks.get("a").threads().get(0));
		for (var handler : map.getAllHandlers())
		{
			assertTrue(List.of("a", "b").contains(handler.getKey()));
		}
	}

	@Test
	void aCopyRunsOnItsOwnThreadAndStopsWithThePipeline() throws InterruptedException
	{
		Designer d = new Designer();
		var peers = threadedMap(d);
		pipeline = d.design().build(d.catalogue());
		pipeline.start();
		MapBlock<String, String, ?> map = pipeline.block(peers);
		assertFalse(pipeline.isRunning(), "no key yet, so no thread");

		map.put("a:1");
		map.put("b:1");
		assertTrue(pipeline.isRunning());
		assertTrue(Async.becomesTrueWithin(() -> sinks.size() == 2
				&& sinks.get("a").items().size() == 1 && sinks.get("b").items().size() == 1, 2000));
		Thread a = sinks.get("a").threads().get(0);
		Thread b = sinks.get("b").threads().get(0);
		assertNotSame(Thread.currentThread(), a);
		assertNotSame(a, b);

		pipeline.stop();
		assertFalse(a.isAlive());
		assertFalse(b.isAlive());
		assertFalse(pipeline.isRunning());
	}

	@Test
	void removingAHandlerStopsItsCopyAndTheKeyGetsAFreshOne() throws InterruptedException
	{
		Designer d = new Designer();
		var peers = threadedMap(d);
		pipeline = d.design().build(d.catalogue());
		pipeline.start();
		MapBlock<String, String, ?> map = pipeline.block(peers);
		map.put("a:1");
		map.put("b:1");
		assertTrue(sinks.get("a").receivedWithin(1, 2000));
		Thread first = sinks.get("a").threads().get(0);

		assertNotNull(map.removeHandler("a"));
		assertFalse(first.isAlive());
		assertTrue(pipeline.isRunning(), "b's copy still runs");
		assertEquals(1, map.getSize());

		map.put("a:2");
		assertEquals(3, made.get());
		assertTrue(sinks.get("a").receivedWithin(1, 2000));
		assertEquals(List.of("a:2"), sinks.get("a").items());
		assertNotSame(first, sinks.get("a").threads().get(0));
	}

	@Test
	void aCopyMadeWhileStoppedStartsWithThePipeline() throws InterruptedException
	{
		Designer d = new Designer();
		var peers = threadedMap(d);
		pipeline = d.design().build(d.catalogue());
		MapBlock<String, String, ?> map = pipeline.block(peers);

		map.put("a:1"); // the copy is built, its item waits in the copy's buffer
		assertEquals(1, made.get());
		assertFalse(pipeline.isRunning());
		Thread.sleep(200);
		assertTrue(sinks.get("a").items().isEmpty());

		pipeline.start();
		assertTrue(sinks.get("a").receivedWithin(1, 2000));
		assertTrue(pipeline.isRunning());

		pipeline.stop();
		map.put("b:1"); // a key arriving after the stop: built, not started
		Thread.sleep(200);
		assertFalse(pipeline.isRunning());
		assertTrue(sinks.get("b").items().isEmpty());
	}

	@Test
	void aThreadFeedingTheMapStopsBeforeTheCopies() throws InterruptedException
	{
		var input = org.abstractica.javablocks.blocks.basic.BasicBlocks.<String>getBufferBlock(8);
		Designer d = new Designer();
		var source = d.hostSource("source", input);
		var driver = d.<String>thread("driver");
		var peers = threadedMap(d);
		d.wire(source.out(), driver.in());
		d.wire(driver.out(), peers.in());
		pipeline = d.design().build(d.catalogue());
		pipeline.start();

		input.put("a:1");
		input.put("b:1");
		input.put("a:2");
		assertTrue(Async.becomesTrueWithin(() -> sinks.size() == 2
				&& sinks.get("a").items().size() == 2 && sinks.get("b").items().size() == 1, 2000));
		assertEquals(List.of("a:1", "a:2"), sinks.get("a").items());

		pipeline.stop();
		assertFalse(pipeline.isRunning());
		assertFalse(sinks.get("a").threads().get(0).isAlive());
	}

	@Test
	void stoppingWaitsForAFeederBlockedOnAFullCopy() throws InterruptedException
	{
		// The copy's buffer holds one item and its sink is slow, so the feeding thread is mostly blocked
		// putting into the copy. Stopping the copies first would leave that put blocked for ever.
		var input = org.abstractica.javablocks.blocks.basic.BasicBlocks.<String>getBufferBlock(16);
		Sink<String> received = new Sink<>();
		Output<String> slow = item ->
		{
			Thread.sleep(50);
			received.put(item);
		};
		Designer d = new Designer();
		var source = d.hostSource("source", input);
		var driver = d.<String>thread("driver");
		var peers = d.map("peers", (String m) -> keyOf(m), perKey ->
		{
			var buffer = perKey.<String>buffer("buffer", 1);
			var worker = perKey.<String>thread("worker");
			var show = perKey.hostSinkPerKey("show", key -> slow);
			perKey.wire(perKey.entry(), buffer.in());
			perKey.wire(buffer.out(), worker.in());
			perKey.wire(worker.out(), show.in());
		});
		d.wire(source.out(), driver.in());
		d.wire(driver.out(), peers.in());
		pipeline = d.design().build(d.catalogue());
		pipeline.start();
		for (int i = 0; i < 10; i++) input.put("a:" + i);
		assertTrue(received.receivedWithin(1, 2000));

		pipeline.stop();
		assertFalse(pipeline.isRunning());
	}

	@Test
	void mapsNest() throws InterruptedException
	{
		Designer d = new Designer();
		var outer = d.map("outer", (String m) -> m.substring(0, 1), byFirst ->
		{
			var inner = byFirst.map("inner", (String m) -> m.substring(0, 2), bySecond ->
			{
				var show = bySecond.hostSinkPerKey("show", this::sinkFor);
				bySecond.wire(bySecond.entry(), show.in());
			});
			byFirst.wire(byFirst.entry(), inner.in());
		});
		assertTrue(d.catalogue().contains("outer.inner.show.object"));
		pipeline = d.design().build(d.catalogue());
		MapBlock<String, String, ?> map = pipeline.block(outer);

		map.put("ab1");
		map.put("ac1");
		map.put("ab2");
		map.put("bb1");

		assertEquals(2, map.getSize());
		assertEquals(List.of("ab1", "ab2"), sinks.get("ab").items());
		assertEquals(List.of("ac1"), sinks.get("ac").items());
		assertEquals(List.of("bb1"), sinks.get("bb").items());
	}

	@Test
	void aPerKeyObjectThatIsNoOutputFailsThePutAndLeavesNoHandler() throws InterruptedException
	{
		Designer d = new Designer();
		var peers = d.map("peers", (String m) -> keyOf(m), perKey ->
		{
			var show = perKey.hostSinkPerKey("show", key -> key.equals("bad") ? null : sinkFor(key));
			perKey.wire(perKey.entry(), show.in());
		});
		pipeline = d.design().build(d.catalogue());
		MapBlock<String, String, ?> map = pipeline.block(peers);

		var e = assertThrows(IllegalStateException.class, () -> map.put("bad:1"));
		assertTrue(e.getMessage().contains("'show'"), e.getMessage());
		assertEquals(0, map.getSize());
		map.put("good:1");
		assertEquals(List.of("good:1"), sinks.get("good").items());
	}
	@Test
	void copiesThatRanStartAgainWithThePipeline() throws InterruptedException
	{
		Designer d = new Designer();
		var peers = threadedMap(d);
		pipeline = d.design().build(d.catalogue());
		pipeline.start();
		MapBlock<String, String, ?> map = pipeline.block(peers);
		map.put("a:1");
		assertTrue(sinks.get("a").receivedWithin(1, 2000));
		pipeline.stop();

		map.put("a:2"); // waits in the stopped copy's buffer
		Thread.sleep(200);
		assertEquals(1, sinks.get("a").items().size());
		pipeline.start();
		assertTrue(sinks.get("a").receivedWithin(2, 2000));
		assertEquals(1, made.get(), "the same copy, restarted");
	}

	@Test
	void aRemovalInterruptedBeforeItsStopLeavesTheCopyToThePipelineStop() throws InterruptedException
	{
		// The copy's worker is mid-put in a slow sink when an interrupted thread removes its handler: that
		// stop gives up waiting at once, so the copy must stay tracked for the pipeline's stop to wait for.
		java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
		java.util.concurrent.atomic.AtomicReference<Thread> worker = new java.util.concurrent.atomic.AtomicReference<>();
		Designer d = new Designer();
		var peers = d.map("peers", (String m) -> keyOf(m), perKey ->
		{
			var buffer = perKey.<String>buffer("buffer", 4);
			var thread = perKey.<String>thread("worker");
			var show = perKey.hostSinkPerKey("show", key -> (Output<String>) item ->
			{
				worker.set(Thread.currentThread());
				entered.countDown();
				Thread.sleep(500);
			});
			perKey.wire(perKey.entry(), buffer.in());
			perKey.wire(buffer.out(), thread.in());
			perKey.wire(thread.out(), show.in());
		});
		pipeline = d.design().build(d.catalogue());
		pipeline.start();
		MapBlock<String, String, ?> map = pipeline.block(peers);
		map.put("a:1");
		assertTrue(entered.await(2, java.util.concurrent.TimeUnit.SECONDS));

		Thread.currentThread().interrupt();
		map.removeHandler("a");
		assertTrue(Thread.interrupted(), "the interrupt is kept");
		assertTrue(pipeline.isRunning(), "the copy is still tracked while its worker finishes its put");

		worker.get().join(2000);
		assertFalse(worker.get().isAlive());
		pipeline.start();
		assertFalse(pipeline.isRunning(), "a removed copy never starts again");
		pipeline.stop();
		pipeline.start();
		assertFalse(pipeline.isRunning());
	}

	@Test
	void aThreadPuttingIntoARemovedFullCopyStaysBlocked() throws InterruptedException
	{
		// Pinned as a known limit (J14): the removed copy's buffer is full and nothing drains it.
		// Every step is forced: the worker holds a:0 in the sink, a:1 fills the buffer, the feeder waits
		// to put a:2 inside the copy; the removal waits for the worker's put; only then is it let go.
		java.util.concurrent.CountDownLatch gate = new java.util.concurrent.CountDownLatch(1);
		java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
		Sink<String> received = new Sink<>();
		Designer d = new Designer();
		var peers = d.map("peers", (String m) -> keyOf(m), perKey ->
		{
			var buffer = perKey.<String>buffer("buffer", 1);
			var worker = perKey.<String>thread("worker");
			var show = perKey.hostSinkPerKey("show", key -> (Output<String>) item ->
			{
				entered.countDown();
				gate.await();
				received.put(item);
			});
			perKey.wire(perKey.entry(), buffer.in());
			perKey.wire(buffer.out(), worker.in());
			perKey.wire(worker.out(), show.in());
		});
		pipeline = d.design().build(d.catalogue());
		pipeline.start();
		MapBlock<String, String, ?> map = pipeline.block(peers);
		Thread feeder = new Thread(() ->
		{
			try
			{
				for (int i = 0; i < 3; i++) map.put("a:" + i);
			} catch (InterruptedException e)
			{
				// released by the test
			}
		});
		feeder.start();
		assertTrue(entered.await(2, java.util.concurrent.TimeUnit.SECONDS));
		assertTrue(Async.becomesTrueWithin(() -> feeder.getState() == Thread.State.WAITING, 2000));
		Thread.sleep(100); // past any wake-up still on its way: the feeder now waits to put a:2
		assertEquals(Thread.State.WAITING, feeder.getState());

		Thread remover = new Thread(() -> map.removeHandler("a"));
		remover.start();
		assertTrue(Async.becomesTrueWithin(() -> remover.getState() == Thread.State.WAITING, 2000));
		gate.countDown();
		remover.join(2000);
		assertFalse(remover.isAlive());

		feeder.join(500);
		assertTrue(feeder.isAlive(), "blocked putting into the removed copy");
		assertEquals(List.of("a:0"), received.items());
		feeder.interrupt();
		feeder.join(2000);
		assertFalse(feeder.isAlive());
	}

	@Test
	void aPerKeyFunctionThatThrowsFailsThePutNamingItsNode()
	{
		Designer d = new Designer();
		var peers = d.map("peers", (String m) -> keyOf(m), perKey ->
		{
			var show = perKey.hostSinkPerKey("show", (String key) -> { throw new RuntimeException("boom"); });
			perKey.wire(perKey.entry(), show.in());
		});
		pipeline = d.design().build(d.catalogue());
		MapBlock<String, String, ?> map = pipeline.block(peers);

		var e = assertThrows(IllegalStateException.class, () -> map.put("a:1"));
		assertTrue(e.getMessage().contains("node 'show'"), e.getMessage());
		assertEquals("boom", e.getCause().getMessage());
		assertEquals(0, map.getSize());
	}

	@Test
	void idsInsideAreTheirOwnAndAGivenNameIsUsedAsGiven() throws InterruptedException
	{
		Sink<String> outerShow = new Sink<>();
		Designer d = new Designer();
		var show = d.hostSink("show", outerShow);
		var peers = d.map("peers", (String m) -> keyOf(m), perKey ->
		{
			var upper = perKey.push("show", "shared.upper", (String m) -> m.toUpperCase());
			var sink = perKey.hostSinkPerKey("sink", this::sinkFor);
			perKey.wire(perKey.entry(), upper.in());
			perKey.wire(upper.out(), sink.in());
		});
		assertTrue(d.catalogue().contains("shared.upper"));
		assertTrue(d.catalogue().contains("peers.sink.object"));
		pipeline = d.design().build(d.catalogue());

		pipeline.block(peers).put("a:x");
		assertEquals(List.of("A:X"), sinks.get("a").items());
		assertTrue(outerShow.items().isEmpty());
		assertSame(outerShow, pipeline.block(show));
	}

	@Test
	void aRefusedMapLeavesTheCatalogueAsItWas()
	{
		Designer d = new Designer();
		d.push("other", "taken", (String s) -> s);
		assertThrows(IllegalArgumentException.class, () -> d.map("peers", "taken", (String m) -> keyOf(m), perKey ->
		{
			var show = perKey.hostSinkPerKey("show", this::sinkFor);
			perKey.wire(perKey.entry(), show.in());
		}));
		assertFalse(d.catalogue().contains("peers.show.object"));
		assertEquals(1, d.design().nodes().size());
	}

	@Test
	void anIdWithADotCanCollideWithANestedNameAndIsRefusedAtOnce()
	{
		Designer d = new Designer();
		d.hostSink("peers.show", new Sink<String>());
		var e = assertThrows(IllegalArgumentException.class, () -> d.map("peers", (String m) -> keyOf(m), perKey ->
		{
			var show = perKey.hostSinkPerKey("show", this::sinkFor);
			perKey.wire(perKey.entry(), show.in());
		}));
		assertTrue(e.getMessage().contains("'peers.show.object'"), e.getMessage());
	}

	// --- refusals: a nested Design is checked in full when the outer one is built ---

	private static String refusal(Design design, Catalogue catalogue)
	{
		return assertThrows(IllegalArgumentException.class, () -> design.build(catalogue)).getMessage();
	}

	private static Design.Wire wire(String from, String to)
	{
		String[] f = from.split("\\.");
		String[] t = to.split("\\.");
		return new Design.Wire(new Design.Port(f[0], f[1]), new Design.Port(t[0], t[1]));
	}

	@Test
	void entryAndPerKeySinksAreRefusedOutsideAMap()
	{
		java.util.function.Function<String, Output<String>> fn = this::sinkFor;
		Design design = new Design(
				List.of(new Design.Node("entry", "entry", Map.of()),
						new Design.Node("show", "hostSinkPerKey", Map.of("object", "fn"))),
				List.of(wire("entry.out", "show.in")));

		String message = refusal(design, new Catalogue().put("fn", fn));

		assertTrue(message.contains("node 'entry': kind 'entry' is allowed only inside a map's nested Design"), message);
		assertTrue(message.contains("node 'show': kind 'hostSinkPerKey' is allowed only inside a map's nested Design"), message);
	}

	@Test
	void everyProblemInsideANestedDesignIsNamedWithItsMap()
	{
		java.util.function.Function<String, String> key = s -> s;
		Design inner = new Design(
				List.of(new Design.Node("entry", "entry", Map.of()), new Design.Node("x", "nonsense", Map.of())),
				List.of());
		Design design = new Design(
				List.of(new Design.Node("peers", "map", Map.of("function", "key", "perKey", inner))),
				List.of());

		String message = refusal(design, new Catalogue().put("key", key));

		assertTrue(message.contains("in map 'peers': port entry.out must be wired"), message);
		assertTrue(message.contains("in map 'peers': node 'x' has unknown kind 'nonsense'"), message);
	}

	@Test
	void aMapNeedsANestedDesignWithExactlyOneEntry()
	{
		java.util.function.Function<String, String> key = s -> s;
		Design noEntry = new Design(List.of(new Design.Node("t", "trashcan", Map.of())), List.of());
		Design design = new Design(
				List.of(new Design.Node("a", "map", Map.of("function", "key")),
						new Design.Node("b", "map", Map.of("function", "key", "perKey", "not a design")),
						new Design.Node("c", "map", Map.of("function", "key", "perKey", noEntry))),
				List.of());

		String message = refusal(design, new Catalogue().put("key", key));

		assertTrue(message.contains("node 'a' needs setting 'perKey' as Design"), message);
		assertTrue(message.contains("node 'b' needs setting 'perKey' as Design"), message);
		assertTrue(message.contains("in map 'c': a map's nested Design needs exactly one node of kind 'entry'"), message);
	}

	@Test
	void aNestedBufferOfCapacityZeroIsRefusedWhenThePipelineIsBuilt()
	{
		Designer d = new Designer();
		d.map("peers", (String m) -> keyOf(m), perKey ->
		{
			var buffer = perKey.<String>buffer("buffer", 0);
			var worker = perKey.<String>thread("worker");
			var show = perKey.hostSinkPerKey("show", this::sinkFor);
			perKey.wire(perKey.entry(), buffer.in());
			perKey.wire(buffer.out(), worker.in());
			perKey.wire(worker.out(), show.in());
		});

		String message = refusal(d.design(), d.catalogue());

		assertTrue(message.contains("in map 'peers': node 'buffer': buffer capacity must be at least 1, was 0"), message);
	}

	@Test
	void aMapWhoseLambdaTakesItsIdIsRefusedAndMergesNothing()
	{
		Designer d = new Designer();
		assertThrows(IllegalArgumentException.class, () -> d.map("peers", (String m) -> keyOf(m), perKey ->
		{
			d.trashcan("peers");
			var show = perKey.hostSinkPerKey("show", this::sinkFor);
			perKey.wire(perKey.entry(), show.in());
		}));
		assertFalse(d.catalogue().contains("peers.show.object"));
		assertFalse(d.catalogue().contains("peers.function"));
	}
}
