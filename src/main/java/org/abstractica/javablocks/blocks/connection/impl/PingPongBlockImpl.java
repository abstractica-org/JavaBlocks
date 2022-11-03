package org.abstractica.javablocks.blocks.connection.impl;

import org.abstractica.javablocks.blocks.basic.BasicBlocks;
import org.abstractica.javablocks.blocks.basic.MapBlock;

import org.abstractica.javablocks.blocks.basic.MapHandlerBlockFactory;
import org.abstractica.javablocks.blocks.basic.Output;
import org.abstractica.javablocks.blocks.basic.impl.AbstractBlock;
import org.abstractica.javablocks.blocks.connection.PingPongBlock;
import org.abstractica.javablocks.blocks.connection.PingPongProtocol;

public class PingPongBlockImpl<Key, E, Handler extends Output<E>> extends AbstractBlock implements PingPongBlock<E>
{
	private TimerConnector timerConnector;
	private MapBlock<Key, E, PingPongHandler<Key, E, Handler>> mapBlock;
	private PingPongProtocol<Key, E, Handler> protocol;
	private Output<E> msgSender;

	public PingPongBlockImpl(PingPongProtocol<Key, E, Handler> protocol,
	                         Output<E> msgSender)
	{
		this.timerConnector = new TimerConnector();
		this.protocol = protocol;
		this.msgSender = msgSender;
		MapHandlerBlockFactory<Key, E, PingPongHandler<Key, E, Handler>> factory =
				new PingPongHandlerFactoryImpl<>(protocol, msgSender);
		mapBlock = BasicBlocks.getMapBlock(factory);
	}


	@Override
	public void put(E item) throws Exception
	{
		mapBlock.put(item);
	}

	@Override
	public Output<Long> getTimerConnector()
	{
		return timerConnector;
	}

	private class TimerConnector implements Output<Long>
	{
		@Override
		public void put(Long timeout) throws Exception
		{
			long curTime = System.currentTimeMillis();
			Iterable<PingPongHandler<Key, E, Handler>> handlers = mapBlock.getAllHandlers();
			for(PingPongHandler<Key, E, Handler> handler : handlers)
			{
				long sinceLastPingSentTime = curTime - handler.getLastPingSentTime();
				if(sinceLastPingSentTime > timeout)
				{
					long sinceLastMessageReceivedTime = curTime - handler.getLastMessageReceivedTime();
					if (sinceLastMessageReceivedTime > timeout)
					{
						msgSender.put(protocol.getPing(handler.getKey()));
						handler.setLastPingSentTime(curTime);
					}
				}
			}
		}
	}
}
