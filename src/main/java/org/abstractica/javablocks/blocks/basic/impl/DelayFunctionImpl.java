package org.abstractica.javablocks.blocks.basic.impl;

import java.util.Random;
import java.util.function.Function;

public class DelayFunctionImpl<E> implements Function<E, E>
{
    private final long delayMillis;

    public DelayFunctionImpl(long delayMillis)
    {
        this.delayMillis = delayMillis;
    }

    @Override
    public E apply(E item)
    {
        long start = System.currentTimeMillis();
        long elapsed = 0;
        while(elapsed < delayMillis)
        {
            try
            {
                Thread.sleep(delayMillis-elapsed);
            } catch (InterruptedException e) {}
            elapsed = System.currentTimeMillis() - start;
        }
        return item;
    }
}
