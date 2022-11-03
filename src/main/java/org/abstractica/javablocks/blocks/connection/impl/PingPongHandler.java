package org.abstractica.javablocks.blocks.connection.impl;

import org.abstractica.javablocks.blocks.basic.MapHandlerBlock;
import org.abstractica.javablocks.blocks.basic.Output;


public interface PingPongHandler<Key, E, Handler extends Output<E>> extends MapHandlerBlock<Key, E>
{
	long getLastMessageReceivedTime();
	long getLastPingSentTime();
	void setLastPingSentTime(long lastPingSentTime);
	Handler getMsgHandler();
}
