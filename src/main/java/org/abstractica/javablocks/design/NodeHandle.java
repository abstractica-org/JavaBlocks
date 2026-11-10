package org.abstractica.javablocks.design;

/**
 * What a {@link Designer} returns for each node: its id and its typed connection points.
 *
 * @param <B> the type of the object the node becomes when built; see {@link Pipeline#block(NodeHandle)}
 */
public abstract class NodeHandle<B>
{
	private final String id;

	NodeHandle(String id)
	{
		this.id = id;
	}

	public String id()
	{
		return id;
	}

	final <E> PushOut<E> pushOut(String name) { return new PushOut<>(new Design.Port(id, name)); }

	final <E> PushIn<E> pushIn(String name) { return new PushIn<>(new Design.Port(id, name)); }

	final <E> PullOut<E> pullOut(String name) { return new PullOut<>(new Design.Port(id, name)); }

	final <E> PullIn<E> pullIn(String name) { return new PullIn<>(new Design.Port(id, name)); }

	@Override
	public String toString()
	{
		return id;
	}
}
