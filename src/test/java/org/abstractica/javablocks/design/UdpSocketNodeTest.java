package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.BasicBlocks;
import org.abstractica.javablocks.blocks.basic.BufferBlock;
import org.abstractica.javablocks.blocks.basic.MapBlock;
import org.abstractica.javablocks.blocks.udp.UDPFunctions;
import org.abstractica.javablocks.blocks.udp.UDPSocketBlock;
import org.abstractica.javablocks.testsupport.Async;
import org.abstractica.javablocks.testsupport.BlockingSink;
import org.abstractica.javablocks.testsupport.Sink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.UncheckedIOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The udpSocket node (J17): opened at build, read by a stoppable thread (J16), closed by stop(). */
@Timeout(10)
class UdpSocketNodeTest
{
	private Pipeline pipeline;

	@AfterEach
	void stop() throws InterruptedException
	{
		if (pipeline != null) pipeline.stop();
	}

	/** A port that was free a moment ago. */
	private static int freePort() throws Exception
	{
		try (DatagramSocket probe = new DatagramSocket(0))
		{
			return probe.getLocalPort();
		}
	}

	/** True when no socket holds {@code port}: a socket block on it has been closed, or never opened. */
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
			sender.send(new DatagramPacket(bytes, bytes.length, InetAddress.getLoopbackAddress(), port));
		}
	}

	/** socket.out, pulled by a thread, pushed into {@code sink}. */
	private static UdpSocketNode reader(Designer d, int port, org.abstractica.javablocks.blocks.basic.Output<DatagramPacket> sink)
	{
		var socket = d.udpSocket("socket", port, 1024);
		var reader = d.<DatagramPacket>thread("reader");
		var out = d.hostSink("sink", sink);
		d.wire(socket.out(), reader.in());
		d.wire(reader.out(), out.in());
		return socket;
	}

	@Test
	void theDesignerWritesTheSocketAsData()
	{
		Designer d = new Designer();
		var socket = d.udpSocket("socket", 3337, 1024);
		var writer = d.<DatagramPacket>thread("writer");
		d.wire(writer.out(), socket.in());
		d.wire(socket.out(), writer.in());

		Design design = d.design();
		assertEquals(new Design.Node("socket", "udpSocket", Map.of("port", 3337, "maxPacketSize", 1024)),
				design.nodes().get(0));
		assertEquals(List.of(
				new Design.Wire(new Design.Port("writer", "out"), new Design.Port("socket", "in")),
				new Design.Wire(new Design.Port("socket", "out"), new Design.Port("writer", "in"))), design.wires());
		assertTrue(d.catalogue().names().isEmpty(), "a socket needs no Java object");
	}

	@Test
	void aPipelineSendsThroughItsSocketAndReceivesFromIt() throws Exception
	{
		int port = freePort();
		BufferBlock<String> lines = BasicBlocks.getBufferBlock(4);
		Sink<String> heard = new Sink<>();
		Designer d = new Designer();
		var source = d.hostSource("lines", lines);
		var writer = d.<String>thread("writer");
		var toPacket = d.push("toPacket", UDPFunctions.getStringToDatagramPacketFunction());
		var address = d.push("address", UDPFunctions.getAddressFunction(InetAddress.getLoopbackAddress(), port));
		var socket = d.udpSocket("socket", port, 1024);
		var reader = d.<DatagramPacket>thread("reader");
		var toString = d.push("toString", UDPFunctions.getDatagramPacketToStringFunction());
		var sink = d.hostSink("heard", heard);
		d.wire(source.out(), writer.in());
		d.wire(writer.out(), toPacket.in());
		d.wire(toPacket.out(), address.in());
		d.wire(address.out(), socket.in());
		d.wire(socket.out(), reader.in());
		d.wire(reader.out(), toString.in());
		d.wire(toString.out(), sink.in());
		pipeline = d.design().build(d.catalogue());
		UDPSocketBlock<DatagramPacket> built = pipeline.block(socket);
		assertNotNull(built.getAddress());
		assertFalse(isFree(port), "building opened the socket");
		pipeline.start();

		lines.put("hello");
		lines.put("again");

		assertTrue(heard.receivedWithin(2, 2000));
		assertEquals(List.of("hello", "again"), heard.items());
		assertNotSame(heard.threads().get(0), Thread.currentThread());
	}

	@Test
	void stopClosesTheSocketOnlyAfterTheReadersPutInFlight() throws Exception
	{
		int port = freePort();
		BlockingSink<DatagramPacket> sink = new BlockingSink<>();
		Designer d = new Designer();
		reader(d, port, sink);
		pipeline = d.design().build(d.catalogue());
		pipeline.start();
		sendTo(port, "in flight");
		assertTrue(sink.enteredWithin(2000));

		Thread stopper = Async.daemon(pipeline::stop);

		assertTrue(Async.stillRunningAfter(stopper, 300), "stop() waits for the reader's put (J4)");
		assertFalse(isFree(port), "the socket stays open while a thread of the pipeline still runs");
		sink.release();
		assertTrue(Async.finishesWithin(stopper, 2000));
		assertFalse(pipeline.isRunning());
		assertEquals(1, sink.delivered.items().size());
		assertTrue(isFree(port), "stop() closed the socket");
	}

	@Test
	void stopStopsAnIdleReaderAndFreesThePort() throws Exception
	{
		int port = freePort();
		Designer d = new Designer();
		reader(d, port, new Sink<>());
		pipeline = d.design().build(d.catalogue());
		pipeline.start();
		Thread.sleep(250); // the reader waits in the socket, a couple of checks in

		Thread stopper = Async.daemon(pipeline::stop);

		assertTrue(Async.finishesWithin(stopper, 2000), "no packet arrives, and the reader still stops (J16)");
		assertFalse(pipeline.isRunning());
		assertTrue(isFree(port));
	}

	@Test
	void stoppingAPipelineThatNeverStartedFreesThePort() throws Exception
	{
		int port = freePort();
		Designer d = new Designer();
		reader(d, port, new Sink<>());
		pipeline = d.design().build(d.catalogue());
		assertFalse(isFree(port));

		pipeline.stop();

		assertTrue(isFree(port), "the socket was opened at build, so stop() closes it, started or not");
	}

	@Test
	void aStoppedPipelineWithASocketRefusesToStartAndStartsNothing() throws Exception
	{
		Designer d = new Designer();
		// A thread with no socket comes first in the node order: a refusal must not have started it.
		var source = d.hostSource("source", BasicBlocks.<String>getBufferBlock(1));
		var other = d.<String>thread("other");
		var bin = d.<String>trashcan("bin");
		d.wire(source.out(), other.in());
		d.wire(other.out(), bin.in());
		reader(d, freePort(), new Sink<>());
		pipeline = d.design().build(d.catalogue());
		pipeline.start();
		pipeline.stop();

		var e = assertThrows(IllegalStateException.class, () -> pipeline.start());

		assertTrue(e.getMessage().contains("closed"), e.getMessage());
		assertFalse(pipeline.isRunning(), "nothing was started");
	}

	@Test
	void aPipelineWithoutSocketsStillStartsAgainAfterStop() throws Exception
	{
		BufferBlock<String> input = BasicBlocks.getBufferBlock(1);
		Sink<String> output = new Sink<>();
		Designer d = new Designer();
		var source = d.hostSource("source", input);
		var driver = d.<String>thread("driver");
		var sink = d.hostSink("sink", output);
		d.wire(source.out(), driver.in());
		d.wire(driver.out(), sink.in());
		pipeline = d.design().build(d.catalogue());
		pipeline.start();
		pipeline.stop();

		pipeline.start();
		input.put("after restart");

		assertTrue(output.receivedWithin(1, 2000));
	}

	@Test
	void aPortInUseIsRefusedNamingTheNodeAndClosesTheSocketsAlreadyOpened() throws Exception
	{
		int freePort = freePort();
		try (DatagramSocket taken = new DatagramSocket(0))
		{
			Designer d = new Designer();
			d.udpSocket("first", freePort, 1024);
			d.udpSocket("second", taken.getLocalPort(), 1024);

			var e = assertThrows(UncheckedIOException.class, () -> d.design().build(d.catalogue()));

			assertTrue(e.getMessage().contains("node 'second'"), e.getMessage());
			assertTrue(e.getMessage().contains(String.valueOf(taken.getLocalPort())), e.getMessage());
			assertTrue(isFree(freePort), "the first socket, opened before the second failed, was closed again");
		}
	}

	@Test
	void aBadPortOrPacketSizeIsListedWithTheOtherProblemsAndOpensNothing() throws Exception
	{
		int port = freePort();
		Designer d = new Designer();
		d.udpSocket("good", port, 1024);
		d.udpSocket("farOut", 70000, 1024);
		d.udpSocket("negative", -1, 1024);
		d.udpSocket("tiny", 0, -1);
		d.<String>thread("unwired");
		Design byHand = new Design(List.of(new Design.Node("bare", "udpSocket", Map.of("port", "3337"))), List.of());

		var e = assertThrows(IllegalArgumentException.class, () -> d.design().build(d.catalogue()));
		var bare = assertThrows(IllegalArgumentException.class, () -> byHand.build(new Catalogue()));

		assertTrue(e.getMessage().contains("node 'farOut': port must be 0 (any free port) to 65535, was 70000"), e.getMessage());
		assertTrue(e.getMessage().contains("node 'negative': port must be 0 (any free port) to 65535, was -1"), e.getMessage());
		assertTrue(e.getMessage().contains("node 'tiny': maxPacketSize must not be negative, was -1"), e.getMessage());
		assertTrue(e.getMessage().contains("port unwired.in must be wired"), e.getMessage());
		assertFalse(e.getMessage().contains("'good'"), e.getMessage());
		assertTrue(isFree(port), "a refused Design opens no socket");
		assertTrue(bare.getMessage().contains("node 'bare' needs setting 'port' as Integer"), bare.getMessage());
		assertTrue(bare.getMessage().contains("node 'bare' needs setting 'maxPacketSize' as Integer"), bare.getMessage());
	}

	@Test
	void anInterruptedStopClosesNoSocketAndTheNextStopFinishesTheJob() throws Exception
	{
		int port = freePort();
		BlockingSink<DatagramPacket> sink = new BlockingSink<>();
		Designer d = new Designer();
		reader(d, port, sink);
		pipeline = d.design().build(d.catalogue());
		pipeline.start();
		sendTo(port, "in flight");
		assertTrue(sink.enteredWithin(2000));
		java.util.concurrent.atomic.AtomicReference<Exception> thrown = new java.util.concurrent.atomic.AtomicReference<>();
		Thread stopper = Async.daemon(() ->
		{
			try
			{
				pipeline.stop();
			} catch (InterruptedException e)
			{
				thrown.set(e);
			}
		});
		assertTrue(Async.parkedWithin(stopper, 2000), "stop() waits for the reader's put");

		stopper.interrupt();

		assertTrue(Async.finishesWithin(stopper, 2000));
		assertInstanceOf(InterruptedException.class, thrown.get());
		assertFalse(isFree(port), "the reader may still be running, so its socket is left open");
		sink.release();
		pipeline.stop();
		assertTrue(isFree(port), "the next stop() closes it");
	}

	// --- a socket the host made itself is the host's: wired in as a host object, it is never closed ---

	@Test
	void aHostsOwnSocketWiredInAsASourceSurvivesStopAndThePipelineStartsAgain() throws Exception
	{
		int port = freePort();
		UDPSocketBlock<DatagramPacket> own = org.abstractica.javablocks.blocks.udp.UDPBlocks.getUDPSocketBlock(port, 1024);
		try
		{
			Sink<DatagramPacket> heard = new Sink<>();
			Designer d = new Designer();
			var source = d.hostSource("own", own);
			var reader = d.<DatagramPacket>thread("reader");
			var sink = d.hostSink("heard", heard);
			d.wire(source.out(), reader.in());
			d.wire(reader.out(), sink.in());
			pipeline = d.design().build(d.catalogue());
			pipeline.start();

			pipeline.stop();

			assertFalse(isFree(port), "the pipeline did not open this socket, so it does not close it");
			pipeline.start();
			sendTo(port, "after restart");
			assertTrue(heard.receivedWithin(1, 2000));
		} finally
		{
			if (pipeline != null) pipeline.stop();
			own.close();
		}
	}

	@Test
	void stopClosesThePipelinesOwnSocketButNotAHostsSocketBesideIt() throws Exception
	{
		int ownPort = freePort();
		UDPSocketBlock<DatagramPacket> own = org.abstractica.javablocks.blocks.udp.UDPBlocks.getUDPSocketBlock(ownPort, 1024);
		try
		{
			int port = freePort();
			Designer d = new Designer();
			d.hostSink("own", own);
			d.udpSocket("socket", port, 1024);
			pipeline = d.design().build(d.catalogue());

			pipeline.stop();

			assertTrue(isFree(port), "the udpSocket node's socket is closed");
			assertFalse(isFree(ownPort), "the host's socket is not");
		} finally
		{
			own.close();
		}
	}

	@Test
	void aHostsOwnSocketSurvivesABuildThatFails() throws Exception
	{
		int port = freePort();
		UDPSocketBlock<DatagramPacket> own = org.abstractica.javablocks.blocks.udp.UDPBlocks.getUDPSocketBlock(port, 1024);
		try (DatagramSocket taken = new DatagramSocket(0))
		{
			Designer d = new Designer();
			d.hostSink("own", own);
			d.udpSocket("second", taken.getLocalPort(), 1024);

			assertThrows(UncheckedIOException.class, () -> d.design().build(d.catalogue()));

			assertFalse(isFree(port), "a failed build closes only the sockets it opened itself");
		} finally
		{
			own.close();
		}
	}

	@Test
	void aHostsOwnSocketSharedByThePerKeyCopiesSurvivesARemovedHandler() throws Exception
	{
		int port = freePort();
		UDPSocketBlock<DatagramPacket> shared = org.abstractica.javablocks.blocks.udp.UDPBlocks.getUDPSocketBlock(port, 1024);
		try
		{
			Designer d = new Designer();
			var peers = d.map("peers", (DatagramPacket p) -> p.getPort(), perKey ->
			{
				var out = perKey.hostSinkPerKey("out", key -> shared);
				perKey.wire(perKey.entry(), out.in());
			});
			pipeline = d.design().build(d.catalogue());
			MapBlock<Integer, DatagramPacket, ?> map = pipeline.block(peers);
			byte[] bytes = "x".getBytes();
			map.put(new DatagramPacket(bytes, bytes.length, InetAddress.getLoopbackAddress(), port));

			map.removeHandler(port);
			assertFalse(isFree(port), "removing one key's copy leaves the host's shared socket open");

			pipeline.stop();
			assertFalse(isFree(port), "and so does the pipeline's stop");
		} finally
		{
			shared.close();
		}
	}

	// --- inside a map's nested Design: one socket per copy ---

	/** A map whose copy shows its messages in {@code shown} and has a socket on {@code port} with a reader. */
	private static MapNode<String, String> mapWithASocketPerKey(Designer d, int port, Sink<String> shown)
	{
		return d.map("peers", (String m) -> m.split(":")[0], perKey ->
		{
			var show = perKey.hostSinkPerKey("show", key -> shown);
			perKey.wire(perKey.entry(), show.in());
			var socket = perKey.udpSocket("socket", port, 1024);
			var reader = perKey.<DatagramPacket>thread("reader");
			var bin = perKey.<DatagramPacket>trashcan("bin");
			perKey.wire(socket.out(), reader.in());
			perKey.wire(reader.out(), bin.in());
		});
	}

	@Test
	void aCopysSocketIsClosedWhenItsHandlerIsRemovedAndOnStopAndAFreshCopyOpensItAgain() throws Exception
	{
		int port = freePort();
		Designer d = new Designer();
		var peers = mapWithASocketPerKey(d, port, new Sink<>());
		pipeline = d.design().build(d.catalogue());
		MapBlock<String, String, ?> map = pipeline.block(peers);
		pipeline.start();
		assertTrue(isFree(port), "a copy's socket is opened when its key first arrives, not at build");

		map.put("a:1");
		assertFalse(isFree(port));
		assertTrue(pipeline.isRunning(), "the copy's reader runs");

		map.removeHandler("a");
		assertTrue(isFree(port), "removing the handler stopped the copy's reader and closed its socket");

		map.put("a:2");
		assertFalse(isFree(port), "a fresh copy opened the port again");

		pipeline.stop();
		assertTrue(isFree(port), "the pipeline's stop closes its live copies' sockets");
	}

	@Test
	void aLiveCopyWithAClosedSocketRefusesTheRestartUntilItsHandlerIsRemoved() throws Exception
	{
		int port = freePort();
		Sink<String> shown = new Sink<>();
		Designer d = new Designer();
		// A map without sockets comes first: a refusal must not have started its copy's thread.
		var plain = d.map("plain", (String m) -> m.split(":")[0], perKey ->
		{
			var buffer = perKey.<String>buffer("buffer", 4);
			var worker = perKey.<String>thread("worker");
			var bin = perKey.<String>trashcan("bin");
			perKey.wire(perKey.entry(), buffer.in());
			perKey.wire(buffer.out(), worker.in());
			perKey.wire(worker.out(), bin.in());
		});
		var peers = mapWithASocketPerKey(d, port, shown);
		pipeline = d.design().build(d.catalogue());
		MapBlock<String, String, ?> map = pipeline.block(peers);
		pipeline.start();
		pipeline.block(plain).put("p:1");
		map.put("a:1");
		pipeline.stop();

		assertThrows(IllegalStateException.class, () -> pipeline.start());
		assertFalse(pipeline.isRunning(), "nothing was started, the other map's copy included");

		map.removeHandler("a");
		pipeline.start();
		map.put("a:2");

		assertFalse(isFree(port), "the key's next message built a fresh copy with an open socket");
		assertTrue(pipeline.isRunning());
		assertEquals(List.of("a:1", "a:2"), shown.items());
	}

	@Test
	void aCopyWhoseSocketCannotOpenFailsThePutAndLeavesNoHandler() throws Exception
	{
		int port = freePort();
		Sink<String> shown = new Sink<>();
		Designer d = new Designer();
		var peers = mapWithASocketPerKey(d, port, shown);
		pipeline = d.design().build(d.catalogue());
		MapBlock<String, String, ?> map = pipeline.block(peers);
		pipeline.start();
		map.put("a:1"); // this copy holds the fixed port

		var e = assertThrows(UncheckedIOException.class, () -> map.put("b:1"));

		assertTrue(e.getMessage().contains("node 'socket'"), e.getMessage());
		assertEquals(1, map.getSize());
		assertEquals(List.of("a:1"), shown.items());
	}
}
