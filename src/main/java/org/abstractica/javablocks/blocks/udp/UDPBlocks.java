package org.abstractica.javablocks.blocks.udp;
import org.abstractica.javablocks.blocks.udp.impl.UDPSocketBlockImpl;

import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPBlocks
{
	public static UDPSocketBlock<DatagramPacket> getUDPSocketBlock(int port, int maxPacketSize)
			throws SocketException, UnknownHostException
	{
		return new UDPSocketBlockImpl(port, maxPacketSize);
	}
}
