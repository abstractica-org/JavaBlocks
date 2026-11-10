package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.BasicBlocks;
import org.abstractica.javablocks.blocks.basic.BufferBlock;
import org.abstractica.javablocks.testsupport.Sink;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/** What a Designer writes: plain data, with the Java objects in the catalogue under names. */
class DesignerTest
{
	@Test
	void theDesignerWritesNodesWiresAndCatalogueNamesAsPlainData()
	{
		BufferBlock<String> input = BasicBlocks.getBufferBlock(4);
		Sink<String> output = new Sink<>();
		Function<String, String> shoutFn = s -> s + "!";

		Designer d = new Designer();
		var source = d.hostSource("source", input);
		var driver = d.<String>thread("driver");
		var shout = d.push("shout", shoutFn);
		var sink = d.hostSink("sink", output);
		d.wire(source.out(), driver.in());
		d.wire(driver.out(), shout.in());
		d.wire(shout.out(), sink.in());
		Design design = d.design();

		assertEquals(List.of(
				new Design.Node("source", "hostSource", Map.of("object", "source.object")),
				new Design.Node("driver", "thread", Map.of()),
				new Design.Node("shout", "push", Map.of("function", "shout.function")),
				new Design.Node("sink", "hostSink", Map.of("object", "sink.object"))), design.nodes());
		assertEquals(List.of(
				new Design.Wire(new Design.Port("source", "out"), new Design.Port("driver", "in")),
				new Design.Wire(new Design.Port("driver", "out"), new Design.Port("shout", "in")),
				new Design.Wire(new Design.Port("shout", "out"), new Design.Port("sink", "in"))), design.wires());

		Catalogue c = d.catalogue();
		assertEquals(Set.of("source.object", "shout.function", "sink.object"), c.names());
		assertSame(shoutFn, c.get("shout.function"));
		assertSame(input, c.get("source.object"));
		assertSame(output, c.get("sink.object"));
	}

	@Test
	void theNamedFormStoresTheChosenName()
	{
		Designer d = new Designer();
		Function<String, String> trim = String::trim;
		d.push("tidy", "trim", trim);
		d.pull("tidy2", "trim", trim); // the same object under the same name is fine

		assertEquals(Map.of("function", "trim"), d.design().nodes().get(0).settings());
		assertEquals(Map.of("function", "trim"), d.design().nodes().get(1).settings());
		assertSame(trim, d.catalogue().get("trim"));
	}

	@Test
	void aConstantKeepsPlainValuesAndNamesOthers()
	{
		Designer d = new Designer();
		Object packet = new Object();
		d.constant("n", 5000L);
		d.constant("s", "hi");
		d.variable("v", true);
		d.constant("o", packet);

		List<Design.Node> nodes = d.design().nodes();
		assertEquals(Map.of("value", 5000L), nodes.get(0).settings());
		assertEquals(Map.of("value", "hi"), nodes.get(1).settings());
		assertEquals(Map.of("value", true), nodes.get(2).settings());
		assertEquals(Map.of("valueName", "o.value"), nodes.get(3).settings());
		assertSame(packet, d.catalogue().get("o.value"));
	}

	@Test
	void aDuplicateIdIsRefusedAndChangesNothing()
	{
		Designer d = new Designer();
		d.thread("a");

		assertThrows(IllegalArgumentException.class, () -> d.push("a", (String s) -> s));

		assertEquals(1, d.design().nodes().size());
		assertFalse(d.catalogue().contains("a.function"));
	}

	@Test
	void oneNameForTwoDifferentObjectsIsRefused()
	{
		Designer d = new Designer();
		Function<String, String> f1 = s -> s;
		Function<String, String> f2 = s -> s + s;
		d.push("x", "f", f1);

		assertThrows(IllegalArgumentException.class, () -> d.push("y", "f", f2));
		assertSame(f1, d.catalogue().get("f"));
	}

	@Test
	void theDesignAndCatalogueReturnedAreSnapshots()
	{
		Designer d = new Designer();
		d.push("x", (String s) -> s);
		Design before = d.design();
		Catalogue catalogueBefore = d.catalogue();

		d.push("y", (String s) -> s);

		assertEquals(1, before.nodes().size());
		assertEquals(Set.of("x.function"), catalogueBefore.names());
	}
}
