package org.abstractica.javablocks.blocks.basic.impl;

import org.abstractica.javablocks.blocks.basic.Block;
import org.abstractica.javablocks.blocks.basic.Output;
import org.abstractica.javablocks.reporter.Reporter;

public abstract class AbstractBlock implements Block
{
	private Output<String> infoReporter;
	private Output<String> debugReporter;
	private Output<String> errorReporter;

	public AbstractBlock()
	{
		infoReporter = Reporter.getInfoReporter(this);
		debugReporter = Reporter.getDebugReporter(this);
		errorReporter = Reporter.getErrorReporter(this);
	}


	@Override
	public void setInfoReporter(Output<String> infoReporter)
	{
		this.infoReporter = infoReporter;
	}

	@Override
	public void setDebugReporter(Output<String> debugReporter)
	{
		this.debugReporter = debugReporter;
	}

	@Override
	public void setErrorReporter(Output<String> errorReporter)
	{
		this.errorReporter = errorReporter;
	}

	@Override
	public Output<String> getInfoReporter()
	{
		return infoReporter;
	}

	@Override
	public Output<String> getDebugReporter()
	{
		return debugReporter;
	}

	@Override
	public Output<String> getErrorReporter()
	{
		return errorReporter;
	}

	@Override
	public void INFO(String msg)
	{
		if(infoReporter != null)
		{
			try
			{
				infoReporter.put(this.getClass().getSimpleName() + " -> " + msg);
			} catch (Exception e)
			{
				ERROR(e.toString());
			}
		}
	}

	@Override
	public void DEBUG(String msg)
	{
		if(debugReporter != null)
		{
			try
			{
				debugReporter.put(this.getClass().getSimpleName() + " -> " + msg);
			} catch (Exception e)
			{
				ERROR(e.toString());
			}
		}
	}

	@Override
	public void ERROR(String msg)
	{
		if(errorReporter != null)
		{
			try
			{
				errorReporter.put(this.getClass().getSimpleName() + " -> " + msg);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
