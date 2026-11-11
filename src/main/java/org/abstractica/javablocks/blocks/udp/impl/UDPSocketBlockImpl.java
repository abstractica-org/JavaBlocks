package org.abstractica.javablocks.blocks.udp.impl;

import org.abstractica.javablocks.blocks.basic.impl.AbstractBlock;
import org.abstractica.javablocks.blocks.udp.UDPSocketBlock;

import java.io.IOException;
import java.net.*;

public class UDPSocketBlockImpl extends AbstractBlock
        implements UDPSocketBlock<DatagramPacket>
{
    /** How long a get() waits for a packet before it checks whether its thread was interrupted (J16). */
    private static final int INTERRUPT_CHECK_MILLIS = 100;

    private final DatagramSocket socket;
    private final int maxPacketSize;
    private final InetAddress address;

    public UDPSocketBlockImpl(int port, int maxPacketSize) throws SocketException, UnknownHostException
    {
        this.socket = new DatagramSocket(port);
        this.socket.setSoTimeout(INTERRUPT_CHECK_MILLIS);
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

    /**
     * Waits for a packet. An interrupt does not wake a socket's receive, so the wait is cut into
     * {@value #INTERRUPT_CHECK_MILLIS} ms pieces with a check between them: an interrupted get() throws
     * within about that long, and the socket stays open.
     */
    @Override
    public DatagramPacket get() throws InterruptedException
    {
        byte[] buffer = new byte[maxPacketSize];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (true)
        {
            if (Thread.interrupted())
            {
                throw new InterruptedException();
            }
            try
            {
                socket.receive(packet);
                return packet;
            } catch (SocketTimeoutException e)
            {
                // no packet yet: check the interrupt, wait again
            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
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
