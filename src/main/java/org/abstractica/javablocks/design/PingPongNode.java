package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.connection.PingPongBlock;

/** A ping-pong block: answers pings, keeps each peer alive with pings, drops silent peers, and passes other messages to a handler per peer. Building creates whatever {@code send()} is wired to first, since the block takes its sender in its constructor. */
public final class PingPongNode<E> extends NodeHandle<PingPongBlock<E>>
{
	PingPongNode(String id)
	{
		super(id);
	}

	/** Push received messages in here. */
	public PushIn<E> in()
	{
		return pushIn("in");
	}

	/** Push the timeout, in milliseconds, here on every tick. */
	public PushIn<Long> timer()
	{
		return pushIn("timer");
	}

	/** Where pings and pongs are sent. Must be wired. */
	public PushOut<E> send()
	{
		return pushOut("send");
	}

	/** Where this block's info messages go; left unwired, the library's default info reporter. */
	public PushOut<String> info()
	{
		return pushOut("info");
	}

	/** Where this block's debug messages go; left unwired, the library's default debug reporter. */
	public PushOut<String> debug()
	{
		return pushOut("debug");
	}

	/** Where this block's error messages go; left unwired, the library's default error reporter. */
	public PushOut<String> error()
	{
		return pushOut("error");
	}
}
