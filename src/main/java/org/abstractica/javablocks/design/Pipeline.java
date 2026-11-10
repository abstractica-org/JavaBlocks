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

	Pipeline(Map<String, Object> blocks)
	{
		this.blocks = Collections.unmodifiableMap(blocks);
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

	/** Starts every thread block, in the Design's node order. */
	@Override
	public void start()
	{
		for (ThreadBlock<?> t : threads) t.start();
	}

	/** Stops every thread block, in the Design's node order; each stop waits as {@link ThreadBlock#stop()} does. */
	@Override
	public void stop() throws InterruptedException
	{
		for (ThreadBlock<?> t : threads) t.stop();
	}

	/** True while any of the pipeline's thread blocks runs. */
	@Override
	public boolean isRunning()
	{
		for (ThreadBlock<?> t : threads)
		{
			if (t.isRunning()) return true;
		}
		return false;
	}
}
