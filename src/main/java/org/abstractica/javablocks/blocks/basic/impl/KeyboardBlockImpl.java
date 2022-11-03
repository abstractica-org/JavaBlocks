package org.abstractica.javablocks.blocks.basic.impl;


import org.abstractica.javablocks.blocks.basic.Input;

import java.util.Scanner;

//Singleton design pattern
public class KeyboardBlockImpl extends AbstractBlock implements Input<String>
{
    private static final Input<String> instance = new KeyboardBlockImpl();
    private final Scanner scanner = new Scanner(System.in);

    private KeyboardBlockImpl()
    {
    }

    public static Input<String> getInstance()
    {
        return instance;
    }


    @Override
    public String get()
    {
        return scanner.nextLine();
    }
}
