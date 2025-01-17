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
    public DatagramPacket get() throws InterruptedException
    {
        byte[] buffer = new byte[maxPacketSize];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try
        {
            socket.receive(packet);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        return packet;
    }

    @Override
    public void put(DatagramPacket item) throws InterruptedException
    {
        try
        {
            socket.send(item);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
