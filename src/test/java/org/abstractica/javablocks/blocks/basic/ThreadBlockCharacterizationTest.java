package org.abstractica.javablocks.blocks.basic;

import org.abstractica.javablocks.testsupport.Async;
import org.abstractica.javablocks.testsupport.Sink;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterizes {@code v0.2.0} ThreadBlock behaviour, warts included.
 * ThreadBlock is the only thread owner; its loop is {@code output.put(input.get())}.
 *
 * <p>The stop() contract this pins: stop() completes only when the worker is parked inside an
 * interruptible get() (a buffer). Otherwise it never returns — either it keeps observing
 * doingOutput == true and re-waits in 5 s rounds, or it sees false, interrupts, and then
 * join()s while holding the block's monitor, which the worker needs for its post-put
 * notifyAll(): a deadlock that also blocks every later isRunning() call. Tests that leave a
 * worker deadlocked leave a non-daemon thread behind; surefire exits regardless.
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
	 * Wart: stop() join()s while holding the monitor. A worker whose get() does not throw on
	 * interrupt comes back to put, then blocks forever on the monitor for its notifyAll().
	 */
	@Test
	void stopDeadlocksWithAWorkerWhoseGetSurvivesTheInterrupt() throws InterruptedException
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
		Thread.sleep(100); // worker is inside get(), doingOutput == false

		Thread stopper = Async.daemon(thread::stop); // sees false, sets running = false, interrupts, joins with the lock held
		assertTrue(Async.stillRunningAfter(stopper, 200));
		release.countDown(); // get returns; the worker puts, then needs the monitor

		assertTrue(out.receivedWithin(1, 1000), "the item still goes through");
		Thread worker = out.threads().getFirst();
		assertTrue(Async.becomesTrueWithin(() -> worker.getState() == Thread.State.BLOCKED, 1000),
				"worker is blocked on the monitor stop() holds");
		assertTrue(Async.stillRunningAfter(stopper, 300), "stop() never returns");
		assertTrue(Async.stillRunningAfter(Async.daemon(thread::isRunning), 300), "and isRunning() blocks behind it");
	}

	/**
	 * Wart: a RuntimeException from downstream kills the worker thread (rethrown, uncaught),
	 * but the block still reports running, and stop() never returns because doingOutput
	 * was never reset. This test leaves a daemon thread parked in stop().
	 */
	@Test
	void downstreamExceptionKillsTheWorkerButTheBlockStillReportsRunningAndStopHangs() throws InterruptedException
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
		Thread.sleep(100);
		in.put(2); // nobody is consuming any more: the worker is dead
		Thread.sleep(100);
		assertEquals(1, in.getLoad(), "the second item is never taken");
		assertEquals(1, attempts.get(), "the worker did not survive the exception");
		assertTrue(thread.isRunning(), "wart: still reports running after the worker died");

		Thread stopper = Async.daemon(thread::stop);
		assertTrue(Async.stillRunningAfter(stopper, 1000), "wart: stop() never returns");
	}
}
