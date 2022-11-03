package org.abstractica.javablocks.blocks.basic;

public interface ThreadBlock<E> extends Block, ThreadControl
{
    public void setInput(Input<E> input) throws IllegalStateException;
    public void setOutput(Output<E> output) throws IllegalStateException;
}
