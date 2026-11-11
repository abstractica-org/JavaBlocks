package org.abstractica.javablocks.examples.udp;

import org.abstractica.javablocks.blocks.udp.UDPFunctions;
import org.abstractica.javablocks.design.Designer;
import org.abstractica.javablocks.design.PullOut;
import org.abstractica.javablocks.design.PushIn;

import java.net.DatagramPacket;

/** Turns each line typed into a packet and back into text, and prints it. Built as a Design (J18). */
public class SimpleDatagramPacketExample
{
	public static void main(String[] args)
	{
		Designer d = new Designer();
		design(d, d.keyboard("keyboard").out(), d.console("console").in());

		//Start the thread
		d.design().build(d.catalogue()).start();
	}

	/**
	 * Adds the pipeline to {@code d}: a thread pulls each line from {@code lines} as a packet and
	 * pushes the packet's text into {@code console}.
	 */
	static void design(Designer d, PullOut<String> lines, PushIn<String> console)
	{
		var thread = d.<DatagramPacket>thread("thread");
		var stringToDatagramPacket = d.pull("stringToDatagramPacket", UDPFunctions.getStringToDatagramPacketFunction());
		var datagramPacketToString = d.push("datagramPacketToString", UDPFunctions.getDatagramPacketToStringFunction());

		//Hook it up
		d.wire(lines, stringToDatagramPacket.in());
		d.wire(stringToDatagramPacket.out(), thread.in());
		d.wire(thread.out(), datagramPacketToString.in());
		d.wire(datagramPacketToString.out(), console);
	}
}
