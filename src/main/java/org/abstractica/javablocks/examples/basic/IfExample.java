package org.abstractica.javablocks.examples.basic;


import org.abstractica.javablocks.blocks.basic.*;

public class IfExample
{
    public static void main(String[] args)
    {
        Input<String> keyboard = BasicBlocks.getKeyboardBlock();
        ThreadBlock<String> thread = BasicBlocks.getThreadBlock();
        IfBlock<String> ifBlock = BasicBlocks.getIfBlock(s -> s.toUpperCase().contains("FUCK"));
        Output<String> console = BasicBlocks.getConsoleBlock();
        PushBlock<String, String> eraser = BasicBlocks.getPushBlock(s -> "****");

        //Hook up blocks
        thread.setInput(keyboard);
        thread.setOutput(ifBlock);
        ifBlock.setTrueOutput(eraser);
        ifBlock.setFalseOutput(console);
        eraser.setOutput(console);

        thread.start();

    }
}
