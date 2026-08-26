package org.abstractica.javablocks.testsupport;

import org.abstractica.javablocks.blocks.basic.Output;

import java.util.concurrent.CountDownLatch;

/** A sink whose {@code put} blocks until {@link #release()} — for observing what a caller does mid-put. */
public final class BlockingSink<E> implements Output<E>
{
	private final CountDownLatch entered = new CountDownLatch(1);
	private final CountDownLatch gate = new CountDownLatch(1);
	public final Sink<E> delivered = new Sink<>();

	@Override
	public void put(E item) throws InterruptedException
	{
		entered.countDown();
		gate.await();
		delivered.put(item);
	}

	/** True once a caller is inside {@code put}. */
	public boolean enteredWithin(long millis) throws InterruptedException
	{
		return entered.await(millis, java.util.concurrent.TimeUnit.MILLISECONDS);
	}

	public void release()
	{
		gate.countDown();
	}
}
