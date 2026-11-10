package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.ThreadBlock;
import org.abstractica.javablocks.blocks.basic.ThreadControl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A pipeline built from a {@link Design}: its blocks, wired, and control of its threads.
 */
public final class Pipeline implements ThreadControl
{
	private final Map<String, Object> blocks;
	private final List<ThreadBlock<?>> threads = new ArrayList<>();
	/** The map nodes' per-key copies. */
	private final List<PerKeyCopies<?, ?>> maps;

	Pipeline(Map<String, Object> blocks, List<PerKeyCopies<?, ?>> maps)
	{
		this.blocks = Collections.unmodifiableMap(blocks);
		this.maps = List.copyOf(maps);
		for (Object o : blocks.values())
		{
			if (o instanceof ThreadBlock<?> t) threads.add(t);
		}
	}

	/** The object {@code node} was built into, typed. */
	@SuppressWarnings("unchecked")
	public <B> B block(NodeHandle<B> node)
	{
		return (B) block(node.id());
	}

	/**
	 * The object the node with {@code id} was built into.
	 *
	 * @throws IllegalArgumentException if the Design has no such node
	 */
	public Object block(String id)
	{
		Object o = blocks.get(id);
		if (o == null)
		{
			throw new IllegalArgumentException("the pipeline has no node '" + id + "'");
		}
		return o;
	}

	/**
	 * Starts the maps' live per-key copies, then every thread block in the Design's node order. A copy
	 * made while the pipeline runs starts at once.
	 */
	@Override
	public void start()
	{
		for (PerKeyCopies<?, ?> m : maps) m.start();
		for (ThreadBlock<?> t : threads) t.start();
	}

	/**
	 * Stops every thread block, in the Design's node order; each stop waits as {@link ThreadBlock#stop()}
	 * does. One that never returns holds up the rest: a thread pulling from a UDP socket (wired in as a
	 * host source) does not wake on its stop until the socket is closed.
	 *
	 * <p>Then stops every map's live per-key copies, once the threads that feed them have stopped. A copy
	 * made while the pipeline is stopped is built but not started; it starts with the next {@link #start()}.
	 */
	@Override
	public void stop() throws InterruptedException
	{
		for (ThreadBlock<?> t : threads) t.stop();
		for (PerKeyCopies<?, ?> m : maps) m.stop();
	}

	/** True while any of the pipeline's thread blocks runs, its maps' per-key copies' included. */
	@Override
	public boolean isRunning()
	{
		for (ThreadBlock<?> t : threads)
		{
			if (t.isRunning()) return true;
		}
		for (PerKeyCopies<?, ?> m : maps)
		{
			if (m.isRunning()) return true;
		}
		return false;
	}
}
