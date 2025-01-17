package org.abstractica.javablocks.blocks.basic;

public interface ThreadBlock<E> extends Block, ThreadControl
{
    public void setInput(Input<E> input);
    public void setOutput(Output<E> output);
}
