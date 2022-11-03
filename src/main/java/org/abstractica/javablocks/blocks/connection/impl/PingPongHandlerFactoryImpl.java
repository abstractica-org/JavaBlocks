package org.abstractica.javablocks.blocks.connection.impl;

import org.abstractica.javablocks.blocks.basic.MapHandlerBlockFactory;
import org.abstractica.javablocks.blocks.basic.Output;
import org.abstractica.javablocks.blocks.connection.PingPongProtocol;

public class PingPongHandlerFactoryImpl<Key, E, Handler extends Output<E>>
		implements MapHandlerBlockFactory<Key, E, PingPongHandler<Key, E, Handler>>
{
	private final PingPongProtocol<Key, E, Handler> protocol;
	private final Output<E> pongSender;


	public PingPongHandlerFactoryImpl(PingPongProtocol<Key, E, Handler> protocol, Output<E> pongSender)
	{
		this.protocol = protocol;
		this.pongSender = pongSender;
	}

	@Override
	public Key extractKey(E msg)
	{
		return protocol.extractKey(msg);
	}

	@Override
	public PingPongHandler<Key, E, Handler> createHandlerBlockFor(Key key)
	{
		return new PingPongHandlerImpl<Key, E, Handler>(key, protocol, pongSender, protocol.createHandlerFor(key));
	}

	@Override
	public void destroyHandlerBlock(PingPongHandler<Key, E, Handler> pingPongHandler)
	{
		protocol.destroyHandler(pingPongHandler.getMsgHandler());
	}
}
