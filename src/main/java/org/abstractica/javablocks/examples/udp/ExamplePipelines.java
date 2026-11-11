package org.abstractica.javablocks.examples.udp;

import org.abstractica.javablocks.design.Designer;
import org.abstractica.javablocks.design.Pipeline;

import java.io.UncheckedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;

final class ExamplePipelines
{
	private ExamplePipelines() {}

	/**
	 * Builds what {@code d} holds. A socket that cannot be opened is thrown as the checked exception
	 * the examples have always declared, not the builder's {@link UncheckedIOException}.
	 */
	static Pipeline build(Designer d) throws SocketException, UnknownHostException
	{
		try
		{
			return d.design().build(d.catalogue());
		} catch (UncheckedIOException e)
		{
			if (e.getCause() instanceof SocketException socket) throw socket;
			if (e.getCause() instanceof UnknownHostException host) throw host;
			throw e;
		}
	}
}
