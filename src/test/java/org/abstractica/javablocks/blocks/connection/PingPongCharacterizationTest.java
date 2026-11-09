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
	 * J11: a silent peer is pinged while quiet for longer than the timeout, then dropped once quiet for
	 * more than three timeouts: its handler is destroyed through the protocol and the pings stop. A
	 * peer that keeps talking is never pinged or dropped; the dropped peer's next message creates a
	 * fresh handler. (v0.2.0 pinged a silent peer on every tick forever and never destroyed a handler.)
	 */
	@Test
	void aSilentPeerIsPingedThenDroppedAfterThreeTimeouts() throws InterruptedException
	{
		Protocol protocol = new Protocol();
		Sink<String> sent = new Sink<>();
		PingPongBlock<String> block = pingPong(protocol, sent);
		Output<Long> timer = block.getTimerConnector();

		block.put("silent:hello");
		for (int tick = 0; tick < 8; tick++) // 8 ticks of 70+ ms: well past 3 x 50 ms of silence
		{
			block.put("talker:still here");
			Thread.sleep(TIMEOUT + 20);
			block.put("talker:still here");
			timer.put(TIMEOUT);
		}

		long pings = pingsTo(sent, "silent");
		assertTrue(pings >= 1 && pings <= 2, "pinged while quiet for 1 to 3 timeouts, then not; got " + pings);
		assertEquals(1, protocol.destroyed.size(), "the silent peer's handler is destroyed exactly once");
		assertEquals(0, pingsTo(sent, "talker"));
		assertEquals(List.of("silent", "talker"), protocol.created, "the talker is never dropped");

		block.put("silent:back again");
		assertEquals(List.of("silent", "talker", "silent"), protocol.created, "a returning peer gets a fresh handler");
		assertEquals(List.of("silent:hello"), protocol.destroyed.getFirst().items(), "the destroyed handler was the silent peer's");
	}

	/** J11: the factor is fixed at 3, as ruled. The timing test above cannot tell 3 from 2 or 4 reliably. */
	@Test
	void theDropFactorIsThree() throws ReflectiveOperationException
	{
		java.lang.reflect.Field factor = PingPongBlockImpl.class.getDeclaredField("SILENT_TIMEOUTS_BEFORE_DROP");
		factor.setAccessible(true);
		assertEquals(3L, factor.getLong(null));
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
