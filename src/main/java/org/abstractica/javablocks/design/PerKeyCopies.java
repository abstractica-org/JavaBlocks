package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.MapHandlerBlock;
import org.abstractica.javablocks.blocks.basic.MapHandlerBlockFactory;
import org.abstractica.javablocks.blocks.basic.Output;
import org.abstractica.javablocks.blocks.basic.impl.AbstractBlock;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * A map node's handler factory: builds a copy of the nested Design for each new key, and starts and
 * stops the live copies with the pipeline. The map calls {@link #createHandlerBlockFor} under its own
 * lock, on the thread whose item first carried the key.
 */
final class PerKeyCopies<K, E> implements MapHandlerBlockFactory<K, E, PerKeyCopies.Handler<K, E>>
{
	private final Function<E, K> key;
	private final Design design;
	private final Catalogue catalogue;
	/** Guarded by {@code this}. */
	private final Set<Handler<K, E>> live = new LinkedHashSet<>();
	/** Guarded by {@code this}: whether the pipeline is started, so new copies start too. */
	private boolean running;

	PerKeyCopies(Function<E, K> key, Design design, Catalogue catalogue)
	{
		this.key = key;
		this.design = design;
		this.catalogue = catalogue;
	}

	@Override
	public K extractKey(E item)
	{
		return key.apply(item);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Handler<K, E> createHandlerBlockFor(K k)
	{
		Handler<K, E> handler = (Handler<K, E>) PipelineBuilder.buildCopy(design, catalogue, k);
		synchronized (this)
		{
			live.add(handler);
			// Started under this lock, so a concurrent stop() either sees the copy or prevents its start.
			if (running) handler.copy.start();
		}
		return handler;
	}

	/**
	 * Stops the removed handler's copy, each thread as {@link org.abstractica.javablocks.blocks.basic.ThreadBlock#stop()}
	 * does. The copy stays tracked until its stop has finished: if this thread is interrupted first, the
	 * flag is kept and the pipeline's next {@code stop()} finishes the job; its next {@code start()} never
	 * restarts it.
	 */
	@Override
	public void destroyHandlerBlock(Handler<K, E> handler)
	{
		synchronized (this)
		{
			handler.removed = true;
		}
		try
		{
			handler.copy.stop();
		} catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			return;
		}
		synchronized (this)
		{
			live.remove(handler);
		}
	}

	synchronized void start()
	{
		running = true;
		for (Handler<K, E> h : live)
		{
			if (!h.removed) h.copy.start();
		}
	}

	/** Stops every live copy; outside this lock, since each stop waits for an in-flight put. */
	void stop() throws InterruptedException
	{
		List<Handler<K, E>> copies;
		synchronized (this)
		{
			running = false;
			copies = new ArrayList<>(live);
		}
		for (Handler<K, E> h : copies)
		{
			h.copy.stop();
			synchronized (this)
			{
				if (h.removed) live.remove(h);
			}
		}
	}

	boolean isRunning()
	{
		List<Handler<K, E>> copies;
		synchronized (this)
		{
			copies = new ArrayList<>(live);
		}
		for (Handler<K, E> h : copies)
		{
			if (h.copy.isRunning()) return true;
		}
		return false;
	}

	/** What a nested Design's {@code entry} node is built into: the map's handler for one key. */
	static final class Handler<K, E> extends AbstractBlock implements MapHandlerBlock<K, E>
	{
		private final K key;
		private Output<E> target;
		/** The copy this handler enters; set once it is built. */
		Pipeline copy;
		/** Guarded by the factory: removed from the map, its copy never to start again. */
		boolean removed;

		Handler(K key)
		{
			this.key = key;
		}

		void setTarget(Output<E> target)
		{
			this.target = target;
		}

		@Override
		public K getKey()
		{
			return key;
		}

		@Override
		public void put(E item) throws InterruptedException
		{
			target.put(item);
		}
	}
}
