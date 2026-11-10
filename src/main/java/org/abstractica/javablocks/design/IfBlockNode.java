package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.IfBlock;

/** An if block: pushes each item to {@code whenTrue()} or {@code whenFalse()} by its predicate. */
public final class IfBlockNode<E> extends NodeHandle<IfBlock<E>>
{
	IfBlockNode(String id)
	{
		super(id);
	}

	/** Push items in here. */
	public PushIn<E> in()
	{
		return pushIn("in");
	}

	/** Where items the predicate accepts go. Must be wired. */
	public PushOut<E> whenTrue()
	{
		return pushOut("whenTrue");
	}

	/** Where the other items go. Must be wired. */
	public PushOut<E> whenFalse()
	{
		return pushOut("whenFalse");
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
