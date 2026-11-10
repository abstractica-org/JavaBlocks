package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.BasicBlocks;
import org.abstractica.javablocks.blocks.basic.BasicFunctions;
import org.abstractica.javablocks.blocks.basic.BufferBlock;
import org.abstractica.javablocks.blocks.basic.VariableBlock;
import org.abstractica.javablocks.blocks.connection.firsttest.SimplePingPongProtokol;
import org.abstractica.javablocks.testsupport.Async;
import org.abstractica.javablocks.testsupport.Sink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** A pipeline built from a Design behaves as the same pipeline wired by hand. */
@Timeout(10)
class PipelineTest
{
	private Pipeline pipeline;

	@AfterEach
	void stop() throws InterruptedException
	{
		if (pipeline != null) pipeline.stop();
	}

	@Test
	void aPushChainDeliversThroughItsThread() throws InterruptedException
	{
		BufferBlock<String> input = BasicBlocks.getBufferBlock(4);
		Sink<String> output = new Sink<>();
		Designer d = new Designer();
		var source = d.hostSource("source", input);
		var driver = d.<String>thread("driver");
		var shout = d.push("shout", (String s) -> s + "!");
		var sink = d.hostSink("sink", output);
		d.wire(source.out(), driver.in());
		d.wire(driver.out(), shout.in());
		d.wire(shout.out(), sink.in());
		pipeline = d.design().build(d.catalogue());

		assertFalse(pipeline.isRunning());
		pipeline.start();
		assertTrue(pipeline.isRunning());
		input.put("a");
		input.put("b");

		assertTrue(output.receivedWithin(2, 2000));
		assertEquals(List.of("a!", "b!"), output.items());
		assertNotSame(Thread.currentThread(), output.threads().get(0));
		pipeline.stop();
		assertFalse(pipeline.isRunning());
	}

	@Test
	void aBufferJoinsAPushChainToAPullChain() throws InterruptedException
	{
		BufferBlock<Integer> input = BasicBlocks.getBufferBlock(8);
		Sink<Integer> output = new Sink<>();
		Designer d = new Designer();
		var source = d.hostSource("source", input);
		var writer = d.<Integer>thread("writer");
		var buffer = d.<Integer>buffer("buffer", 2);
		var reader = d.<Integer>thread("reader");
		var sink = d.hostSink("sink", output);
		d.wire(source.out(), writer.in());
		d.wire(writer.out(), buffer.in());
		d.wire(buffer.out(), reader.in());
		d.wire(reader.out(), sink.in());
		pipeline = d.design().build(d.catalogue());
		pipeline.start();
		for (int i = 0; i < 5; i++) input.put(i);

		assertTrue(output.receivedWithin(5, 2000));
		assertEquals(List.of(0, 1, 2, 3, 4), output.items());
		assertNotSame(output.threads().get(0), Thread.currentThread());
	}

	@Test
	void anIfBlockRoutesByItsPredicate() throws InterruptedException
	{
		BufferBlock<Integer> input = BasicBlocks.getBufferBlock(8);
		Sink<Integer> evens = new Sink<>();
		Sink<Integer> odds = new Sink<>();
		Designer d = new Designer();
		var source = d.hostSource("source", input);
		var driver = d.<Integer>thread("driver");
		var even = d.ifBlock("even", (Integer i) -> i % 2 == 0);
		var a = d.hostSink("evens", evens);
		var b = d.hostSink("odds", odds);
		d.wire(source.out(), driver.in());
		d.wire(driver.out(), even.in());
		d.wire(even.whenTrue(), a.in());
		d.wire(even.whenFalse(), b.in());
		pipeline = d.design().build(d.catalogue());
		pipeline.start();
		for (int i = 1; i <= 4; i++) input.put(i);

		assertTrue(Async.becomesTrueWithin(() -> evens.items().size() + odds.items().size() == 4, 2000));
		assertEquals(List.of(2, 4), evens.items());
		assertEquals(List.of(1, 3), odds.items());
	}

	@Test
	void aDistributorBroadcastsToEveryOutput() throws InterruptedException
	{
		BufferBlock<String> input = BasicBlocks.getBufferBlock(4);
		Sink<String> one = new Sink<>();
		Sink<String> two = new Sink<>();
		Designer d = new Designer();
		var source = d.hostSource("source", input);
		var driver = d.<String>thread("driver");
		var all = d.<String>distributor("all");
		var s1 = d.hostSink("one", one);
		var s2 = d.hostSink("two", two);
		d.wire(source.out(), driver.in());
		d.wire(driver.out(), all.in());
		d.wire(all.out(), s1.in());
		d.wire(all.out(), s2.in());
		pipeline = d.design().build(d.catalogue());
		pipeline.start();
		input.put("x");

		assertTrue(one.receivedWithin(1, 2000));
		assertTrue(two.receivedWithin(1, 2000));
		assertEquals(List.of("x"), one.items());
		assertEquals(List.of("x"), two.items());
	}

	@Test
	void aPullChainPullsFromAConstant() throws InterruptedException
	{
		Sink<Integer> output = new Sink<>();
		Designer d = new Designer();
		var seven = d.constant("seven", 7);
		var times6 = d.pull("times6", (Integer i) -> i * 6);
		var driver = d.<Integer>thread("driver");
		var sink = d.hostSink("sink", output);
		d.wire(seven.out(), times6.in());
		d.wire(times6.out(), driver.in());
		d.wire(driver.out(), sink.in());
		pipeline = d.design().build(d.catalogue());
		pipeline.start();

		assertTrue(output.receivedWithin(3, 2000));
		pipeline.stop();
		assertTrue(output.items().stream().allMatch(i -> i == 42));
	}

	@Test
	void aVariableHoldsTheLatestValuePushed() throws InterruptedException
	{
		BufferBlock<String> input = BasicBlocks.getBufferBlock(4);
		Designer d = new Designer();
		var source = d.hostSource("source", input);
		var driver = d.<String>thread("driver");
		var latest = d.variable("latest", "none");
		d.wire(source.out(), driver.in());
		d.wire(driver.out(), latest.in());
		pipeline = d.design().build(d.catalogue());
		VariableBlock<String> variable = pipeline.block(latest);

		assertEquals("none", variable.get());
		pipeline.start();
		input.put("first");
		input.put("second");

		assertTrue(Async.becomesTrueWithin(() -> input.getLoad() == 0, 2000));
		assertTrue(Async.becomesTrueWithin(() -> get(variable).equals("second"), 2000));
	}

	private static String get(VariableBlock<String> v)
	{
		try
		{
			return v.get();
		} catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
	}

	/** FirstTest's shape, with a buffer for the keyboard and a recording sink for the sender. */
	@Test
	void aPingPongBlockAnswersPingsAndPingsAQuietPeer() throws InterruptedException
	{
		BufferBlock<String> received = BasicBlocks.getBufferBlock(4);
		Sink<String> sent = new Sink<>();
		Designer d = new Designer();
		var network = d.hostSource("network", received);
		var driver = d.<String>thread("driver");
		var ping = d.pingPong("ping", new SimplePingPongProtokol());
		var timeout = d.constant("timeout", 100L);
		var timer = d.<Long>thread("timer");
		var tick = d.push("tick", BasicFunctions.<Long>DelayFunction(20));
		var sender = d.hostSink("sender", sent); // declared after the ping-pong block that needs it
		d.wire(network.out(), driver.in());
		d.wire(driver.out(), ping.in());
		d.wire(ping.send(), sender.in());
		d.wire(timeout.out(), timer.in());
		d.wire(timer.out(), tick.in());
		d.wire(tick.out(), ping.timer());
		pipeline = d.design().build(d.catalogue());
		pipeline.start();

		received.put("peer:PING");
		assertTrue(Async.becomesTrueWithin(() -> sent.items().contains("peer:PONG"), 2000));
		assertTrue(Async.becomesTrueWithin(() -> sent.items().contains("peer:PING"), 2000),
				"a peer quiet for longer than the timeout is pinged");
	}

	@Test
	void aPingPongBlockSendsThroughABlockDeclaredAfterIt() throws InterruptedException
	{
		BufferBlock<String> received = BasicBlocks.getBufferBlock(4);
		Sink<String> sent = new Sink<>();
		Designer d = new Designer();
		var network = d.hostSource("network", received);
		var driver = d.<String>thread("driver");
		var ping = d.pingPong("ping", new SimplePingPongProtokol());
		var lower = d.push("lower", (String s) -> s.toLowerCase());
		var sender = d.hostSink("sender", sent);
		d.wire(network.out(), driver.in());
		d.wire(driver.out(), ping.in());
		d.wire(ping.send(), lower.in());
		d.wire(lower.out(), sender.in());
		pipeline = d.design().build(d.catalogue());
		pipeline.start();

		received.put("peer:PING");
		assertTrue(sent.receivedWithin(1, 2000));
		assertEquals("peer:pong", sent.items().get(0));
	}

	/** The first ping-pong block sends into a second, declared after it: the second must be built first. */
	@Test
	void aPingPongBlockSendsIntoAnotherPingPongBlock() throws InterruptedException
	{
		BufferBlock<String> received = BasicBlocks.getBufferBlock(4);
		Sink<String> sent = new Sink<>();
		Designer d = new Designer();
		var network = d.hostSource("network", received);
		var driver = d.<String>thread("driver");
		var first = d.pingPong("first", new SimplePingPongProtokol());
		var second = d.pingPong("second", new SimplePingPongProtokol());
		var timeout = d.constant("timeout", 100L);
		var timer = d.<Long>thread("timer");
		var tick = d.push("tick", BasicFunctions.<Long>DelayFunction(20));
		var sender = d.hostSink("sender", sent);
		d.wire(network.out(), driver.in());
		d.wire(driver.out(), first.in());
		d.wire(first.send(), second.in());
		d.wire(second.send(), sender.in());
		d.wire(timeout.out(), timer.in());
		d.wire(timer.out(), tick.in());
		d.wire(tick.out(), second.timer());
		pipeline = d.design().build(d.catalogue());
		pipeline.start();

		received.put("peer:PING"); // first answers "peer:PONG" into second, which then keeps "peer" alive
		assertTrue(Async.becomesTrueWithin(() -> sent.items().contains("peer:PING"), 2000), sent.items().toString());
	}

	@Test
	void aReporterPortReceivesTheBlocksMessages() throws InterruptedException
	{
		BufferBlock<String> input = BasicBlocks.getBufferBlock(4);
		Sink<String> output = new Sink<>();
		Sink<String> debug = new Sink<>();
		Designer d = new Designer();
		var source = d.hostSource("source", input);
		var driver = d.<String>thread("driver");
		var same = d.push("same", (String s) -> s);
		var sink = d.hostSink("sink", output);
		var log = d.hostSink("log", debug);
		d.wire(source.out(), driver.in());
		d.wire(driver.out(), same.in());
		d.wire(same.out(), sink.in());
		d.wire(same.debug(), log.in());
		pipeline = d.design().build(d.catalogue());
		pipeline.start();
		input.put("hello");

		assertTrue(output.receivedWithin(1, 2000));
		assertTrue(debug.items().stream().anyMatch(m -> m.contains("hello")), debug.items().toString());
	}

	@Test
	void blockGivesBackTheBuiltObjectTyped()
	{
		Designer d = new Designer();
		var buffer = d.<String>buffer("buffer", 3);
		pipeline = d.design().build(d.catalogue());

		BufferBlock<String> built = pipeline.block(buffer);
		assertEquals(3, built.getCapacity());
		assertSame(built, pipeline.block("buffer"));
		assertThrows(IllegalArgumentException.class, () -> pipeline.block("nope"));
	}

	@Test
	void aDesignWrittenAsPlainDataBuildsWithACatalogue() throws InterruptedException
	{
		BufferBlock<String> input = BasicBlocks.getBufferBlock(4);
		Sink<String> output = new Sink<>();
		Design design = new Design(
				List.of(new Design.Node("in", "hostSource", Map.of("object", "keyboardStandIn")),
						new Design.Node("t", "thread", Map.of()),
						new Design.Node("up", "push", Map.of("function", "upper")),
						new Design.Node("out", "hostSink", Map.of("object", "screen"))),
				List.of(new Design.Wire(new Design.Port("in", "out"), new Design.Port("t", "in")),
						new Design.Wire(new Design.Port("t", "out"), new Design.Port("up", "in")),
						new Design.Wire(new Design.Port("up", "out"), new Design.Port("out", "in"))));
		Catalogue catalogue = new Catalogue()
				.put("keyboardStandIn", input)
				.put("upper", (java.util.function.Function<String, String>) String::toUpperCase)
				.put("screen", output);
		pipeline = design.build(catalogue);
		pipeline.start();
		input.put("abc");

		assertTrue(output.receivedWithin(1, 2000));
		assertEquals(List.of("ABC"), output.items());
	}

	@Test
	void stopStopsEveryThread() throws InterruptedException
	{
		BufferBlock<String> a = BasicBlocks.getBufferBlock(1);
		BufferBlock<String> b = BasicBlocks.getBufferBlock(1);
		Designer d = new Designer();
		var sa = d.hostSource("a", a);
		var sb = d.hostSource("b", b);
		var ta = d.<String>thread("ta");
		var tb = d.<String>thread("tb");
		var bin = d.<String>trashcan("bin");
		d.wire(sa.out(), ta.in());
		d.wire(sb.out(), tb.in());
		d.wire(ta.out(), bin.in());
		d.wire(tb.out(), bin.in());
		pipeline = d.design().build(d.catalogue());
		pipeline.start();
		assertTrue(pipeline.block(ta).isRunning());
		assertTrue(pipeline.block(tb).isRunning());

		pipeline.stop();

		assertFalse(pipeline.block(ta).isRunning());
		assertFalse(pipeline.block(tb).isRunning());
		assertFalse(pipeline.isRunning());
	}
}
