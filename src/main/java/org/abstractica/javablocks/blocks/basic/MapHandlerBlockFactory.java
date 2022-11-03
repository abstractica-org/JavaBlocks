package org.abstractica.javablocks.blocks.basic;

public interface MapHandlerBlockFactory<Key, E, Handler extends MapHandlerBlock<Key, E>>
{
	Key extractKey(E item);
	Handler createHandlerBlockFor(Key key);
	void destroyHandlerBlock(Handler handler);
}
