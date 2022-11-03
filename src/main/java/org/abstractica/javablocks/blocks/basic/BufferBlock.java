package org.abstractica.javablocks.blocks.basic;

public interface BufferBlock<E> extends Block, Output<E>, Input<E>
{
    int getCapacity();
    int getLoad();
}
