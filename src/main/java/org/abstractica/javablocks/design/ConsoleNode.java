package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.Output;

/** The program's console: prints each item pushed in. One shared object per program, so it has no reporter ports. */
public final class ConsoleNode extends NodeHandle<Output<String>>
{
	ConsoleNode(String id)
	{
		super(id);
	}

	/** Push lines to print in here. */
	public PushIn<String> in()
	{
		return pushIn("in");
	}
}
