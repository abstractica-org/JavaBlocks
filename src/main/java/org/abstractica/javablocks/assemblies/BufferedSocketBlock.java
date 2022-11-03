package org.abstractica.javablocks.assemblies;

import org.abstractica.javablocks.blocks.basic.ThreadControl;
import org.abstractica.javablocks.blocks.udp.UDPSocketBlock;

public interface BufferedSocketBlock<E> extends UDPSocketBlock<E>, ThreadControl
{
}
