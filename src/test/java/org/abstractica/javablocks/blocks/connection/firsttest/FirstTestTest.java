package org.abstractica.javablocks.blocks.connection.firsttest;

import org.abstractica.javablocks.blocks.basic.BasicBlocks;
import org.abstractica.javablocks.blocks.basic.BufferBlock;
import org.abstractica.javablocks.design.Designer;
import org.abstractica.javablocks.design.Pipeline;
import org.abstractica.javablocks.testsupport.Async;
import org.abstractica.javablocks.testsupport.Sink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** FirstTest's pipeline, built through the Design (J15), with a host source for the keyboard. */
@Timeout(10)
class FirstTestTest
{
	private Pipeline pipeline;

	@AfterEach
	void tearDown() throws InterruptedException
	{
		if (pipeline != null) pipeline.stop();
	}

	private void build(BufferBlock<String> commands, Sink<String> sent, long timeout, long tick)
	{
		Designer d = new Designer();
		FirstTest.design(d, d.hostSource("commands", commands).out(), sent, timeout, tick);
		pipeline = d.design().build(d.catalogue());
	}

	@Test
	void aPingFromAPeerIsAnsweredWithAPongThroughTheSender() throws InterruptedException
	{
		BufferBlock<String> commands = BasicBlocks.getBufferBlock(4);
		Sink<String> sent = new Sink<>();
		build(commands, sent, 5000L, 20L); // FirstTest's timeout; a short tick so tearDown's stop is quick
		pipeline.start();

		commands.put("peer:PING");
		assertTrue(sent.receivedWithin(1, 2000));
		assertEquals("peer:PONG", sent.items().get(0));
		Thread.sleep(300); // many ticks, all well inside the 5 s timeout
		assertEquals(List.of("peer:PONG"), sent.items(), "a peer heard from within the timeout is not pinged");
	}

	@Test
	void theTimerPingsAPeerQuietForLongerThanTheTimeout() throws InterruptedException
	{
		BufferBlock<String> commands = BasicBlocks.getBufferBlock(4);
		Sink<String> sent = new Sink<>();
		build(commands, sent, 400L, 20L);
		pipeline.start();

		commands.put("peer:PING");
		assertTrue(sent.receivedWithin(1, 2000));
		Thread.sleep(200); // ten ticks, half the timeout
		assertFalse(sent.items().contains("peer:PING"), "a peer quiet for less than the timeout is not pinged yet");
		assertTrue(Async.becomesTrueWithin(() -> sent.items().contains("peer:PING"), 2000),
				"a peer quiet for longer than the timeout is pinged");
	}

	@Test
	void thePipelineStopsAfterTheTickInFlight() throws InterruptedException
	{
		BufferBlock<String> commands = BasicBlocks.getBufferBlock(4);
		build(commands, new Sink<>(), 5000L, 200L);
		pipeline.start();
		assertTrue(pipeline.isRunning());

		// The timer thread is usually inside the delayed put; stop() waits for it (J4), so stopping
		// FirstTest's own pipeline can take up to its 10 s tick.
		long started = System.nanoTime();
		pipeline.stop();
		assertFalse(pipeline.isRunning());
		assertTrue(System.nanoTime() - started < 2_000_000_000L, "stop waits for the 200 ms tick in flight, not longer (2 s allowed for slow machines)");
	}
}
