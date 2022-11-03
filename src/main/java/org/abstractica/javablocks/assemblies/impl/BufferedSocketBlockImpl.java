package org.abstractica.javablocks.assemblies.impl;

import org.abstractica.javablocks.assemblies.BufferedSocketBlock;
import org.abstractica.javablocks.blocks.basic.*;
import org.abstractica.javablocks.blocks.basic.impl.AbstractBlock;
import org.abstractica.javablocks.blocks.udp.UDPBlocks;
import org.abstractica.javablocks.blocks.udp.UDPFunctions;
import org.abstractica.javablocks.blocks.udp.UDPSocketBlock;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.function.Function;

public class BufferedSocketBlockImpl<E>  extends AbstractBlock
		implements BufferedSocketBlock<E>
{
	private final ThreadBlock<DatagramPacket> receiveThread;
	private final ThreadBlock<DatagramPacket> sendThread;
	private final UDPSocketBlock<DatagramPacket> socketBlock;
	private final BufferBlock<E> sendBuffer;
	private final BufferBlock<E> recieveBuffer;
	private final PushBlock<DatagramPacket, DatagramPacket> addressBlock;
	private final Function<DatagramPacket, E> fromDatagramPacket;
	private final Function<E, DatagramPacket> toDatagramPacket;

	public BufferedSocketBlockImpl(
			int port,
			int maxPacketSize,
			int bufferSize,
			Function<DatagramPacket, E> fromDatagramPacket,
			Function<E, DatagramPacket> toDatagramPacket) throws SocketException, UnknownHostException
	{
		this.fromDatagramPacket = fromDatagramPacket;
		this.toDatagramPacket = toDatagramPacket;
		this.socketBlock = UDPBlocks.getUDPSocketBlock(port, maxPacketSize);
		this.receiveThread = BasicBlocks.getThreadBlock();
		this.sendThread = BasicBlocks.getThreadBlock();
		this.sendBuffer = BasicBlocks.getBufferBlock(bufferSize);
		this.recieveBuffer = BasicBlocks.getBufferBlock(bufferSize);
		this.addressBlock =
				BasicBlocks.getPushBlock(UDPFunctions.getAddressFunction(InetAddress.getLocalHost(), port));
		PushBlock<DatagramPacket, E> fromDatagramPacketBlock = BasicBlocks.getPushBlock(fromDatagramPacket);
		PullBlock<E, DatagramPacket> toDatagramPacketBlock = BasicBlocks.getPullBlock(toDatagramPacket);

		//Hookup
		receiveThread.setInput(socketBlock);
		receiveThread.setOutput(fromDatagramPacketBlock);
		fromDatagramPacketBlock.setOutput(recieveBuffer);
		toDatagramPacketBlock.setInput(sendBuffer);
		sendThread.setInput(toDatagramPacketBlock);
		sendThread.setOutput(addressBlock);
		addressBlock.setOutput(socketBlock);
	}

	@Override
	public E get() throws Exception
	{
		return recieveBuffer.get();
	}

	@Override
	public void put(E packet) throws Exception
	{
		sendBuffer.put(packet);
	}

	@Override
	public void start()
	{
		sendThread.start();
		receiveThread.start();
	}

	@Override
	public void stop() throws InterruptedException
	{
		sendThread.stop();
		receiveThread.stop();
		socketBlock.close();
	}

	@Override
	public boolean isRunning()
	{
		return sendThread.isRunning() && receiveThread.isRunning();
	}

	@Override
	public int getPort()
	{
		return socketBlock.getPort();
	}

	@Override
	public InetAddress getAddress()
	{
		return socketBlock.getAddress();
	}

	@Override
	public void close()
	{
		socketBlock.close();
	}
}
