package org.abstractica.javablocks.examples.udp;

import org.abstractica.javablocks.blocks.udp.UDPFunctions;
import org.abstractica.javablocks.design.Designer;
import org.abstractica.javablocks.design.Pipeline;
import org.abstractica.javablocks.design.PullOut;
import org.abstractica.javablocks.design.PushIn;
import org.abstractica.javablocks.design.UdpSocketNode;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/** Sends each line typed to its own socket and prints what the socket receives. Built as a Design (J18). */
public class SimpleUDPExample
{
    public static void main(String[] args) throws UnknownHostException, SocketException
    {
        Designer d = new Designer();
        UdpSocketNode socket = design(d, d.keyboard("keyboard").out(), d.console("console").in(),
                InetAddress.getLocalHost(), 3337);
        Pipeline pipeline = ExamplePipelines.build(d);

        System.out.println("Socket on: " + pipeline.block(socket).getAddress());

        //Start the threads
        pipeline.start();
    }

    /**
     * Adds the pipeline to {@code d}: a reader thread pulls packets from a socket on {@code port} and
     * pushes their text into {@code console}; a writer thread pulls each line from {@code lines} as a
     * packet, addresses it to {@code address} and {@code port} (the socket itself) and pushes it in.
     */
    static UdpSocketNode design(Designer d, PullOut<String> lines, PushIn<String> console, InetAddress address, int port)
    {
        var socket = d.udpSocket("socket", port, 1024);
        var readThread = d.<DatagramPacket>thread("readThread"); // first: it is started first
        var writeThread = d.<DatagramPacket>thread("writeThread");
        var stringToDatagramPacket = d.pull("stringToDatagramPacket", UDPFunctions.getStringToDatagramPacketFunction());
        var datagramPacketToString = d.push("datagramPacketToString", UDPFunctions.getDatagramPacketToStringFunction());
        var addressBlock = d.push("addressBlock", UDPFunctions.getAddressFunction(address, port));

        //Writer
        d.wire(lines, stringToDatagramPacket.in());
        d.wire(stringToDatagramPacket.out(), writeThread.in());
        d.wire(writeThread.out(), addressBlock.in());
        d.wire(addressBlock.out(), socket.in());

        //Reader
        d.wire(socket.out(), readThread.in());
        d.wire(readThread.out(), datagramPacketToString.in());
        d.wire(datagramPacketToString.out(), console);

        return socket;
    }
}
