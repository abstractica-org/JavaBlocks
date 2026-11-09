package org.abstractica.javablocks.reporter;

import org.abstractica.javablocks.blocks.basic.BasicBlocks;
import org.abstractica.javablocks.blocks.basic.Block;
import org.abstractica.javablocks.blocks.basic.Output;
import org.abstractica.javablocks.testsupport.Sink;
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

	/** J10: a host sets the defaults once; blocks built afterwards pick them up, earlier blocks keep theirs. */
	@Test
	void aSetDefaultReachesBlocksBuiltAfterItAndNotBefore()
	{
		Output<String> info = Reporter.getInfoReporter(null);
		Output<String> debug = Reporter.getDebugReporter(null);
		Output<String> error = Reporter.getErrorReporter(null);
		try
		{
			Block before = BasicBlocks.getDistributorBlock();
			Sink<String> infoSink = new Sink<>();
			Sink<String> debugSink = new Sink<>();
			Sink<String> errorSink = new Sink<>();
			Reporter.setDefaultInfoReporter(infoSink);
			Reporter.setDefaultDebugReporter(debugSink);
			Reporter.setDefaultErrorReporter(errorSink);

			Block after = BasicBlocks.getDistributorBlock();
			after.INFO("i");
			after.DEBUG("d");
			after.ERROR("e");
			assertEquals(1, infoSink.items().size());
			assertTrue(infoSink.items().getFirst().endsWith("-> i"));
			assertTrue(debugSink.items().getFirst().endsWith("-> d"));
			assertTrue(errorSink.items().getFirst().endsWith("-> e"));
			assertSame(info, before.getInfoReporter(), "a block built before keeps its reporter");
			assertSame(debug, before.getDebugReporter());
			assertSame(error, before.getErrorReporter());

			Reporter.setDefaultDebugReporter(null);
			Block silent = BasicBlocks.getDistributorBlock();
			assertNull(silent.getDebugReporter(), "null means silent");
			assertDoesNotThrow(() -> silent.DEBUG("nobody hears this"));
			assertEquals(1, debugSink.items().size());
		} finally
		{
			Reporter.setDefaultInfoReporter(info);
			Reporter.setDefaultDebugReporter(debug);
			Reporter.setDefaultErrorReporter(error);
		}
	}
}
