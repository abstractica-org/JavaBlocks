package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.Input;

/** The program's keyboard: each pull returns a line typed. One shared object per program, so it has no reporter ports. */
public final class KeyboardNode extends NodeHandle<Input<String>>
{
	KeyboardNode(String id)
	{
		super(id);
	}

	/** Pull typed lines from here. */
	public PullOut<String> out()
	{
		return pullOut("out");
	}
}
