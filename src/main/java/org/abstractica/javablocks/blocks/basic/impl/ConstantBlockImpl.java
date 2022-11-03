package org.abstractica.javablocks.blocks.basic.impl;

import org.abstractica.javablocks.blocks.basic.ConstantBlock;

public class ConstantBlockImpl<E> extends AbstractBlock implements ConstantBlock<E>
{
	private final E value;

	public ConstantBlockImpl(E value)
	{
		this.value = value;
	}

	@Override
	public E get() throws Exception
	{
		return value;
	}
}
