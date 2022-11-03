package org.abstractica.javablocks.blocks.connection;

import org.abstractica.javablocks.blocks.basic.Block;
import org.abstractica.javablocks.blocks.basic.Output;

public interface PingPongBlock<E> extends Block, Output<E>
{
	//Must input the Ping frequency
	Output<Long> getTimerConnector();
}
