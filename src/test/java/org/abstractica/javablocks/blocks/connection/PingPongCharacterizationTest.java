package org.abstractica.javablocks.blocks.connection;

import org.abstractica.javablocks.blocks.basic.BasicBlocks;
import org.abstractica.javablocks.blocks.basic.Output;
import org.abstractica.javablocks.blocks.connection.impl.PingPongBlockImpl;
import org.abstractica.javablocks.blocks.connection.impl.PingPongHandlerImpl;
import org.abstractica.javablocks.testsupport.Sink;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/** Characterizes the ping-pong block: the connection keep-alive built on a MapBlock keyed by peer. */
class PingPongCharacterizationTest
{
	/** Messages are "key:payload"; records every handler created and destroyed. */
	static final class Protocol implements PingPongProtocol<String, String, Sink<String>>
	{
		final List<String> created = new CopyOnWriteArrayList<>();
		final List<Sink<String>> destroyed = new CopyOnWriteArrayList<>();

		@Override public boolean isPing(String msg) { return msg.endsWith(":PING"); }
		@Override public boolean isPong(String msg) { return msg.endsWith(":PONG"); }
		@Override public String getPong(String key) { return key + ":PONG"; }
		@Override public String getPing(String key) { return key + ":PING"; }
		@Override public String extractKey(String msg) { return msg.substring(0, msg.indexOf(':')); }

		@Override
		public Sink<String> createHandlerFor(String key)
		{
			created.add(key);
			return new Sink<>();
		}

		@Override
		public void destroyHandler(Sink<String> handler)
		{
			destroyed.add(handler);
		}
	}

	static final long TIMEOUT = 50;

	static PingPongBlock<String> pingPong(Protocol protocol, Output<String> sender)
	{
		PingPongBlock<String> block = new PingPongBlockImpl<>(protocol, sender);
		block.setDebugReporter(BasicBlocks.getTrashcanBlock());
		return block;
	}

	static long pingsTo(Sink<String> sent, String key)
	{
		return sent.items().stream().filter((key + ":PING")::equals).count();
	}

	/**
	 * Wart: a peer that has gone silent keeps its handler forever and is pinged on every tick that
	 * finds it quiet for longer than the timeout; nothing ever destroys its handler. A peer that
	 * keeps talking is never pinged.
	 */
	@Test
	void aSilentPeerKeepsItsHandlerAndIsPingedForever() throws InterruptedException
	{
		Protocol protocol = new Protocol();
		Sink<String> sent = new Sink<>();
		PingPongBlock<String> block = pingPong(protocol, sent);
		Output<Long> timer = block.getTimerConnector();

		block.put("silent:hello");
		for (int tick = 0; tick < 8; tick++)
		{
			block.put("talker:still here");
			Thread.sleep(TIMEOUT + 20);
			block.put("talker:still here");
			timer.put(TIMEOUT);
		}

		assertEquals(8, pingsTo(sent, "silent"), "pinged on every tick, long after 3 timeouts of silence");
		assertEquals(0, pingsTo(sent, "talker"));
		assertEquals(List.of(), protocol.destroyed, "no handler is ever destroyed");
		assertEquals(List.of("silent", "talker"), protocol.created);
	}

	/**
	 * J7: the timestamps cross threads (receiver writes, timer reads), so both are volatile. A stale
	 * read cannot be provoked reliably, so the declaration is what is pinned.
	 */
	@Test
	void handlerTimestampsAreVolatile() throws NoSuchFieldException
	{
		for (String field : new String[]{"lastMessageReceivedTime", "lastPingSentTime"})
		{
			int modifiers = PingPongHandlerImpl.class.getDeclaredField(field).getModifiers();
			assertTrue(Modifier.isVolatile(modifiers), field + " is volatile");
		}
	}
}
