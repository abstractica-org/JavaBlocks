package org.abstractica.javablocks.examples.udp;

import org.abstractica.javablocks.blocks.basic.ThreadControl;
import org.abstractica.javablocks.blocks.udp.UDPFunctions;
import org.abstractica.javablocks.design.Designer;
import org.abstractica.javablocks.design.PushIn;

import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;

/** Prints the text of each UDP packet arriving on a port. Built as a Design (J18). */
public class UDPToConsole implements ThreadControl
{
    private ThreadControl threadCtrl;

    public UDPToConsole(int port, int maxPackageSize) throws SocketException, UnknownHostException
    {
        Designer d = new Designer();
        design(d, port, maxPackageSize, d.console("console").in());
        threadCtrl = ExamplePipelines.build(d);
    }

    /**
     * Adds the pipeline to {@code d}: a thread pulls packets of up to {@code maxPackageSize} bytes
     * from a socket on {@code port} and pushes each one's text into {@code console}.
     */
    static void design(Designer d, int port, int maxPackageSize, PushIn<String> console)
    {
        var socket = d.udpSocket("socket", port, maxPackageSize);
        var thread = d.<DatagramPacket>thread("thread");
        var datagramPacketToString = d.push("datagramPacketToString", UDPFunctions.getDatagramPacketToStringFunction());

        d.wire(socket.out(), thread.in());
        d.wire(thread.out(), datagramPacketToString.in());
        d.wire(datagramPacketToString.out(), console);
    }

    @Override
    public void start()
    {
        threadCtrl.start();
    }

    /** Stops the thread, then closes the socket: a stopped UDPToConsole cannot be started again. */
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
