package org.abstractica.javablocks.blocks.basic;

public interface MapBlock<Key, E, Handler extends MapHandlerBlock<Key, E>> extends Block, Output<E>
{
	int getSize();
	Handler removeHandler(Key key);
	Key extractKey(E item);
	Iterable<Handler> getAllHandlers();
}
