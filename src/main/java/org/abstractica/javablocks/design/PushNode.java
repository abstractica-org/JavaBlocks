package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.PushBlock;

/** A push block: applies its function to each item pushed in and pushes the result on. */
public final class PushNode<F, T> extends NodeHandle<PushBlock<F, T>>
{
	PushNode(String id)
	{
		super(id);
	}

	/** Push items in here. */
	public PushIn<F> in()
	{
		return pushIn("in");
	}

	/** Where results are pushed. Must be wired. */
	public PushOut<T> out()
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
