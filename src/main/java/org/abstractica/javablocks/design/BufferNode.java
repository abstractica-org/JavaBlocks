package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.BufferBlock;

/** A buffer: push in, pull out; the only place back-pressure lives. */
public final class BufferNode<E> extends NodeHandle<BufferBlock<E>>
{
	BufferNode(String id)
	{
		super(id);
	}

	/** Push items in here; blocks while full. */
	public PushIn<E> in()
	{
		return pushIn("in");
	}

	/** Pull items from here; blocks while empty. */
	public PullOut<E> out()
	{
		return pullOut("out");
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
