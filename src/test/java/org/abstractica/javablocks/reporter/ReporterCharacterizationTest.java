package org.abstractica.javablocks.reporter;

import org.abstractica.javablocks.blocks.basic.BasicBlocks;
import org.abstractica.javablocks.blocks.basic.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Characterizes the library's reporters: the defaults every block picks up when it is built. */
class ReporterCharacterizationTest
{
	/** Every block starts with the same shared default printers (INFO/DEBUG to stdout, ERROR to stderr). */
	@Test
	void everyNewBlockStartsWithTheSharedDefaultReporters()
	{
		Block a = BasicBlocks.getThreadBlock();
		Block b = BasicBlocks.getDistributorBlock();

		assertInstanceOf(Reporter.class, a.getInfoReporter());
		assertInstanceOf(Reporter.class, a.getDebugReporter());
		assertInstanceOf(Reporter.class, a.getErrorReporter());
		assertSame(a.getInfoReporter(), b.getInfoReporter());
		assertSame(a.getDebugReporter(), b.getDebugReporter());
		assertSame(a.getErrorReporter(), b.getErrorReporter());
		assertNotSame(a.getInfoReporter(), a.getDebugReporter());
		assertSame(Reporter.getInfoReporter(a), Reporter.getInfoReporter(b), "the block argument is ignored");
		assertSame(a.getInfoReporter(), Reporter.getInfoReporter(null));
	}
}
