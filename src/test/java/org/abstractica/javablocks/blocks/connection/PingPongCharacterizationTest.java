package org.abstractica.javablocks.blocks.connection;

import org.abstractica.javablocks.blocks.connection.impl.PingPongHandlerImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/** Characterizes the ping-pong block: the connection keep-alive built on a MapBlock keyed by peer. */
class PingPongCharacterizationTest
{
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
