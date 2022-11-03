package org.abstractica.javablocks.blocks.basic;

public interface PullBlock<From, To> extends Block, Input<To>
{
    public void setInput(Input<From> input);
}
