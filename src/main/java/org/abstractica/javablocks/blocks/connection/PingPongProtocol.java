package org.abstractica.javablocks.blocks.connection;

import org.abstractica.javablocks.blocks.basic.Output;

public interface PingPongProtocol<Key, E, Handler extends Output<E>>
{
	boolean isPing(E msg);
	boolean isPong(E msg);
	E getPong(Key key);
	E getPing(Key key);
	Key extractKey(E msg);
	//Handler factory
	Handler createHandlerFor(Key key);
	void destroyHandler(Handler handler);
}
