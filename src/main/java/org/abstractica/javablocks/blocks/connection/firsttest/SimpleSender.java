package org.abstractica.javablocks.blocks.connection.firsttest;

import org.abstractica.javablocks.blocks.basic.Output;

public class SimpleSender implements Output<String>
{
	@Override
	public void put(String item) throws Exception
	{
		System.out.println("Sender is sending -> " + item);
	}
}
