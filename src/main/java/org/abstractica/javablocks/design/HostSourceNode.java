package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.Input;

/** A host's own Input object, something the pipeline pulls from. */
public final class HostSourceNode<E> extends NodeHandle<Input<E>>
{
	HostSourceNode(String id)
	{
		super(id);
	}

	/** Pull items from here. */
	public PullOut<E> out()
	{
		return pullOut("out");
	}
}
