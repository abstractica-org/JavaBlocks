package org.abstractica.javablocks.blocks.basic;

public interface Block
{
	void setInfoReporter(Output<String> infoReporter);
	void setDebugReporter(Output<String> debugReporter);
	void setErrorReporter(Output<String> errorReporter);
	Output<String> getInfoReporter();
	Output<String> getDebugReporter();
	Output<String> getErrorReporter();
	void INFO(String msg);
	void DEBUG(String msg);
	void ERROR(String msg);
}
