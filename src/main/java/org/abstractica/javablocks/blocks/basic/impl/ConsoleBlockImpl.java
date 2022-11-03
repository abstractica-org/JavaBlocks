package org.abstractica.javablocks.blocks.basic.impl;


import org.abstractica.javablocks.blocks.basic.Output;

//Singleton
public class ConsoleBlockImpl extends AbstractBlock implements Output<String>
{
    private final static Output<String> instance = new ConsoleBlockImpl();

    public static Output<String> getInstance()
    {
        return instance;
    }

    private ConsoleBlockImpl()
    {
    }

    @Override
    public void put(String str)
    {
        System.out.println(str);
    }
}
