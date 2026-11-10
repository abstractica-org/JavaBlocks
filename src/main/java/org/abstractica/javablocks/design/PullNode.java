package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.PullBlock;

/** A pull block: when pulled, pulls from its input and returns its function's result. */
public final class PullNode<F, T> extends NodeHandle<PullBlock<F, T>>
{
	PullNode(String id)
	{
		super(id);
	}

	/** Where the block pulls from. Must be wired. */
	public PullIn<F> in()
	{
		return pullIn("in");
	}

	/** Pull results from here. */
	public PullOut<T> out()
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
