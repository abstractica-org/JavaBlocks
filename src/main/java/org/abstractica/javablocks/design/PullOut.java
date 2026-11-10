package org.abstractica.javablocks.design;

/**
 * A connection point that is an Input: a pull wire starts here, e.g. a buffer's {@code out()}.
 *
 * @param <E> the type of the items that pass through it
 */
public final class PullOut<E>
{
	private final Design.Port port;

	PullOut(Design.Port port)
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
