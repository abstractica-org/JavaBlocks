package org.abstractica.javablocks.blocks.basic;

public interface PushBlock<From, To> extends Block, Output<From>
{
    public void setOutput(Output<To> output);
}
