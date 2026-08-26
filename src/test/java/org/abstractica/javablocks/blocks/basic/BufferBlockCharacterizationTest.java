package org.abstractica.javablocks.blocks.basic;

import org.abstractica.javablocks.blocks.basic.impl.BufferBlockImpl;
import org.abstractica.javablocks.blocks.basic.impl.SingleBufferBlockImpl;
import org.abstractica.javablocks.testsupport.Async;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterizes {@code v0.2.0} BufferBlock behaviour, warts included.
 * The buffer is the only place back-pressure lives: put blocks when full, get blocks when empty.
 */
class BufferBlockCharacterizationTest
{
	@Test
	void acceptsCapacityManyPutsWithoutBlocking() throws InterruptedException
	{
		BufferBlock<Integer> buffer = BasicBlocks.getBufferBlock(3);
		buffer.put(1);
		buffer.put(2);
		buffer.put(3);
		assertEquals(3, buffer.getCapacity());
		assertEquals(3, buffer.getLoad());
	}

	@Test
	void putOnFullBufferBlocksTheProducerUntilAGet() throws InterruptedException
	{
		BufferBlock<Integer> buffer = BasicBlocks.getBufferBlock(3);
		buffer.put(1);
		buffer.put(2);
		buffer.put(3);

		Thread producer = Async.daemon(() -> buffer.put(4));
		assertTrue(Async.stillRunningAfter(producer, 200), "fourth put should block on a full buffer");
		assertEquals(3, buffer.getLoad());

		assertEquals(1, buffer.get());
		assertTrue(Async.finishesWithin(producer, 1000), "put should complete once a slot frees");
		assertEquals(3, buffer.getLoad());
		assertEquals(List.of(2, 3, 4), List.of(buffer.get(), buffer.get(), buffer.get()));
	}

	@Test
	void getOnEmptyBufferBlocksTheConsumerUntilAPut() throws InterruptedException
	{
		BufferBlock<Integer> buffer = BasicBlocks.getBufferBlock(3);
		AtomicReference<Integer> got = new AtomicReference<>();

		Thread consumer = Async.daemon(() -> got.set(buffer.get()));
		assertTrue(Async.stillRunningAfter(consumer, 200), "get should block on an empty buffer");

		buffer.put(42);
		assertTrue(Async.finishesWithin(consumer, 1000), "get should complete once an item arrives");
		assertEquals(42, got.get());
		assertEquals(0, buffer.getLoad());
	}

	@Test
	void isFifoAcrossRingWraparound() throws InterruptedException
	{
		BufferBlock<Integer> buffer = BasicBlocks.getBufferBlock(3);
		buffer.put(1);
		buffer.put(2);
		buffer.put(3);
		assertEquals(1, buffer.get());
		assertEquals(2, buffer.get());
		buffer.put(4);
		buffer.put(5); // wraps around the ring
		assertEquals(List.of(3, 4, 5), List.of(buffer.get(), buffer.get(), buffer.get()));
	}

	@Test
	void blockedPutThrowsInterruptedExceptionWhenInterrupted() throws InterruptedException
	{
		BufferBlock<Integer> buffer = BasicBlocks.getBufferBlock(1);
		buffer.put(1);
		AtomicBoolean interrupted = new AtomicBoolean();
		Thread producer = Async.daemon(() ->
		{
			try
			{
				buffer.put(2);
			} catch (InterruptedException e)
			{
				interrupted.set(true);
			}
		});
		assertTrue(Async.stillRunningAfter(producer, 100));
		producer.interrupt();
		assertTrue(Async.finishesWithin(producer, 1000));
		assertTrue(interrupted.get());
		assertEquals(1, buffer.getLoad(), "the interrupted item is not stored");
	}

	@Test
	void blockedGetThrowsInterruptedExceptionWhenInterrupted() throws InterruptedException
	{
		BufferBlock<Integer> buffer = BasicBlocks.getBufferBlock(3);
		AtomicBoolean interrupted = new AtomicBoolean();
		Thread consumer = Async.daemon(() ->
		{
			try
			{
				buffer.get();
			} catch (InterruptedException e)
			{
				interrupted.set(true);
			}
		});
		assertTrue(Async.stillRunningAfter(consumer, 100));
		consumer.interrupt();
		assertTrue(Async.finishesWithin(consumer, 1000));
		assertTrue(interrupted.get());
	}

	@Test
	void capacityOneIsTheSingleSlotVariantWithTheSameSemantics() throws InterruptedException
	{
		BufferBlock<String> buffer = BasicBlocks.getBufferBlock(1);
		assertInstanceOf(SingleBufferBlockImpl.class, buffer);
		assertInstanceOf(BufferBlockImpl.class, BasicBlocks.getBufferBlock(2));

		assertEquals(1, buffer.getCapacity());
		assertEquals(0, buffer.getLoad());
		buffer.put("a");
		assertEquals(1, buffer.getLoad());

		Thread producer = Async.daemon(() -> buffer.put("b"));
		assertTrue(Async.stillRunningAfter(producer, 200), "second put blocks on the single slot");
		assertEquals("a", buffer.get());
		assertTrue(Async.finishesWithin(producer, 1000));
		assertEquals("b", buffer.get());
	}

	/** Wart: a capacity-0 buffer is constructible and every put on it blocks forever. */
	@Test
	void capacityZeroBufferBlocksEveryPut() throws InterruptedException
	{
		BufferBlock<Integer> buffer = BasicBlocks.getBufferBlock(0);
		assertEquals(0, buffer.getCapacity());
		Thread producer = Async.daemon(() -> buffer.put(1));
		assertTrue(Async.stillRunningAfter(producer, 200));
		producer.interrupt();
		assertTrue(Async.finishesWithin(producer, 1000));
	}
}
