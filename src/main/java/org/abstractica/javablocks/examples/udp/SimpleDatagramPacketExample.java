package org.abstractica.javablocks.examples.udp;

import org.abstractica.javablocks.blocks.basic.*;
import org.abstractica.javablocks.blocks.udp.UDPFunctions;

import java.net.DatagramPacket;

public class SimpleDatagramPacketExample
{
	public static void main(String[] args)
	{
		Input<String> keyboard = BasicBlocks.getKeyboardBlock();
		Output<String> console = BasicBlocks.getConsoleBlock();
		ThreadBlock thread = BasicBlocks.getThreadBlock();
		PullBlock<String, DatagramPacket> stringToDatagramPacket = BasicBlocks.getPullBlock(UDPFunctions.getStringToDatagramPacketFunction());
		PushBlock<DatagramPacket, String> datagramPacketToString = BasicBlocks.getPushBlock(UDPFunctions.getDatagramPacketToStringFunction());

		//Hook it up
		stringToDatagramPacket.setInput(keyboard);
		thread.setInput(stringToDatagramPacket);
		thread.setOutput(datagramPacketToString);
		datagramPacketToString.setOutput(console);

		//Start the thread
		thread.start();
	}
}
