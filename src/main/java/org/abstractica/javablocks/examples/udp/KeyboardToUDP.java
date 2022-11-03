package org.abstractica.javablocks.examples.udp;

import org.abstractica.javablocks.blocks.basic.*;
import org.abstractica.javablocks.blocks.udp.UDPBlocks;
import org.abstractica.javablocks.blocks.udp.UDPFunctions;
import org.abstractica.javablocks.blocks.udp.UDPSocketBlock;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class KeyboardToUDP implements ThreadControl
{
    private ThreadControl threadCtrl;

    public KeyboardToUDP(int socketPort, InetAddress receiverAddress, int receiverPort) throws SocketException, UnknownHostException
    {
        //Create blocks
        Input<String> keyboard = BasicBlocks.getKeyboardBlock();
        PullBlock<String, DatagramPacket> stringToDatagramPacket = BasicBlocks.getPullBlock(UDPFunctions.getStringToDatagramPacketFunction());
        PullBlock<DatagramPacket, DatagramPacket> addressBlock = BasicBlocks.getPullBlock(UDPFunctions.getAddressFunction(receiverAddress, receiverPort));
        ThreadBlock<DatagramPacket> thread = BasicBlocks.getThreadBlock();
        //Setting maxPacketsize to 0 since we will not read from the socket...
        UDPSocketBlock<DatagramPacket> socket = UDPBlocks.getUDPSocketBlock(socketPort, 0);

        //Hook up blocks
        stringToDatagramPacket.setInput(keyboard);
        addressBlock.setInput(stringToDatagramPacket);
        thread.setInput(addressBlock);
        thread.setOutput(socket);

        //Set thread control
        threadCtrl = thread;
    }

    @Override
    public void start()
    {
        threadCtrl.start();
    }

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
