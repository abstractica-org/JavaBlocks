package org.abstractica.javablocks.blocks.basic.impl;


import org.abstractica.javablocks.blocks.basic.Input;
import org.abstractica.javablocks.blocks.basic.Output;
import org.abstractica.javablocks.blocks.basic.ThreadBlock;

public class ThreadBlockImpl<E>
        extends AbstractBlock
        implements ThreadBlock<E>, Runnable
{
    private Input<E> input;
    private Output<E> output;
    private Thread workerThread;
    private volatile boolean running;
    private volatile boolean doingOutput;


    public ThreadBlockImpl()
    {
        super();
        this.input = null;
        this.output = null;
        this.running = false;
    }

    @Override
    public void run()
    {
        while (running)
        {
            try
            {
                DEBUG("Trying to get!");
                E item = input.get();
                DEBUG("Got: " + item);
                doingOutput = true;
                DEBUG("Trying to put!");
                output.put(item);
                DEBUG("Put: " + item);
                doingOutput = false;
                synchronized (this)
                {
                    this.notifyAll();
                }
            } catch (InterruptedException e)
            {
                //Do nothing, let 'stopped' do its work...
            } catch (Exception e)
            {
                if(running)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public synchronized void start() throws IllegalStateException
    {
        if(running) return;
        if(input == null || output == null)
        {
            throw new IllegalStateException("Worker may only be started if both input and output are connected!");
        }
        running = true;
        workerThread = new Thread(this);
        workerThread.start();
    }

    @Override
    public synchronized void stop() throws InterruptedException
    {
        if(!running) return;
        while (doingOutput)
        {
            wait(5000);
        }
        running = false;
        workerThread.interrupt();
        workerThread.join();
    }

    @Override
    public synchronized boolean isRunning()
    {
        return running;
    }

    @Override
    public synchronized void setInput(Input<E> input) throws IllegalStateException
    {
        if(running)
        {
            throw new IllegalStateException("Input can only be set, when worker is not running!");
        }
        this.input = input;
        this.notifyAll();
    }

    @Override
    public synchronized void setOutput(Output<E> output) throws IllegalStateException
    {
        if(running)
        {
            throw new IllegalStateException("Output can only be set, when worker is not running!");
        }
        this.output = output;
        notifyAll();
    }
}

