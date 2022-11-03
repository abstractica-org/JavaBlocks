package org.abstractica.javablocks.blocks.connection.firsttest;

import org.abstractica.javablocks.blocks.connection.PingPongProtocol;

public class SimplePingPongProtokol implements PingPongProtocol<String,String, SimpleHandler>
{
	@Override
	public boolean isPing(String msg)
	{
		return "PING".equals(getMsg(msg));
	}

	@Override
	public boolean isPong(String msg)
	{
		return "PONG".equals(getMsg(msg));
	}

	@Override
	public String getPong(String key)
	{
		return key + ":PONG";
	}

	@Override
	public String getPing(String key)
	{
		return key + ":PING";
	}

	@Override
	public String extractKey(String msg)
	{
		return getKey(msg);
	}

	@Override
	public SimpleHandler createHandlerFor(String key)
	{
		return new SimpleHandler(key);
	}

	@Override
	public void destroyHandler(SimpleHandler simpleHandler)
	{

	}

	private String getKey(String msg)
	{
		return msg.split(":")[0];
	}

	private String getMsg(String msg)
	{
		return msg.split(":")[1];
	}
}
