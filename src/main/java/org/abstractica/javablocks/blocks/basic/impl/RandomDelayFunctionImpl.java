package org.abstractica.javablocks.blocks.basic.impl;

import java.util.Random;
import java.util.function.Function;

public class RandomDelayFunctionImpl<E> implements Function<E, E>
{
    private final Random rnd = new Random();
    private final long minTime;
    private final long maxTime;

    public RandomDelayFunctionImpl(long minTime, long maxTime)
    {
        this.minTime = minTime;
        this.maxTime = maxTime;
    }

    @Override
    public E apply(E item)
    {
        long start = System.currentTimeMillis();
        long delayMillis = rnd.nextLong(maxTime - minTime) + minTime;
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
