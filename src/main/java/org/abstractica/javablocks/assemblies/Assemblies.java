package org.abstractica.javablocks.assemblies;

import org.abstractica.javablocks.assemblies.impl.BufferedSocketBlockImpl;

import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.function.Function;

public class Assemblies
{
	public static <E> BufferedSocketBlock<E> getBufferedSocketBlock
			(
				int port,
				int maxPacketSize,
				int bufferSize,
				Function<DatagramPacket, E> fromDatagramPacket,
				Function<E, DatagramPacket> toDatagramPacket
			) throws SocketException, UnknownHostException
	{
		return new BufferedSocketBlockImpl
				(
						port,
						maxPacketSize,
						bufferSize,
						fromDatagramPacket,
						toDatagramPacket);
	}
}
