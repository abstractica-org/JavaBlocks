package org.abstractica.javablocks.blocks.basic;

import org.abstractica.javablocks.blocks.basic.impl.DelayFunctionImpl;
import org.abstractica.javablocks.blocks.basic.impl.RandomDelayFunctionImpl;

import java.util.function.Function;

public class BasicFunctions
{
	public static <E> Function<E, E> getRandomDelayFunction(int minDelay, int maxDelay)
	{
		return new RandomDelayFunctionImpl<>(minDelay, maxDelay);
	}

	public static <E> Function<E, E> DelayFunction(long delay)
	{
		return new DelayFunctionImpl<>(delay);
	}
}
