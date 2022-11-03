package org.abstractica.javablocks.blocks.connection.firsttest;

import org.abstractica.javablocks.blocks.basic.Output;

public class SimpleHandler implements Output<String>
{
	private final String key;

	public SimpleHandler(String key)
	{
		this.key = key;
	}

	@Override
	public void put(String item) throws Exception
	{
		System.out.println("Handler for " + key + " is handeling -> " + item);
	}
}
