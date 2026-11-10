package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.DistributorBlock;

/** A distributor: pushes each item to every output wired to {@code out()}. */
public final class DistributorNode<E> extends NodeHandle<DistributorBlock<E>>
{
	DistributorNode(String id)
	{
		super(id);
	}

	/** Push items in here. */
	public PushIn<E> in()
	{
		return pushIn("in");
	}

	/** The one port that takes many wires: each item goes to all of them. */
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
