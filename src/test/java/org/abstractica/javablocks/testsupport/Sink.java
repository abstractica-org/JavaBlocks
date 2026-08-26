package org.abstractica.javablocks.testsupport;

import org.abstractica.javablocks.blocks.basic.Output;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** A recording sink: keeps every item and the thread that delivered it. */
public final class Sink<E> implements Output<E>
{
	private final List<E> items = new CopyOnWriteArrayList<>();
	private final List<Thread> threads = new CopyOnWriteArrayList<>();

	@Override
	public void put(E item)
	{
		items.add(item);
		threads.add(Thread.currentThread());
	}

	public List<E> items()
	{
		return items;
	}

	public List<Thread> threads()
	{
		return threads;
	}

	/** Blocks until at least {@code n} items have arrived, or {@code millis} have passed. */
	public boolean receivedWithin(int n, long millis) throws InterruptedException
	{
		return Async.becomesTrueWithin(() -> items.size() >= n, millis);
	}
}
