package org.abstractica.javablocks.blocks.basic;

public interface IfBlock<E> extends Block, Output<E>
{
    public void setTrueOutput(Output<E> output);
    public void setFalseOutput(Output<E> output);
}
