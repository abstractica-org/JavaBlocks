package org.abstractica.javablocks.testsupport;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Daemon threads with join-based observation, so a test can assert "this call is still blocked". */
public final class Async
{
	private Async() {}

	public interface Step
	{
		void run() throws Exception;
	}

	/** Runs {@code step} on a daemon thread. A checked exception becomes a RuntimeException on that thread. */
	public static Thread daemon(Step step)
	{
		return Thread.ofPlatform().daemon(true).start(() ->
		{
			try
			{
				step.run();
			} catch (RuntimeException e)
			{
				throw e;
			} catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		});
	}

	public static boolean finishesWithin(Thread t, long millis) throws InterruptedException
	{
		t.join(millis);
		return !t.isAlive();
	}

	public static boolean stillRunningAfter(Thread t, long millis) throws InterruptedException
	{
		t.join(millis);
		return t.isAlive();
	}

	/** True once {@code t} is parked in a wait/sleep/park (WAITING or TIMED_WAITING). */
	public static boolean parkedWithin(Thread t, long millis) throws InterruptedException
	{
		return becomesTrueWithin(() ->
		{
			Thread.State s = t.getState();
			return s == Thread.State.WAITING || s == Thread.State.TIMED_WAITING;
		}, millis);
	}

	public static boolean becomesTrueWithin(BooleanSupplier condition, long millis) throws InterruptedException
	{
		long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
		while (!condition.getAsBoolean())
		{
			if (System.nanoTime() > deadline) return false;
			Thread.sleep(5);
		}
		return true;
	}
}
