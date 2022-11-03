package org.abstractica.javablocks.blocks.connection.firsttest;

import org.abstractica.javablocks.blocks.basic.*;
import org.abstractica.javablocks.blocks.connection.PingPongBlock;
import org.abstractica.javablocks.blocks.connection.impl.PingPongBlockImpl;

public class FirstTest
{
	public static void main(String[] args)
	{
		PingPongBlock<String> ppBlock =
				new PingPongBlockImpl<String,String, SimpleHandler>
						(new SimplePingPongProtokol(),new SimpleSender());
		Input<String> keyboard = BasicBlocks.getKeyboardBlock();
		ThreadBlock<String> driver = BasicBlocks.getThreadBlock();
		driver.setInput(keyboard);
		driver.setOutput(ppBlock);
		ThreadBlock<Long> timerThread = BasicBlocks.getThreadBlock();
		ConstantBlock<Long> frequency = BasicBlocks.getConstantBlock(5000L);
		PushBlock<Long,Long> delay = BasicBlocks.getPushBlock(BasicFunctions.DelayFunction(10000L));
		timerThread.setInput(frequency);
		timerThread.setOutput(delay);
		delay.setOutput(ppBlock.getTimerConnector());
		timerThread.start();
		driver.start();
	}
}
