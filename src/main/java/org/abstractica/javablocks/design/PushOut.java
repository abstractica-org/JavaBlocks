package org.abstractica.javablocks.design;

/**
 * A connection point that holds an Output: a push wire starts here, e.g. a thread's {@code out()}.
 *
 * @param <E> the type of the items that pass through it
 */
public final class PushOut<E>
{
	private final Design.Port port;

	PushOut(Design.Port port)
	{
		this.port = port;
	}

	/** Where this connection point is, in the Design's data. */
	public Design.Port port()
	{
		return port;
	}

	@Override
	public String toString()
	{
		return port.toString();
	}
}
