package org.abstractica.javablocks.blocks.basic.impl;

import org.abstractica.javablocks.blocks.basic.Output;
import org.abstractica.javablocks.blocks.basic.PushBlock;

import java.util.function.Function;

public class PushBlockImpl<From, To>
        extends AbstractBlock
        implements PushBlock<From, To>
{
    private final Function<From, To> fun;
    private Output<To> output;

    public PushBlockImpl(Function<From, To> fun)
    {
        this.fun = fun;
    }

    @Override
    public void setOutput(Output<To> output)
    {
        this.output = output;
    }

    @Override
    public void put(From item) throws Exception
    {
        DEBUG(" put(" + item + ")");
        if(output == null)
        {
            throw new IllegalStateException("PushBlock's output is null!");
        }
        output.put(fun.apply(item));
    }
}
