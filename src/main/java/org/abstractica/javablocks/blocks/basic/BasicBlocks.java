package org.abstractica.javablocks.blocks.basic;

import org.abstractica.javablocks.blocks.basic.impl.*;

import java.util.function.Function;
import java.util.function.Predicate;

public class BasicBlocks
{
	public static <E> ConstantBlock<E> getConstantBlock(E value)
	{
		return new ConstantBlockImpl<E>(value);
	}

	public static <E> VariableBlock<E> getVariableBlock(E value)
	{
		return new VariableBlockImpl<>(value);
	}

	public static <E> IfBlock<E> getIfBlock(Predicate<E> predicate)
	{
		return new IfBlockImpl<>(predicate);
	}

	public static <E> BufferBlock<E> getBufferBlock(int capacity)
	{
		if(capacity == 1)
		{
			return new SingleBufferBlockImpl<>();
		}
		return new BufferBlockImpl<E>(capacity);
	}

	public static <E> ThreadBlock<E> getThreadBlock()
	{
		return new ThreadBlockImpl<>();
	}

	public static <E> DistributorBlock<E> getDistributorBlock()
	{
		return new DistributorBlockImpl<>();
	}

	public static <From, To> PullBlock<From, To> getPullBlock(Function<From, To> fun)
	{
		return new PullBlockImpl<>(fun);
	}

	public static <From, To> PushBlock<From, To> getPushBlock(Function<From, To> fun)
	{
		return new PushBlockImpl<>(fun);
	}

	public static <Key, E, Handler extends MapHandlerBlock<Key, E>> MapBlock<Key, E, Handler>
			getMapBlock(MapHandlerBlockFactory<Key, E, Handler> handlerFactory)
	{
		return new MapBlockImpl<>(handlerFactory);
	}

	public static <E> Output<E> getTrashcanBlock()
	{
		return new Output<E>()
		{
			@Override
			public void put(E item){}
		};
	}

	public static Input<String> getKeyboardBlock()
	{
		return KeyboardBlockImpl.getInstance();
	}

	public static  Output<String> getConsoleBlock()
	{
		return ConsoleBlockImpl.getInstance();
	}
}
