package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.Output;

/** A trashcan: drops every item pushed in. */
public final class TrashcanNode<E> extends NodeHandle<Output<E>>
{
	TrashcanNode(String id)
	{
		super(id);
	}

	/** Push items in here. */
	public PushIn<E> in()
	{
		return pushIn("in");
	}
}
