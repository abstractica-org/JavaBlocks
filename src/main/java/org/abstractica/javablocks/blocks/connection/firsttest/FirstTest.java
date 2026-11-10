package org.abstractica.javablocks.blocks.connection.firsttest;

import org.abstractica.javablocks.blocks.basic.BasicFunctions;
import org.abstractica.javablocks.blocks.basic.Output;
import org.abstractica.javablocks.design.Designer;
import org.abstractica.javablocks.design.PullOut;

/**
 * Lines typed on the keyboard drive a ping-pong block that sends through a {@link SimpleSender};
 * a timer ticks it every 10 s with a 5 s timeout. Built as a Design (J15).
 */
public class FirstTest
{
	public static void main(String[] args)
	{
		Designer d = new Designer();
		design(d, d.keyboard("keyboard").out(), new SimpleSender(), 5000L, 10000L);
		d.design().build(d.catalogue()).start();
	}

	/**
	 * Adds the ping-pong pipeline to {@code d}: a driver thread pulls lines from {@code commands} into
	 * a ping-pong block that sends through {@code sender}; a timer thread pulls the {@code timeoutMillis}
	 * constant, delays it {@code tickMillis}, and pushes it into the block's timer.
	 */
	static void design(Designer d, PullOut<String> commands, Output<String> sender,
	                   long timeoutMillis, long tickMillis)
	{
		var driver = d.<String>thread("driver");
		var ping = d.pingPong("ping", new SimplePingPongProtokol());
		var send = d.hostSink("sender", sender);
		var timeout = d.constant("timeout", timeoutMillis);
		var timer = d.<Long>thread("timer");
		var tick = d.push("tick", BasicFunctions.<Long>DelayFunction(tickMillis));
		d.wire(commands, driver.in());
		d.wire(driver.out(), ping.in());
		d.wire(ping.send(), send.in());
		d.wire(timeout.out(), timer.in());
		d.wire(timer.out(), tick.in());
		d.wire(tick.out(), ping.timer());
	}
}
