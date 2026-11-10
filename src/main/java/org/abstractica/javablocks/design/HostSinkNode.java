package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.Output;

/** A host's own Output object, something the pipeline pushes into. */
public final class HostSinkNode<E> extends NodeHandle<Output<E>>
{
	HostSinkNode(String id)
	{
		super(id);
	}

	/** Push items in here. */
	public PushIn<E> in()
	{
		return pushIn("in");
	}
}
