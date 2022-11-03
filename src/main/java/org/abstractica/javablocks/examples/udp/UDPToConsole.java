package org.abstractica.javablocks.examples.udp;

import org.abstractica.javablocks.blocks.basic.*;
import org.abstractica.javablocks.blocks.udp.UDPBlocks;
import org.abstractica.javablocks.blocks.udp.UDPFunctions;
import org.abstractica.javablocks.blocks.udp.UDPSocketBlock;

import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPToConsole implements ThreadControl
{
    private ThreadControl threadCtrl;

    public UDPToConsole(int port, int maxPackageSize) throws SocketException, UnknownHostException
    {
        //Create blocks
        UDPSocketBlock<DatagramPacket> socket = UDPBlocks.getUDPSocketBlock(port, maxPackageSize);
        ThreadBlock<DatagramPacket> thread = BasicBlocks.getThreadBlock();
        PushBlock<DatagramPacket, String> datagramPacketToString = BasicBlocks.getPushBlock(UDPFunctions.getDatagramPacketToStringFunction());
        Output<String> console = BasicBlocks.getConsoleBlock();

        //Hook up blocks
        thread.setInput(socket);
        thread.setOutput(datagramPacketToString);
        datagramPacketToString.setOutput(console);

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
