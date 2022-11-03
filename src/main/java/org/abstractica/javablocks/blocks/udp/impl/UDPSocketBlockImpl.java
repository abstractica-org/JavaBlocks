package org.abstractica.javablocks.blocks.udp.impl;

import org.abstractica.javablocks.blocks.basic.impl.AbstractBlock;
import org.abstractica.javablocks.blocks.udp.UDPSocketBlock;

import java.io.IOException;
import java.net.*;

public class UDPSocketBlockImpl extends AbstractBlock
        implements UDPSocketBlock<DatagramPacket>
{
    private final DatagramSocket socket;
    private final int maxPacketSize;
    private final InetAddress address;

    public UDPSocketBlockImpl(int port, int maxPacketSize) throws SocketException, UnknownHostException
    {
        this.socket = new DatagramSocket(port);
        this.maxPacketSize = maxPacketSize;
        this.address = InetAddress.getLocalHost();
    }

    @Override
    public int getPort()
    {
        return socket.getPort();
    }

    @Override
    public InetAddress getAddress()
    {
        return address;
    }

    @Override
    public void close()
    {
        socket.close();
    }

    @Override
    public DatagramPacket get() throws Exception
    {
        byte[] buffer = new byte[maxPacketSize];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }

    @Override
    public void put(DatagramPacket item) throws IOException
    {
        socket.send(item);
    }
}
