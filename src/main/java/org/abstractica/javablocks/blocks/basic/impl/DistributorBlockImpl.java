package org.abstractica.javablocks.blocks.basic.impl;



import org.abstractica.javablocks.blocks.basic.DistributorBlock;
import org.abstractica.javablocks.blocks.basic.Output;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

public class DistributorBlockImpl<E> extends AbstractBlock implements DistributorBlock<E>
{
    private final Collection<Output<E>> outputs;

    public DistributorBlockImpl()
    {
        this.outputs = new CopyOnWriteArrayList<>();
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

    // Not synchronized: forwards from the list's snapshot, so a slow output never holds the lock (J8).
    @Override
    public void put(E item) throws InterruptedException
    {
        for(Output<E> output : outputs)
        {
            output.put(item);
        }
    }
}
