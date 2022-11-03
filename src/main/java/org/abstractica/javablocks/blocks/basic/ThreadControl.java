package org.abstractica.javablocks.blocks.basic;

public interface ThreadControl
{
    public void start();
    public void stop() throws InterruptedException;
    public boolean isRunning();
}
