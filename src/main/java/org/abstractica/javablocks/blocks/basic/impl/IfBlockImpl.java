package org.abstractica.javablocks.blocks.basic.impl;


import org.abstractica.javablocks.blocks.basic.IfBlock;
import org.abstractica.javablocks.blocks.basic.Output;

import java.util.function.Predicate;

public class IfBlockImpl<E>  extends AbstractBlock implements IfBlock<E>
{
    private Predicate<E> predicate;
    private Output<E> trueOut;
    private Output<E> falseOut;

    public IfBlockImpl(Predicate<E> predicate)
    {
        this.predicate = predicate;
    }

    @Override
    public void setTrueOutput(Output<E> output)
    {
        this.trueOut = output;
    }

    @Override
    public void setFalseOutput(Output<E> output)
    {
        this.falseOut = output;
    }

    @Override
    public void put(E item) throws Exception
    {
        if(trueOut == null || falseOut == null)
        {
            throw new RuntimeException("Block not fully connected!");
        }
        if(predicate.test(item))
        {
            trueOut.put(item);
        }
        else
        {
            falseOut.put(item);
        }
    }
}
