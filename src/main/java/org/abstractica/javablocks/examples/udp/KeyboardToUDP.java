package org.abstractica.javablocks.examples.udp;

import org.abstractica.javablocks.blocks.basic.ThreadControl;
import org.abstractica.javablocks.blocks.udp.UDPFunctions;
import org.abstractica.javablocks.design.Designer;
import org.abstractica.javablocks.design.PullOut;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/** Sends each line typed on the keyboard to a receiver as a UDP packet. Built as a Design (J18). */
public class KeyboardToUDP implements ThreadControl
{
    private ThreadControl threadCtrl;

    public KeyboardToUDP(int socketPort, InetAddress receiverAddress, int receiverPort) throws SocketException, UnknownHostException
    {
        Designer d = new Designer();
        design(d, d.keyboard("keyboard").out(), socketPort, receiverAddress, receiverPort);
        threadCtrl = ExamplePipelines.build(d);
    }

    /**
     * Adds the pipeline to {@code d}: a thread pulls each line from {@code lines} through two pull
     * blocks, which make it a packet and address it to the receiver, and pushes it into a socket on
     * {@code socketPort}.
     */
    static void design(Designer d, PullOut<String> lines, int socketPort, InetAddress receiverAddress, int receiverPort)
    {
        var stringToDatagramPacket = d.pull("stringToDatagramPacket", UDPFunctions.getStringToDatagramPacketFunction());
        var addressBlock = d.pull("addressBlock", UDPFunctions.getAddressFunction(receiverAddress, receiverPort));
        var thread = d.<DatagramPacket>thread("thread");
        //Setting maxPacketsize to 0 since we will not read from the socket...
        var socket = d.udpSocket("socket", socketPort, 0);

        d.wire(lines, stringToDatagramPacket.in());
        d.wire(stringToDatagramPacket.out(), addressBlock.in());
        d.wire(addressBlock.out(), thread.in());
        d.wire(thread.out(), socket.in());
    }

    @Override
    public void start()
    {
        threadCtrl.start();
    }

    /** Stops the thread, then closes the socket: a stopped KeyboardToUDP cannot be started again. */
    @Override
    public void stop() throws InterruptedException
    {
        threadCtrl.stop();
    }

    @Override
    public boolean isRunning()
    {
        return threadCtrl.isRunning();
    }
}
