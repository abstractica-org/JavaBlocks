package org.abstractica.javablocks.blocks.udp.impl;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.function.Function;

public class IPAddressFunction implements Function<DatagramPacket, DatagramPacket>
{
    private final InetAddress iaddr;
    private final int port;

    public IPAddressFunction(InetAddress iaddr, int port)
    {
        this.iaddr = iaddr;
        this.port = port;
    }

    @Override
    public DatagramPacket apply(DatagramPacket packet)
    {
        packet.setAddress(iaddr);
        packet.setPort(port);
        return packet;
    }
}
