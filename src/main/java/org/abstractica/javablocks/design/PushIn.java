package org.abstractica.javablocks.design;

/**
 * A connection point that is an Output: a push wire ends here, e.g. a push block's {@code in()}.
 *
 * @param <E> the type of the items that pass through it
 */
public final class PushIn<E>
{
	private final Design.Port port;

	PushIn(Design.Port port)
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
