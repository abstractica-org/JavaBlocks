package org.abstractica.javablocks.examples.udp;

import org.abstractica.javablocks.blocks.basic.*;
import org.abstractica.javablocks.blocks.udp.UDPBlocks;
import org.abstractica.javablocks.blocks.udp.UDPFunctions;
import org.abstractica.javablocks.blocks.udp.UDPSocketBlock;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class SimpleUDPExample
{
    public static void main(String[] args) throws UnknownHostException, SocketException
    {
        Input<String> keyboard = BasicBlocks.getKeyboardBlock();
        Output<String> console = BasicBlocks.getConsoleBlock();
        ThreadBlock<DatagramPacket> writeThread = BasicBlocks.getThreadBlock();
        ThreadBlock<DatagramPacket> readThread = BasicBlocks.getThreadBlock();
        PullBlock<String, DatagramPacket> stringToDatagramPacket = BasicBlocks.getPullBlock(UDPFunctions.getStringToDatagramPacketFunction());
        PushBlock<DatagramPacket, String> datagramPacketToString = BasicBlocks.getPushBlock(UDPFunctions.getDatagramPacketToStringFunction());
        PushBlock<DatagramPacket, DatagramPacket> addressBlock = BasicBlocks.getPushBlock(UDPFunctions.getAddressFunction(InetAddress.getLocalHost(), 3337));
        UDPSocketBlock<DatagramPacket> socket = UDPBlocks.getUDPSocketBlock(3337, 1024);

        System.out.println("Socket on: " + socket.getAddress());

        //Hook it up

        //Writer
        stringToDatagramPacket.setInput(keyboard);
        writeThread.setInput(stringToDatagramPacket);
        writeThread.setOutput(addressBlock);
        addressBlock.setOutput(socket);

        //Reader
        readThread.setInput(socket);
        readThread.setOutput(datagramPacketToString);
        datagramPacketToString.setOutput(console);


        //Start the thread
        readThread.start();
        writeThread.start();
    }
}
