package org.abstractica.javablocks.blocks.basic;

public interface MapHandlerBlock<Key, E> extends Block, Output<E>
{
	Key getKey();
}
