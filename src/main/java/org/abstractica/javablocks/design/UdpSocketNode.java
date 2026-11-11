package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.udp.UDPSocketBlock;

import java.net.DatagramPacket;

/**
 * A UDP socket, opened when the pipeline is built and closed by {@link Pipeline#stop()}: packets
 * pushed in are sent, received packets are pulled out. A thread pulling from it stops within about
 * 100 ms of being asked to.
 */
public final class UdpSocketNode extends NodeHandle<UDPSocketBlock<DatagramPacket>>
{
	UdpSocketNode(String id)
	{
		super(id);
	}

	/** Push packets in here to send them; each packet carries its own destination. */
	public PushIn<DatagramPacket> in()
	{
		return pushIn("in");
	}

	/** Pull received packets from here; blocks until one arrives. */
	public PullOut<DatagramPacket> out()
	{
		return pullOut("out");
	}

	/** Where this block's info messages go; left unwired, the library's default info reporter. */
	public PushOut<String> info()
	{
		return pushOut("info");
	}

	/** Where this block's debug messages go; left unwired, the library's default debug reporter. */
	public PushOut<String> debug()
	{
		return pushOut("debug");
	}

	/** Where this block's error messages go; left unwired, the library's default error reporter. */
	public PushOut<String> error()
	{
		return pushOut("error");
	}
}
