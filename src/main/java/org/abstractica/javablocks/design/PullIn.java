package org.abstractica.javablocks.design;

/**
 * A connection point that holds an Input: a pull wire ends here, e.g. a thread's {@code in()}.
 *
 * @param <E> the type of the items that pass through it
 */
public final class PullIn<E>
{
	private final Design.Port port;

	PullIn(Design.Port port)
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
