package org.abstractica.javablocks.examples.basic;

import org.abstractica.javablocks.blocks.basic.*;

public class SimpleKeyboardConsoleExample
{
    public static void main(String[] args)
    {
        Input<String> keyboard = BasicBlocks.getKeyboardBlock();

        Output<String> console = BasicBlocks.getConsoleBlock();
        ThreadBlock<String> thread = BasicBlocks.getThreadBlock();
        PushBlock<String, String> push = BasicBlocks.getPushBlock(s -> s+", for fanden!");
        push.setOutput(console);
        thread.setInput(keyboard);
        thread.setOutput(push);
        thread.start();
    }
}
