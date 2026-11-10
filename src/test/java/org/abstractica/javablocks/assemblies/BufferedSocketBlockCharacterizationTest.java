package org.abstractica.javablocks.assemblies;

import org.abstractica.javablocks.blocks.udp.UDPFunctions;
import org.abstractica.javablocks.testsupport.Async;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins a wart found while building the Design core (J13): interrupting a thread does not wake a
 * {@code DatagramSocket.receive}, only closing the socket does. {@code stop()} stops its threads
 * before it closes the socket, so while no packet arrives it never returns; a {@code close()} from
 * another thread releases it.
 */
@Timeout(10)
class BufferedSocketBlockCharacterizationTest
{
	@Test
	void stopNeverReturnsWhileNoPacketArrivesUntilTheSocketIsClosed() throws Exception
	{
		BufferedSocketBlock<String> block = Assemblies.getBufferedSocketBlock(0, 1024, 4,
				UDPFunctions.getDatagramPacketToStringFunction(),
				UDPFunctions.getStringToDatagramPacketFunction());
		block.start();

		Thread stopper = Async.daemon(block::stop);

		assertTrue(Async.stillRunningAfter(stopper, 500), "stop() waits on the receive thread, blocked in receive");
		block.close();
		assertTrue(Async.finishesWithin(stopper, 2000), "closing the socket releases the receive, and stop() returns");
		assertFalse(block.isRunning());
	}
}
