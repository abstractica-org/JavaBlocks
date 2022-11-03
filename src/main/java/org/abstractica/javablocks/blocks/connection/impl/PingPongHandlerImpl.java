package org.abstractica.javablocks.blocks.connection.impl;

import org.abstractica.javablocks.blocks.basic.Output;
import org.abstractica.javablocks.blocks.basic.impl.AbstractBlock;
import org.abstractica.javablocks.blocks.connection.PingPongProtocol;

public class PingPongHandlerImpl<Key, E, Handler extends Output<E>>
		extends AbstractBlock
		implements PingPongHandler<Key, E, Handler>
{
	private final Key key;
	private final PingPongProtocol<Key, E, Handler> protocol;
	private final Output<E> pongSender;
	private final Handler msgHandler;

	private long lastMessageReceivedTime;
	private long lastPingSentTime;

	public PingPongHandlerImpl( Key key,
								PingPongProtocol<Key, E, Handler> protocol,
								Output<E> pongSender,
								Handler msgHandler )
	{
		this.key = key;
		this.protocol = protocol;
		this.pongSender = pongSender;
		this.msgHandler = msgHandler;
		this.lastMessageReceivedTime = -1;
		this.lastPingSentTime = -1;
	}

	@Override
	public long getLastPingSentTime()
	{
		return lastPingSentTime;
	}

	@Override
	public void setLastPingSentTime(long lastPingSentTime)
	{
		this.lastPingSentTime = lastPingSentTime;
	}


	@Override
	public void put(E msg) throws Exception
	{
		//Incoming message
		lastMessageReceivedTime = System.currentTimeMillis();
		if(protocol.isPong(msg))
		{
			DEBUG(key + ": Pong received!");
			return;
		}
		if(protocol.isPing(msg))
		{
			DEBUG(key + ": Ping received, sending Pong!");
			pongSender.put(protocol.getPong(key));
			return;
		}
		msgHandler.put(msg);
	}

	@Override
	public long getLastMessageReceivedTime()
	{
		return lastMessageReceivedTime;
	}

	@Override
	public Handler getMsgHandler()
	{
		return msgHandler;
	}

	@Override
	public Key getKey()
	{
		return key;
	}
}
