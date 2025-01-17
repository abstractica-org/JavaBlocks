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
	public synchronized E get() throws InterruptedException
	{
		return value;
	}

	@Override
	public synchronized void put(E value) throws InterruptedException
	{
		this.value = value;
	}
}
