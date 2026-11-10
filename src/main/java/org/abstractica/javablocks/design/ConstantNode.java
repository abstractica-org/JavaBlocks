package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.ConstantBlock;

/** A constant: every pull returns the same value. */
public final class ConstantNode<E> extends NodeHandle<ConstantBlock<E>>
{
	ConstantNode(String id)
	{
		super(id);
	}

	/** Pull the value from here. */
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
