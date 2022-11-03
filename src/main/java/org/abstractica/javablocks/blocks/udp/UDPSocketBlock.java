package org.abstractica.javablocks.blocks.udp;


import org.abstractica.javablocks.blocks.basic.Block;
import org.abstractica.javablocks.blocks.basic.Input;
import org.abstractica.javablocks.blocks.basic.Output;

import java.net.InetAddress;

public interface UDPSocketBlock<E> extends Block, Input<E>, Output<E>
{
    int getPort();
    InetAddress getAddress();
    void close();
}
