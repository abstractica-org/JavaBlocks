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
    private volatile boolean stopping;
    private volatile boolean interruptedByStop;
    private boolean doingOutput; // guarded by this


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
        try
        {
            while (!stopping)
            {
                E item;
                try
                {
                    DEBUG("Trying to get!");
                    item = input.get();
                    DEBUG("Got: " + item);
                } catch (InterruptedException e)
                {
                    continue; // stop() interrupts a blocked get; the loop condition decides
                }
                synchronized (this)
                {
                    doingOutput = true;
                    Thread.interrupted(); // an interrupt aimed at the get must not abort this put
                }
                try
                {
                    DEBUG("Trying to put!");
                    output.put(item);
                    DEBUG("Put: " + item);
                } catch (InterruptedException e)
                {
                    //Do nothing, as before: the loop condition decides
                } finally
                {
                    synchronized (this)
                    {
                        doingOutput = false;
                        this.notifyAll();
                    }
                }
            }
        } catch (RuntimeException e)
        {
            if (!interruptedByStop) // as v0.2.0: only a failure after stop's interrupt is swallowed
            {
                throw new RuntimeException(e);
            }
        } finally
        {
            synchronized (this)
            {
                running = false;
                doingOutput = false;
                this.notifyAll();
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
        stopping = false;
        interruptedByStop = false;
        workerThread = new Thread(this);
        workerThread.start();
    }

    @Override
    public void stop() throws InterruptedException
    {
        Thread worker;
        synchronized (this)
        {
            if(!running) return;
            worker = workerThread; // the run this call stops, even if start() begins another while it waits
            stopping = true;
            while (doingOutput && workerThread == worker)
            {
                wait();
            }
            if (workerThread == worker)
            {
                interruptedByStop = true;
                worker.interrupt();
            }
        }
        worker.join(); // outside the monitor: the worker's handshake never waits on a stopper
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

