package org.abstractica.javablocks.assemblies;

import org.abstractica.javablocks.blocks.udp.UDPFunctions;
import org.abstractica.javablocks.testsupport.Async;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the fix of a wart found while building the Design core (J13), fixed in J16: interrupting a
 * thread does not wake a {@code DatagramSocket.receive}, so {@code stop()}, which stops its threads
 * before it closes the socket, never returned while no packet arrived. The socket block now checks
 * for the interrupt every 100 ms, and {@code stop()} returns on its own.
 */
@Timeout(10)
class BufferedSocketBlockCharacterizationTest
{
	@Test
	void stopReturnsWhileNoPacketArrivesWithoutAnyoneClosingTheSocket() throws Exception
	{
		BufferedSocketBlock<String> block = Assemblies.getBufferedSocketBlock(0, 1024, 4,
				UDPFunctions.getDatagramPacketToStringFunction(),
				UDPFunctions.getStringToDatagramPacketFunction());
		block.start();
		assertTrue(block.isRunning());
		Thread.sleep(300); // the receive thread is waiting in the socket, several checks in

		Thread stopper = Async.daemon(block::stop);

		assertTrue(Async.finishesWithin(stopper, 2000),
				"stop() interrupts the receive thread, which notices within about 100 ms; no packet, no outside close");
		assertFalse(block.isRunning());
	}

	/** The assembly passes the socket block's {@code getPort()} on, wart included: always -1. */
	@Test
	void getPortIsMinusOneAsTheSocketBlocksIs() throws Exception
	{
		BufferedSocketBlock<String> block = Assemblies.getBufferedSocketBlock(0, 1024, 4,
				UDPFunctions.getDatagramPacketToStringFunction(),
				UDPFunctions.getStringToDatagramPacketFunction());
		try
		{
			assertEquals(-1, block.getPort());
		} finally
		{
			block.close();
		}
	}
}
