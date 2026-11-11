package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.ThreadBlock;
import org.abstractica.javablocks.blocks.basic.ThreadControl;
import org.abstractica.javablocks.blocks.udp.UDPSocketBlock;

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
	/** The udpSocket nodes' sockets: opened at build, closed by {@link #stop()}. Never a host's own socket. */
	private final List<UDPSocketBlock<?>> sockets;
	/** True once {@link #stop()} has closed the sockets; a closed socket cannot be opened again. */
	private volatile boolean socketsClosed;

	Pipeline(Map<String, Object> blocks, List<PerKeyCopies<?, ?>> maps, List<UDPSocketBlock<?>> sockets)
	{
		this.sockets = List.copyOf(sockets);
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
	 *
	 * @throws IllegalStateException if an earlier {@link #stop()} closed a socket of this pipeline, or
	 *                               of a live per-key copy: a closed socket cannot be opened again, so
	 *                               nothing is started. Build the Design again; for a copy's socket,
	 *                               removing that copy's handler from its map is enough.
	 *                               The check is not locked against a {@code stop()} running at the same
	 *                               time: do not start and stop a pipeline from two threads at once.
	 */
	@Override
	public void start()
	{
		if (hasClosedSockets())
		{
			throw new IllegalStateException("stop() closed this pipeline's sockets, and a closed socket cannot be"
					+ " opened again: build the Design again");
		}
		for (PerKeyCopies<?, ?> m : maps) m.start();
		for (ThreadBlock<?> t : threads) t.start();
	}

	/** True when stop() has closed a socket of this pipeline or of a live per-key copy. */
	boolean hasClosedSockets()
	{
		if (socketsClosed) return true;
		for (PerKeyCopies<?, ?> m : maps)
		{
			if (m.hasClosedSockets()) return true;
		}
		return false;
	}

	/**
	 * Stops every thread block, in the Design's node order; each stop waits as {@link ThreadBlock#stop()}
	 * does, and one that never returns holds up the rest. A thread pulling from a udpSocket node stops
	 * within about 100 ms.
	 *
	 * <p>Then stops every map's live per-key copies, once the threads that feed them have stopped. A copy
	 * made while the pipeline is stopped is built but not started; it starts with the next {@link #start()}.
	 *
	 * <p>Last, closes the udpSocket nodes' sockets, also when the pipeline was never started: they were
	 * opened when it was built. A pipeline with sockets cannot be started again after this. A socket the
	 * host made itself and wired in as a host source or sink is the host's: it is never closed here.
	 *
	 * <p>If this thread is interrupted while it waits, none of this pipeline's own sockets is closed
	 * (per-key copies already stopped have closed theirs); call {@code stop()} again. Call it again, too,
	 * after a key that arrived once the pipeline was stopped: its copy is built with its socket open.
	 */
	@Override
	public void stop() throws InterruptedException
	{
		for (ThreadBlock<?> t : threads) t.stop();
		for (PerKeyCopies<?, ?> m : maps) m.stop();
		if (!sockets.isEmpty())
		{
			socketsClosed = true;
			for (UDPSocketBlock<?> s : sockets) s.close();
		}
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
