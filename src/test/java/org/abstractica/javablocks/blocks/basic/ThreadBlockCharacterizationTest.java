package org.abstractica.javablocks.blocks.basic;

import org.abstractica.javablocks.testsupport.Async;
import org.abstractica.javablocks.testsupport.Sink;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterizes ThreadBlock behaviour: {@code v0.2.0}, with the stop() fix ruled in J4.
 * ThreadBlock is the only thread owner; its loop is {@code output.put(input.get())}.
 *
 * <p>The stop() contract this pins (J4): stop() waits for an in-flight put to complete, then
 * interrupts the worker and joins it without holding the block's monitor. A worker parked in an
 * interruptible get() (a buffer) exits at once; one inside a get() that survives the interrupt
 * finishes that get, delivers that item, and exits. isRunning() never blocks and turns false
 * when the worker ends for any reason, including a downstream exception.
 */
class ThreadBlockCharacterizationTest
{
	@Test
	void startThrowsUnlessBothSidesAreConnected()
	{
		ThreadBlock<Integer> thread = BasicBlocks.getThreadBlock();
		assertThrows(IllegalStateException.class, thread::start);

		thread.setInput(BasicBlocks.getBufferBlock(1));
		assertThrows(IllegalStateException.class, thread::start, "output missing");

		ThreadBlock<Integer> other = BasicBlocks.getThreadBlock();
		other.setOutput(new Sink<>());
		assertThrows(IllegalStateException.class, other::start, "input missing");

		assertFalse(thread.isRunning());
		assertFalse(other.isRunning());
	}

	@Test
	void handsOffPullToPushOnItsOwnThread() throws InterruptedException
	{
		BufferBlock<Integer> in = BasicBlocks.getBufferBlock(4);
		Sink<Integer> out = new Sink<>();
		ThreadBlock<Integer> thread = BasicBlocks.getThreadBlock();
		thread.setInput(in);
		thread.setOutput(out);

		thread.start();
		assertTrue(thread.isRunning());
		in.put(7);
		in.put(8);
		in.put(9);

		assertTrue(out.receivedWithin(3, 1000));
		assertEquals(List.of(7, 8, 9), out.items());
		assertEquals(1, out.threads().stream().distinct().count(), "one worker thread delivers everything");
		Thread worker = out.threads().getFirst();
		assertNotSame(Thread.currentThread(), worker, "delivery is not on the producer's thread");

		assertTrue(Async.parkedWithin(worker, 1000), "worker is back in get() on the empty buffer");
		thread.stop();
		assertFalse(thread.isRunning());
		assertFalse(worker.isAlive());
	}

	@Test
	void stopWakesAWorkerBlockedOnItsInput() throws InterruptedException
	{
		ThreadBlock<Integer> thread = BasicBlocks.getThreadBlock();
		thread.setInput(BasicBlocks.getBufferBlock(1)); // empty: the worker blocks in get()
		thread.setOutput(new Sink<>());
		thread.start();
		Thread.sleep(100);

		Thread stopper = Async.daemon(thread::stop);
		assertTrue(Async.finishesWithin(stopper, 1000), "stop interrupts the blocked get and joins");
		assertFalse(thread.isRunning());
	}

	@Test
	void stopReturnsOnlyAfterAnInFlightPutCompletes() throws InterruptedException
	{
		BufferBlock<String> in = BasicBlocks.getBufferBlock(1);
		BufferBlock<String> out = BasicBlocks.getBufferBlock(1);
		out.put("x"); // downstream is full: the worker's put will block
		ThreadBlock<String> thread = BasicBlocks.getThreadBlock();
		thread.setInput(in);
		thread.setOutput(out);
		thread.start();

		in.put("a");
		assertTrue(Async.becomesTrueWithin(() -> in.getLoad() == 0, 1000), "worker took the item");
		Thread.sleep(100); // and is now blocked inside out.put("a")

		Thread stopper = Async.daemon(thread::stop);
		assertTrue(Async.stillRunningAfter(stopper, 300), "stop waits for the in-flight put");
		assertTrue(thread.isRunning());

		assertEquals("x", out.get()); // free the slot: the put completes, the worker parks in in.get()
		assertTrue(Async.finishesWithin(stopper, 2000), "stop returns once the put has completed");
		assertFalse(thread.isRunning());
		assertEquals("a", out.get(), "the in-flight item was delivered, not dropped");
	}

	@Test
	void canBeRestartedAfterStop() throws InterruptedException
	{
		BufferBlock<Integer> in = BasicBlocks.getBufferBlock(2);
		Sink<Integer> out = new Sink<>();
		ThreadBlock<Integer> thread = BasicBlocks.getThreadBlock();
		thread.setInput(in);
		thread.setOutput(out);

		thread.start();
		in.put(1);
		assertTrue(out.receivedWithin(1, 1000));
		assertTrue(Async.parkedWithin(out.threads().get(0), 1000));
		thread.stop();

		thread.start();
		in.put(2);
		assertTrue(out.receivedWithin(2, 1000));
		assertTrue(Async.parkedWithin(out.threads().get(1), 1000));
		thread.stop();

		assertEquals(List.of(1, 2), out.items());
		assertEquals(2, out.threads().stream().distinct().count(), "each start is a fresh thread");
	}

	@Test
	void secondStartIsANoOpAndSettersThrowWhileRunning() throws InterruptedException
	{
		ThreadBlock<Integer> thread = BasicBlocks.getThreadBlock();
		thread.setInput(BasicBlocks.getBufferBlock(1));
		thread.setOutput(new Sink<>());
		thread.start();
		assertDoesNotThrow(thread::start);
		assertThrows(IllegalStateException.class, () -> thread.setInput(BasicBlocks.getBufferBlock(1)));
		assertThrows(IllegalStateException.class, () -> thread.setOutput(new Sink<>()));
		Thread.sleep(100);
		thread.stop();
		assertDoesNotThrow(thread::stop, "stop when not running is a no-op");
	}

	/**
	 * J4: a worker whose get() does not throw on interrupt finishes that get, delivers the item,
	 * and exits; stop() waits for it without holding the monitor, so isRunning() stays answerable.
	 * (v0.2.0 deadlocked here: stop() joined while holding the monitor the worker needed.)
	 */
	@Test
	void stopWaitsForAnUninterruptibleGetThenDeliversTheItemAndReturns() throws InterruptedException
	{
		CountDownLatch release = new CountDownLatch(1);
		Input<String> uninterruptible = () ->
		{
			while (true)
			{
				try
				{
					release.await();
					return "item";
				} catch (InterruptedException ignored)
				{
					// like DelayFunction and the socket blocks: swallow and carry on
				}
			}
		};
		Sink<String> out = new Sink<>();
		ThreadBlock<String> thread = BasicBlocks.getThreadBlock();
		thread.setInput(uninterruptible);
		thread.setOutput(out);
		thread.setDebugReporter(BasicBlocks.getTrashcanBlock());
		thread.start();
		Thread.sleep(100); // worker is inside get()

		Thread stopper = Async.daemon(thread::stop);
		assertTrue(Async.stillRunningAfter(stopper, 200), "stop waits: the get swallowed the interrupt");
		Thread asker = Async.daemon(thread::isRunning);
		assertTrue(Async.finishesWithin(asker, 300), "isRunning() does not block behind stop()");
		assertTrue(thread.isRunning(), "still running until the worker exits");

		release.countDown(); // get returns; the worker puts, sees the stop, exits
		assertTrue(Async.finishesWithin(stopper, 1000), "stop() returns");
		assertEquals(List.of("item"), out.items(), "the item in hand is delivered, not dropped");
		assertFalse(thread.isRunning());
		assertFalse(out.threads().getFirst().isAlive(), "the worker has exited");
	}

	/**
	 * J4: the interrupt stop() aims at a get() that ignores it must not abort the put of the item
	 * that get() returns: the worker puts into a full buffer, stop() waits for that put, and the
	 * item arrives once the buffer has room.
	 */
	@Test
	void theInterruptAimedAtTheGetDoesNotAbortThePutThatFollows() throws InterruptedException
	{
		AtomicBoolean released = new AtomicBoolean();
		Input<String> ignoresInterrupts = () ->
		{
			while (!released.get()) Thread.onSpinWait(); // never looks at the interrupt flag, like ConstantBlock
			return "item";
		};
		BufferBlock<String> out = BasicBlocks.getBufferBlock(1);
		out.put("x"); // downstream is full: the worker's put will block
		ThreadBlock<String> thread = BasicBlocks.getThreadBlock();
		thread.setInput(ignoresInterrupts);
		thread.setOutput(out);
		thread.setDebugReporter(BasicBlocks.getTrashcanBlock());
		thread.start();
		Thread.sleep(100); // worker is inside get()

		Thread stopper = Async.daemon(thread::stop); // interrupts the get, which ignores it: the flag stays set
		assertTrue(Async.stillRunningAfter(stopper, 200));
		released.set(true); // get returns "item" with the interrupt still pending; the put blocks on the full buffer
		assertTrue(Async.stillRunningAfter(stopper, 300), "stop waits for the put, which was not aborted");

		assertEquals("x", out.get()); // free the slot
		assertTrue(Async.finishesWithin(stopper, 1000), "stop returns once the put has completed");
		assertEquals(1, out.getLoad(), "the item was delivered");
		assertEquals("item", out.get());
	}

	/**
	 * J4: a RuntimeException from downstream still kills the worker (rethrown, uncaught), but the
	 * block now reports stopped and stop() returns at once. (v0.2.0 kept reporting running and
	 * stop() hung, because the in-flight flag was never reset.)
	 */
	@Test
	void downstreamExceptionKillsTheWorkerAndTheBlockReportsStopped() throws InterruptedException
	{
		BufferBlock<Integer> in = BasicBlocks.getBufferBlock(1);
		AtomicInteger attempts = new AtomicInteger();
		Output<Integer> failing = item ->
		{
			attempts.incrementAndGet();
			throw new IllegalArgumentException("downstream rejects " + item);
		};
		ThreadBlock<Integer> thread = BasicBlocks.getThreadBlock();
		thread.setInput(in);
		thread.setOutput(failing);
		thread.start();

		in.put(1);
		assertTrue(Async.becomesTrueWithin(() -> attempts.get() == 1, 1000));
		assertTrue(Async.becomesTrueWithin(() -> !thread.isRunning(), 1000), "reports stopped once the worker died");
		in.put(2); // nobody is consuming any more: the worker is dead
		Thread.sleep(100);
		assertEquals(1, in.getLoad(), "the second item is never taken");
		assertEquals(1, attempts.get(), "the worker did not survive the exception");

		Thread stopper = Async.daemon(thread::stop);
		assertTrue(Async.finishesWithin(stopper, 1000), "stop() on a dead worker returns at once");
	}
}
