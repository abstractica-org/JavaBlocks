package org.abstractica.javablocks.blocks.basic.impl;

import org.abstractica.javablocks.blocks.basic.VariableBlock;

public class VariableBlockImpl<E> extends AbstractBlock implements VariableBlock<E>
{
	private E value;

	public VariableBlockImpl(E value)
	{
		this.value = value;
	}

	@Override
	public synchronized E get() throws Exception
	{
		return value;
	}

	@Override
	public synchronized void put(E value) throws Exception
	{
		this.value = value;
	}
}
