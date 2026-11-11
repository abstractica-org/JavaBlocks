package org.abstractica.javablocks.blocks.udp;

import org.abstractica.javablocks.testsupport.Async;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The UDP socket block's {@code get()} answers an interrupt (J16): it waits for a packet 100 ms at a
 * time and checks its thread's interrupt flag between waits. Before J16 an interrupt never woke it.
 */
@Timeout(10)
class UDPSocketBlockCharacterizationTest
{
	private UDPSocketBlock<DatagramPacket> socket;
	private int port;

	/** A socket block on a port that was free a moment ago. */
	private void open() throws Exception
	{
		try (DatagramSocket probe = new DatagramSocket(0))
		{
			port = probe.getLocalPort();
		}
		socket = UDPBlocks.getUDPSocketBlock(port, 1024);
	}

	@AfterEach
	void close()
	{
		if (socket != null) socket.close();
	}

	private void send(String text) throws Exception
	{
		byte[] bytes = text.getBytes();
		socket.put(new DatagramPacket(bytes, bytes.length, InetAddress.getLoopbackAddress(), port));
	}

	private static String text(DatagramPacket packet)
	{
		return UDPFunctions.getDatagramPacketToStringFunction().apply(packet);
	}

	/** Runs one {@code get()} on a daemon thread; {@code outcome} gets the packet's text or the exception. */
	private Thread getter(AtomicReference<Object> outcome)
	{
		return Async.daemon(() ->
		{
			try
			{
				outcome.set(text(socket.get()));
			} catch (InterruptedException | RuntimeException e)
			{
				outcome.set(e);
			}
		});
	}

	@Test
	void anInterruptEndsAWaitingGetAndLeavesTheSocketOpen() throws Exception
	{
		open();
		AtomicReference<Object> outcome = new AtomicReference<>();
		Thread getter = getter(outcome);
		assertTrue(Async.stillRunningAfter(getter, 350), "no packet: get() keeps waiting through several checks");

		getter.interrupt();

		assertTrue(Async.finishesWithin(getter, 1000), "the next check, at most about 100 ms away, sees the interrupt");
		assertInstanceOf(InterruptedException.class, outcome.get());

		AtomicReference<Object> second = new AtomicReference<>();
		Thread again = getter(second);
		send("still open");
		assertTrue(Async.finishesWithin(again, 2000));
		assertEquals("still open", second.get(), "the interrupt did not close the socket");
	}

	@Test
	void aGetByAnAlreadyInterruptedThreadThrowsAtOnceAndClearsTheFlag() throws Exception
	{
		open();
		send("waiting in the socket"); // even with a packet ready, the interrupt wins
		Thread.sleep(100);
		AtomicReference<Object> outcome = new AtomicReference<>();
		AtomicReference<Boolean> flagAfter = new AtomicReference<>();
		Thread getter = Async.daemon(() ->
		{
			Thread.currentThread().interrupt();
			try
			{
				outcome.set(text(socket.get()));
			} catch (InterruptedException e)
			{
				outcome.set(e);
			}
			flagAfter.set(Thread.currentThread().isInterrupted());
		});

		assertTrue(Async.finishesWithin(getter, 2000));
		assertInstanceOf(InterruptedException.class, outcome.get());
		assertEquals(false, flagAfter.get(), "throwing InterruptedException clears the flag, as every blocking get() does");
	}

	@Test
	void aPacketArrivingAfterSeveralChecksIsStillReceivedWhole() throws Exception
	{
		open();
		AtomicReference<Object> outcome = new AtomicReference<>();
		Thread getter = getter(outcome);
		assertTrue(Async.stillRunningAfter(getter, 350), "three 100 ms waits have timed out; get() is still waiting");

		send("late packet");

		assertTrue(Async.finishesWithin(getter, 2000));
		assertEquals("late packet", outcome.get());
	}
}
