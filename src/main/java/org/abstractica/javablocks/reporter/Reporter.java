package org.abstractica.javablocks.reporter;

import org.abstractica.javablocks.blocks.basic.Block;
import org.abstractica.javablocks.blocks.basic.Output;

import java.io.PrintStream;

public class Reporter implements Output<String>
{
	private static final boolean infoOn = true;
	private static final boolean debugOn = true;
	private static final boolean errorOn = true;

	private static volatile Output<String> defaultInfoReporter;
	private static volatile Output<String> defaultDebugReporter;
	private static volatile Output<String> defaultErrorReporter;

	static
	{
		if(infoOn)
		{
			defaultInfoReporter = new Reporter( "INFO: ", System.out);
		}
		if(debugOn)
		{
			defaultDebugReporter = new Reporter( "DEBUG: ", System.out);
		}
		if(errorOn)
		{
			defaultErrorReporter = new Reporter( "ERROR: ", System.err);
		}
	}

	/**
	 * Sets the INFO reporter that blocks built from now on start with; {@code null} means silent.
	 * Blocks built before keep theirs. (J10)
	 */
	public static void setDefaultInfoReporter(Output<String> reporter)
	{
		defaultInfoReporter = reporter;
	}

	/**
	 * Sets the DEBUG reporter that blocks built from now on start with; {@code null} means silent.
	 * Blocks built before keep theirs. (J10)
	 */
	public static void setDefaultDebugReporter(Output<String> reporter)
	{
		defaultDebugReporter = reporter;
	}

	/**
	 * Sets the ERROR reporter that blocks built from now on start with; {@code null} means silent.
	 * Blocks built before keep theirs. (J10)
	 */
	public static void setDefaultErrorReporter(Output<String> reporter)
	{
		defaultErrorReporter = reporter;
	}

	public static Output<String> getInfoReporter(Block block)
	{

		return defaultInfoReporter;
	}

	public static Output<String> getDebugReporter(Block block)
	{
		return defaultDebugReporter;
	}

	public static Output<String> getErrorReporter(Block block)
	{
		return defaultErrorReporter;
	}

	private final String prefix;
	private final PrintStream out;

	public Reporter(String prefix, PrintStream out)
	{
		this.prefix = prefix;
		this.out = out;
	}

	@Override
	public void put(String msg) throws InterruptedException
	{
		out.println(prefix + msg);
	}
}
