package org.abstractica.javablocks.blocks.basic;

import org.abstractica.javablocks.testsupport.Async;
import org.abstractica.javablocks.testsupport.BlockingSink;
import org.abstractica.javablocks.testsupport.Sink;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/** Characterizes {@code v0.2.0} DistributorBlock behaviour — the only broadcast. */
class DistributorBlockCharacterizationTest
{
	@Test
	void forwardsToEveryOutputInRegistrationOrderOnTheCallersThread() throws InterruptedException
	{
		List<String> log = new CopyOnWriteArrayList<>();
		List<Thread> threads = new CopyOnWriteArrayList<>();
		Output<Integer> a = item -> { log.add("a" + item); threads.add(Thread.currentThread()); };
		Output<Integer> b = item -> { log.add("b" + item); threads.add(Thread.currentThread()); };
		Output<Integer> c = item -> { log.add("c" + item); threads.add(Thread.currentThread()); };

		DistributorBlock<Integer> distributor = BasicBlocks.getDistributorBlock();
		distributor.addOutput(b);
		distributor.addOutput(a);
		distributor.addOutput(c);

		distributor.put(1);
		distributor.put(2);

		assertEquals(List.of("b1", "a1", "c1", "b2", "a2", "c2"), log);
		assertTrue(threads.stream().allMatch(t -> t == Thread.currentThread()));
	}

	@Test
	void withNoOutputsPutIsANoOp()
	{
		DistributorBlock<Integer> distributor = BasicBlocks.getDistributorBlock();
		assertDoesNotThrow(() -> distributor.put(1));
	}

	@Test
	void removeOutputStopsForwardingAndGetOutputsIsADefensiveCopy() throws InterruptedException
	{
		Sink<Integer> a = new Sink<>();
		Sink<Integer> b = new Sink<>();
		DistributorBlock<Integer> distributor = BasicBlocks.getDistributorBlock();
		distributor.addOutput(a);
		distributor.addOutput(b);

		assertTrue(distributor.removeOutput(a));
		assertFalse(distributor.removeOutput(a), "already gone");
		distributor.put(1);
		assertEquals(List.of(), a.items());
		assertEquals(List.of(1), b.items());

		Collection<Output<Integer>> outputs = distributor.getOutputs();
		outputs.clear();
		distributor.put(2);
		assertEquals(List.of(1, 2), b.items(), "clearing the copy did not touch the block");
	}

	/** Wart: put holds the block's monitor while forwarding, so a slow output blocks addOutput/removeOutput. */
	@Test
	void putHoldsTheMonitorWhileForwarding() throws InterruptedException
	{
		BlockingSink<Integer> slow = new BlockingSink<>();
		DistributorBlock<Integer> distributor = BasicBlocks.getDistributorBlock();
		distributor.addOutput(slow);

		Thread producer = Async.daemon(() -> distributor.put(1));
		assertTrue(slow.enteredWithin(1000));

		Thread adder = Async.daemon(() -> distributor.addOutput(new Sink<>()));
		assertTrue(Async.stillRunningAfter(adder, 200), "addOutput waits for the in-flight put");

		slow.release();
		assertTrue(Async.finishesWithin(producer, 1000));
		assertTrue(Async.finishesWithin(adder, 1000));
		assertEquals(2, distributor.getOutputs().size());
	}
}
