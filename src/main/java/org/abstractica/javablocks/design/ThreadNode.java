package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.ThreadBlock;

/** A thread block: the only owner of a thread; it pulls from {@code in()} and pushes to {@code out()}. */
public final class ThreadNode<E> extends NodeHandle<ThreadBlock<E>>
{
	ThreadNode(String id)
	{
		super(id);
	}

	/** Where the thread pulls from. Must be wired. */
	public PullIn<E> in()
	{
		return pullIn("in");
	}

	/** Where the thread pushes to. Must be wired. */
	public PushOut<E> out()
	{
		return pushOut("out");
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
