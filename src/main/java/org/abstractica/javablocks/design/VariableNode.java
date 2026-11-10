package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.VariableBlock;

/** A variable: a push replaces the value, a pull returns the latest. */
public final class VariableNode<E> extends NodeHandle<VariableBlock<E>>
{
	VariableNode(String id)
	{
		super(id);
	}

	/** Push a new value in here. */
	public PushIn<E> in()
	{
		return pushIn("in");
	}

	/** Pull the latest value from here. */
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
