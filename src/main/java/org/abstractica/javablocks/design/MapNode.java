package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.MapBlock;

/**
 * A map: sorts the items pushed in by key, and builds a copy of its nested Design for each new key.
 * Built, it is a {@link MapBlock}; its per-key copies are reached through
 * {@link MapBlock#getAllHandlers()} and removed with {@link MapBlock#removeHandler}.
 */
public final class MapNode<K, E> extends NodeHandle<MapBlock<K, E, ?>>
{
	MapNode(String id)
	{
		super(id);
	}

	/** Push items in here. */
	public PushIn<E> in()
	{
		return pushIn("in");
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
