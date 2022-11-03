package org.abstractica.javablocks.blocks.udp;

import org.abstractica.javablocks.blocks.udp.impl.IPAddressFunction;
import org.abstractica.javablocks.blocks.udp.impl.DatagramPackageToStringFunctionImpl;
import org.abstractica.javablocks.blocks.udp.impl.StringToDatagramPacketFunctionImpl;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.function.Function;

public class UDPFunctions
{
	private static final Function<String, DatagramPacket> strToDatagram = new StringToDatagramPacketFunctionImpl();
	private static final Function<DatagramPacket, String> datagramToString = new DatagramPackageToStringFunctionImpl();
	public static Function<DatagramPacket, DatagramPacket> getAddressFunction(InetAddress address, int port)
	{
		return new IPAddressFunction(address, port);
	}

	public static Function<String, DatagramPacket> getStringToDatagramPacketFunction()
	{
		return strToDatagram;
	}

	public static Function<DatagramPacket, String> getDatagramPacketToStringFunction()
	{
		return datagramToString;
	}
}
