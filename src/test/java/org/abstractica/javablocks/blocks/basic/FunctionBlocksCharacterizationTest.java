package org.abstractica.javablocks.blocks.basic;

import org.abstractica.javablocks.testsupport.Async;
import org.abstractica.javablocks.testsupport.Sink;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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

	/**
	 * The library's timer idiom as FirstTest wires it (delay on the push side): it ticks, and it
	 * cannot be stopped. stop() either keeps observing doingOutput == true (the get() is
	 * instantaneous, so the window is microseconds) and re-waits in 5 s rounds, or catches the
	 * window and deadlocks on join() — see ThreadBlockCharacterizationTest. Either way it never
	 * returns, and whether ticks continue afterwards depends on which path it took. The test ends
	 * a still-ticking worker the only way v0.2.0 allows: a downstream RuntimeException kills it.
	 */
	@Test
	void constantPlusThreadPlusPushSideDelayTicksAndCannotBeStopped() throws InterruptedException
	{
		AtomicBoolean poison = new AtomicBoolean();
		Sink<Long> ticks = new Sink<>();
		Output<Long> sink = item ->
		{
			if (poison.get()) throw new IllegalStateException("test over: killing the unstoppable worker");
			ticks.put(item);
		};
		PushBlock<Long, Long> delay = BasicBlocks.getPushBlock(BasicFunctions.DelayFunction(100));
		delay.setOutput(sink);
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
		assertTrue(Async.stillRunningAfter(stopper, 1500), "stop() never returns");

		poison.set(true);
		Thread.sleep(400);
		int afterPoison = ticks.items().size();
		Thread.sleep(300);
		assertEquals(afterPoison, ticks.items().size(), "no ticks once the worker is dead or deadlocked");
	}

	/**
	 * The same idiom with the delay on the pull side. The worker sits inside get() with
	 * doingOutput == false, so stop() proceeds immediately — the interrupt is swallowed by the
	 * delay, the worker delivers the tick in flight, and then deadlocks on the monitor stop()
	 * holds in join(). Deterministic: exactly one tick after stop() is called, then silence.
	 */
	@Test
	void constantPlusPullSideDelayPlusThreadTicksOnceMoreAfterStopThenDeadlocks() throws InterruptedException
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
		assertTrue(Async.stillRunningAfter(stopper, 1000), "stop() never returns");
		assertEquals(atStop + 1, ticks.items().size(), "exactly the tick in flight is delivered");
		assertEquals(Thread.State.BLOCKED, worker.getState(), "worker is deadlocked on the monitor");
	}
}
