package org.abstractica.javablocks.blocks.basic;

import org.abstractica.javablocks.testsupport.Async;
import org.abstractica.javablocks.testsupport.Sink;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterizes {@code v0.2.0} push/pull/variable/constant/trashcan blocks, the delay functions,
 * the process-wide singletons, and the library's timer idiom (constant + thread + delay).
 */
class FunctionBlocksCharacterizationTest
{
	@Test
	void pushBlockAppliesTheFunctionAndForwardsOnTheCallersThread() throws InterruptedException
	{
		Sink<String> sink = new Sink<>();
		PushBlock<Integer, String> push = BasicBlocks.getPushBlock(n -> "n" + n);
		assertThrows(IllegalStateException.class, () -> push.put(1), "unconnected push block");

		push.setOutput(sink);
		push.put(1);
		push.put(2);
		assertEquals(List.of("n1", "n2"), sink.items());
		assertSame(Thread.currentThread(), sink.threads().getFirst());
	}

	@Test
	void pullBlockAppliesTheFunctionOnGetOnTheCallersThread() throws InterruptedException
	{
		PullBlock<Integer, String> pull = BasicBlocks.getPullBlock(n -> "n" + n);
		assertThrows(IllegalStateException.class, pull::get, "unconnected pull block");

		AtomicReference<Thread> pulledOn = new AtomicReference<>();
		pull.setInput(() -> { pulledOn.set(Thread.currentThread()); return 5; });
		assertEquals("n5", pull.get());
		assertSame(Thread.currentThread(), pulledOn.get());
	}

	@Test
	void variableBlockIsALossyRegisterAndConstantBlockNeverChanges() throws InterruptedException
	{
		VariableBlock<String> variable = BasicBlocks.getVariableBlock("init");
		assertEquals("init", variable.get());
		variable.put("one");
		variable.put("two");
		assertEquals("two", variable.get());
		assertEquals("two", variable.get(), "get does not consume");

		ConstantBlock<Integer> constant = BasicBlocks.getConstantBlock(7);
		assertEquals(7, constant.get());
		assertEquals(7, constant.get());
	}

	@Test
	void trashcanSwallowsAndKeyboardAndConsoleAreSingletons()
	{
		Output<Object> trash = BasicBlocks.getTrashcanBlock();
		assertDoesNotThrow(() -> trash.put(new Object()));
		assertSame(BasicBlocks.getKeyboardBlock(), BasicBlocks.getKeyboardBlock());
		assertSame(BasicBlocks.getConsoleBlock(), BasicBlocks.getConsoleBlock());
	}

	/** Wart: the delay function absorbs interrupts and still sleeps out the full delay. */
	@Test
	void delayFunctionAbsorbsInterruptsAndDelaysTheFullTime() throws InterruptedException
	{
		Function<String, String> delay = BasicFunctions.DelayFunction(200);
		AtomicReference<String> result = new AtomicReference<>();
		long start = System.nanoTime();
		Thread t = Async.daemon(() -> result.set(delay.apply("x")));
		Thread.sleep(20);
		t.interrupt();
		assertTrue(Async.finishesWithin(t, 2000));
		long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
		assertTrue(elapsedMillis >= 200, "returned after " + elapsedMillis + " ms despite the interrupt");
		assertEquals("x", result.get());
	}

	/** Wart: the random delay absorbs interrupts too (range [200, 201) ms, so the delay is 200 ms). */
	@Test
	void randomDelayFunctionAbsorbsInterruptsAndDelaysTheFullTime() throws InterruptedException
	{
		Function<String, String> delay = BasicFunctions.getRandomDelayFunction(200, 201);
		AtomicReference<String> result = new AtomicReference<>();
		long start = System.nanoTime();
		Thread t = Async.daemon(() -> result.set(delay.apply("x")));
		Thread.sleep(20);
		t.interrupt();
		assertTrue(Async.finishesWithin(t, 2000));
		long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
		assertTrue(elapsedMillis >= 200, "returned after " + elapsedMillis + " ms despite the interrupt");
		assertEquals("x", result.get());
	}

	/**
	 * The library's timer idiom as FirstTest wires it (delay on the push side): it ticks, and since
	 * J4 it can be stopped. stop() waits for the tick in flight (the delayed put), then the worker
	 * sees the stop and exits; no tick arrives after stop() returns. (v0.2.0 never returned here:
	 * it re-waited in 5 s rounds or deadlocked on join().)
	 */
	@Test
	void constantPlusThreadPlusPushSideDelayTicksAndCanBeStopped() throws InterruptedException
	{
		Sink<Long> ticks = new Sink<>();
		PushBlock<Long, Long> delay = BasicBlocks.getPushBlock(BasicFunctions.DelayFunction(100));
		delay.setOutput(ticks);
		delay.setDebugReporter(BasicBlocks.getTrashcanBlock());
		ThreadBlock<Long> thread = BasicBlocks.getThreadBlock();
		thread.setInput(BasicBlocks.getConstantBlock(1L));
		thread.setOutput(delay);
		thread.setDebugReporter(BasicBlocks.getTrashcanBlock());

		thread.start();
		assertTrue(ticks.receivedWithin(3, 2000), "ticks keep coming");
		assertTrue(ticks.items().stream().allMatch(v -> v == 1L));
		assertEquals(1, ticks.threads().stream().distinct().count(), "all ticks on the worker thread");

		Thread stopper = Async.daemon(thread::stop);
		assertTrue(Async.finishesWithin(stopper, 1000), "stop() returns after at most the tick in flight");
		assertFalse(thread.isRunning());
		assertFalse(ticks.threads().getFirst().isAlive(), "the worker has exited");
		int atReturn = ticks.items().size();
		Thread.sleep(300);
		assertEquals(atReturn, ticks.items().size(), "no ticks after stop() returned");
	}

	/**
	 * The same idiom with the delay on the pull side. The worker sits inside get() (the delay's
	 * sleep, which swallows the interrupt), so it finishes that delay, delivers that one tick, sees
	 * the stop, and exits. Deterministic: exactly one tick after stop() is called, then stop()
	 * returns. (v0.2.0 delivered the same tick and then deadlocked on the monitor.)
	 */
	@Test
	void constantPlusPullSideDelayPlusThreadTicksOnceMoreAfterStopThenStops() throws InterruptedException
	{
		Sink<Long> ticks = new Sink<>();
		PullBlock<Long, Long> delay = BasicBlocks.getPullBlock(BasicFunctions.DelayFunction(100));
		delay.setInput(BasicBlocks.getConstantBlock(1L));
		ThreadBlock<Long> thread = BasicBlocks.getThreadBlock();
		thread.setInput(delay);
		thread.setOutput(ticks);
		thread.setDebugReporter(BasicBlocks.getTrashcanBlock());

		thread.start();
		assertTrue(ticks.receivedWithin(3, 2000), "ticks keep coming");
		Thread worker = ticks.threads().getFirst();
		assertTrue(Async.parkedWithin(worker, 1000), "worker is sleeping inside get()");
		int atStop = ticks.items().size();

		Thread stopper = Async.daemon(thread::stop);
		assertTrue(Async.finishesWithin(stopper, 1000), "stop() returns");
		assertEquals(atStop + 1, ticks.items().size(), "exactly the tick in flight is delivered");
		assertFalse(worker.isAlive(), "the worker has exited");
		assertFalse(thread.isRunning());
	}
}
