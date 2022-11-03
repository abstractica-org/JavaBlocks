package org.abstractica.javablocks.blocks.basic.impl;



import org.abstractica.javablocks.blocks.basic.DistributorBlock;
import org.abstractica.javablocks.blocks.basic.Output;

import java.util.ArrayList;
import java.util.Collection;

public class DistributorBlockImpl<E> extends AbstractBlock implements DistributorBlock<E>
{
    private final Collection<Output<E>> outputs;

    public DistributorBlockImpl()
    {
        this.outputs = new ArrayList<>();
    }

    @Override
    public synchronized void addOutput(Output<E> output)
    {
        outputs.add(output);
    }

    @Override
    public synchronized boolean removeOutput(Output<E> output)
    {
        return outputs.remove(output);
    }

    @Override
    public synchronized Collection<Output<E>> getOutputs()
    {
        //defensive copy
        return new ArrayList<>(outputs);
    }

    @Override
    public synchronized void put(E item) throws Exception
    {
        for(Output<E> output : outputs)
        {
            output.put(item);
        }
    }
}
