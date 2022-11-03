package org.abstractica.javablocks.blocks.basic.impl;

import org.abstractica.javablocks.blocks.basic.Input;
import org.abstractica.javablocks.blocks.basic.PullBlock;

import java.util.function.Function;

public class PullBlockImpl<From, To>
        extends AbstractBlock
        implements PullBlock<From, To>
{
    private final Function<From, To> fun;
    private Input<From> input;

    public PullBlockImpl(Function<From, To> fun)
    {
        this.fun = fun;
    }

    @Override
    public void setInput(Input<From> input)
    {
        this.input = input;
    }

    @Override
    public To get() throws Exception
    {
        if(input == null)
        {
            throw new IllegalStateException("PullBlock's input is null!");
        }
        return fun.apply(input.get());
    }
}
