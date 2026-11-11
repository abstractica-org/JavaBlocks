package org.abstractica.javablocks.examples.udp;

import org.abstractica.javablocks.blocks.basic.BasicBlocks;
import org.abstractica.javablocks.blocks.basic.BufferBlock;
import org.abstractica.javablocks.blocks.basic.ThreadControl;
import org.abstractica.javablocks.design.Designer;
import org.abstractica.javablocks.design.Pipeline;
import org.abstractica.javablocks.testsupport.Async;
import org.abstractica.javablocks.testsupport.Sink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The examples/udp pipelines, built through the Design (J18), with a host source for the keyboard
 * and a sink for the console; then the two example classes themselves.
 */
@Timeout(10)
class UdpExamplesTest
{
	private static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress();

	private ThreadControl running;

	@AfterEach
	void tearDown() throws InterruptedException
	{
		if (running != null) running.stop();
	}

	private static int freePort() throws Exception
	{
		try (DatagramSocket probe = new DatagramSocket(0))
		{
			return probe.getLocalPort();
		}
	}

	private static boolean isFree(int port) throws Exception
	{
		try (DatagramSocket probe = new DatagramSocket(port))
		{
			return true;
		} catch (BindException e)
		{
			return false;
		}
	}

	private static void sendTo(int port, String text) throws Exception
	{
		byte[] bytes = text.getBytes();
		try (DatagramSocket sender = new DatagramSocket())
		{
			sender.send(new DatagramPacket(bytes, bytes.length, LOOPBACK, port));
		}
	}

	private static String receive(DatagramSocket socket) throws Exception
	{
		DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
		socket.receive(packet);
		return new String(packet.getData(), 0, packet.getLength());
	}

	private Pipeline start(Designer d)
	{
		Pipeline pipeline = d.design().build(d.catalogue());
		running = pipeline;
		pipeline.start();
		return pipeline;
	}

	@Test
	void keyboardToUdpSendsEachLineToTheReceiverFromItsOwnPort() throws Exception
	{
		int ownPort = freePort();
		BufferBlock<String> lines = BasicBlocks.getBufferBlock(4);
		try (DatagramSocket receiver = new DatagramSocket(0))
		{
			receiver.setSoTimeout(2000);
			Designer d = new Designer();
			KeyboardToUDP.design(d, d.hostSource("lines", lines).out(), ownPort, LOOPBACK, receiver.getLocalPort());
			start(d);

			lines.put("hello");
			lines.put("again");

			DatagramPacket first = new DatagramPacket(new byte[1024], 1024);
			receiver.receive(first);
			assertEquals("hello", new String(first.getData(), 0, first.getLength()));
			assertEquals(ownPort, first.getPort(), "sent from the socket on the example's own port");
			assertEquals("again", receive(receiver));
		}
	}

	@Test
	void udpToConsoleShowsEachPacketsTextCutToThePacketSize() throws Exception
	{
		int port = freePort();
		Sink<String> console = new Sink<>();
		Designer d = new Designer();
		UDPToConsole.design(d, port, 4, d.hostSink("console", console).in());
		start(d);

		sendTo(port, "hi");
		assertTrue(console.receivedWithin(1, 2000));
		sendTo(port, "hello");
		assertTrue(console.receivedWithin(2, 2000));

		assertEquals(List.of("hi", "hell"), console.items(), "a packet longer than the 4 bytes given is cut");
		assertNotSame(Thread.currentThread(), console.threads().get(0));
	}

	@Test
	void simpleUdpExampleSendsEachLineToItsOwnSocketAndShowsIt() throws Exception
	{
		int port = freePort();
		BufferBlock<String> lines = BasicBlocks.getBufferBlock(4);
		Sink<String> console = new Sink<>();
		Designer d = new Designer();
		var socket = SimpleUDPExample.design(d, d.hostSource("lines", lines).out(), d.hostSink("console", console).in(),
				LOOPBACK, port);
		List<String> ids = d.design().nodes().stream().map(n -> n.id()).toList();
		assertTrue(ids.indexOf("readThread") < ids.indexOf("writeThread"), "the reader is started first, as the example always did");
		Pipeline pipeline = start(d);
		assertEquals(InetAddress.getLocalHost(), pipeline.block(socket).getAddress(), "what main prints");
		assertFalse(isFree(port), "the socket is on the port given");

		lines.put("hello");
		lines.put("x".repeat(1024) + "cut");

		assertTrue(console.receivedWithin(2, 2000));
		assertEquals(List.of("hello", "x".repeat(1024)), console.items(), "the example reads packets of up to 1024 bytes");
		assertNotSame(console.threads().get(0), Thread.currentThread());
	}

	@Test
	void simpleDatagramPacketExampleTurnsALineIntoAPacketAndBack() throws Exception
	{
		BufferBlock<String> lines = BasicBlocks.getBufferBlock(4);
		Sink<String> console = new Sink<>();
		Designer d = new Designer();
		SimpleDatagramPacketExample.design(d, d.hostSource("lines", lines).out(), d.hostSink("console", console).in());
		start(d);

		lines.put("hello");
		lines.put("again");

		assertTrue(console.receivedWithin(2, 2000));
		assertEquals(List.of("hello", "again"), console.items());
	}

	// --- the example classes themselves ---

	@Test
	void udpToConsolePrintsAPacketStopsWithNoneArrivingFreesItsPortAndCannotStartAgain() throws Exception
	{
		int port = freePort();
		UDPToConsole receiver = new UDPToConsole(port, 1024);
		running = receiver;
		assertFalse(receiver.isRunning());
		ByteArrayOutputStream printed = new ByteArrayOutputStream();
		PrintStream realOut = System.out;
		System.setOut(new PrintStream(printed, true));
		try
		{
			receiver.start();
			assertTrue(receiver.isRunning());
			sendTo(port, "to the real console");
			assertTrue(Async.becomesTrueWithin(() -> printed.toString().contains("to the real console"), 2000));
		} finally
		{
			System.setOut(realOut);
		}
		Thread.sleep(250); // the thread waits in the socket again

		Thread stopper = Async.daemon(receiver::stop);

		assertTrue(Async.finishesWithin(stopper, 2000), "before J16 this stop waited until a packet came");
		assertFalse(receiver.isRunning());
		assertTrue(isFree(port), "stop() closed the socket");
		assertThrows(IllegalStateException.class, receiver::start, "the socket is closed (J17)");
	}

	@Test
	void aTakenPortIsStillASocketExceptionFromBothConstructors() throws Exception
	{
		try (DatagramSocket taken = new DatagramSocket(0))
		{
			assertThrows(SocketException.class, () -> new UDPToConsole(taken.getLocalPort(), 1024));
			assertThrows(SocketException.class, () -> new KeyboardToUDP(taken.getLocalPort(), LOOPBACK, 3337));
		}
	}

	@Test
	void aKeyboardToUdpThatNeverStartedFreesItsPortOnStop() throws Exception
	{
		int port = freePort();
		KeyboardToUDP sender = new KeyboardToUDP(port, LOOPBACK, 3337);
		assertFalse(sender.isRunning());
		assertFalse(isFree(port), "the constructor opens the socket, as it always did");

		sender.stop();

		assertTrue(isFree(port));
	}
}
