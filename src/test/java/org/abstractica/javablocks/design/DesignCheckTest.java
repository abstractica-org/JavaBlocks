package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.BasicBlocks;
import org.abstractica.javablocks.blocks.connection.firsttest.SimplePingPongProtokol;
import org.abstractica.javablocks.testsupport.Sink;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/** A Design that cannot be built is refused at build time, every problem named, nothing built. */
class DesignCheckTest
{
	private static Design.Node node(String id, String kind, Map<String, Object> settings)
	{
		return new Design.Node(id, kind, settings);
	}

	private static Design.Wire wire(String from, String to)
	{
		String[] f = from.split("\\.");
		String[] t = to.split("\\.");
		return new Design.Wire(new Design.Port(f[0], f[1]), new Design.Port(t[0], t[1]));
	}

	private static String refusal(Design design, Catalogue catalogue)
	{
		return assertThrows(IllegalArgumentException.class, () -> design.build(catalogue)).getMessage();
	}

	private static String refusal(Designer d)
	{
		return refusal(d.design(), d.catalogue());
	}

	@Test
	void everyMissingCatalogueEntryIsNamed()
	{
		Design design = new Design(
				List.of(node("f", "push", Map.of("function", "shout")),
						node("p", "ifBlock", Map.of("predicate", "isEven")),
						node("s", "trashcan", Map.of())),
				List.of(wire("f.out", "s.in"), wire("p.whenTrue", "s.in"), wire("p.whenFalse", "s.in")));

		String message = refusal(design, new Catalogue());

		assertTrue(message.contains("no entry 'shout'"), message);
		assertTrue(message.contains("no entry 'isEven'"), message);
	}

	@Test
	void aCatalogueEntryOfTheWrongTypeIsRefused()
	{
		Design design = new Design(
				List.of(node("f", "push", Map.of("function", "shout")), node("s", "trashcan", Map.of())),
				List.of(wire("f.out", "s.in")));

		String message = refusal(design, new Catalogue().put("shout", "not a function"));

		assertTrue(message.contains("'shout' is not a Function"), message);
	}

	@Test
	void unknownKindsNodesPortsAndSettingsAreRefused()
	{
		Design design = new Design(
				List.of(node("a", "teleporter", Map.of()),
						node("t", "thread", Map.of("speed", 3)),
						node("s", "trashcan", Map.of())),
				List.of(wire("t.out", "ghost.in"), wire("t.sideways", "s.in")));

		String message = refusal(design, new Catalogue());

		assertTrue(message.contains("unknown kind 'teleporter'"), message);
		assertTrue(message.contains("unknown setting 'speed'"), message);
		assertTrue(message.contains("unknown node 'ghost'"), message);
		assertTrue(message.contains("no port 'sideways'"), message);
	}

	@Test
	void aDuplicateIdInTheDataIsRefused()
	{
		Design design = new Design(List.of(node("a", "trashcan", Map.of()), node("a", "console", Map.of())), List.of());

		assertTrue(refusal(design, new Catalogue()).contains("'a' is used twice"));
	}

	@Test
	void aWireThatIsNeitherPushNorPullIsRefused()
	{
		// A buffer's out (an Input) straight into a push block's in (an Output): both sides wait to be called.
		Design design = new Design(
				List.of(node("b", "buffer", Map.of("capacity", 1)),
						node("f", "push", Map.of("function", "id")),
						node("s", "trashcan", Map.of())),
				List.of(wire("b.out", "f.in"), wire("f.out", "s.in")));

		String message = refusal(design, new Catalogue().put("id", Function.identity()));

		assertTrue(message.contains("wire b.out -> f.in is neither a push wire"), message);
	}

	@Test
	void aSingleSlotWiredTwiceIsRefusedButTheDistributorTakesMany()
	{
		Designer d = new Designer();
		var source = d.hostSource("source", BasicBlocks.<String>getBufferBlock(1));
		var t = d.<String>thread("t");
		var one = d.<String>trashcan("one");
		var two = d.<String>trashcan("two");
		d.wire(source.out(), t.in());
		d.wire(t.out(), one.in());
		d.wire(t.out(), two.in());

		assertTrue(refusal(d).contains("port t.out takes one wire but has more"));

		Designer ok = new Designer();
		var all = ok.<String>distributor("all");
		var a = ok.<String>trashcan("a");
		var b = ok.<String>trashcan("b");
		ok.wire(all.out(), a.in());
		ok.wire(all.out(), b.in());
		assertDoesNotThrow(() -> ok.design().build(ok.catalogue()));
	}

	@Test
	void requiredSlotsLeftUnwiredAreNamed()
	{
		Designer d = new Designer();
		var source = d.hostSource("source", BasicBlocks.<Integer>getBufferBlock(1));
		var driver = d.<Integer>thread("driver");
		var even = d.ifBlock("even", (Integer i) -> i % 2 == 0);
		var sink = d.hostSink("sink", new Sink<Integer>());
		d.thread("idle");
		d.push("f", (Integer i) -> i);
		d.pull("g", (Integer i) -> i);
		d.pingPong("ping", new SimplePingPongProtokol());
		d.wire(source.out(), driver.in());
		d.wire(even.whenTrue(), sink.in());

		String message = refusal(d);

		assertTrue(message.contains("port driver.out must be wired"), message);
		assertTrue(message.contains("port even.whenFalse must be wired"), message);
		assertTrue(message.contains("port idle.in must be wired"), message);
		assertTrue(message.contains("port idle.out must be wired"), message);
		assertTrue(message.contains("port f.out must be wired"), message);
		assertTrue(message.contains("port g.in must be wired"), message);
		assertTrue(message.contains("port ping.send must be wired"), message);
		assertFalse(message.contains("even.in"), message);
		assertFalse(message.contains("debug"), message);
	}

	@Test
	void pingPongBlocksThatAreEachOthersSendersAreRefused()
	{
		Designer d = new Designer();
		var one = d.pingPong("one", new SimplePingPongProtokol());
		var two = d.pingPong("two", new SimplePingPongProtokol());
		d.wire(one.send(), two.in());
		d.wire(two.send(), one.in());

		String message = refusal(d);

		assertTrue(message.contains("'one' is, through its senders, its own sender"), message);
	}

	@Test
	void settingsOfTheWrongShapeAreRefused()
	{
		Design design = new Design(
				List.of(node("b", "buffer", Map.of()),
						node("c", "constant", Map.of()),
						node("v", "variable", Map.of("value", List.of(1)))),
				List.of());

		String message = refusal(design, new Catalogue());

		assertTrue(message.contains("'b' needs setting 'capacity' as Integer"), message);
		assertTrue(message.contains("'c' needs exactly one of the settings 'value' and 'valueName'"), message);
		assertTrue(message.contains("'v': setting 'value' must be a string, number or boolean"), message);
	}

	@Test
	void aFactoryRefusalNamesTheNode()
	{
		Designer d = new Designer();
		d.buffer("tiny", 0);

		String message = assertThrows(IllegalArgumentException.class, () -> d.design().build(d.catalogue())).getMessage();

		assertTrue(message.startsWith("node 'tiny': "), message);
	}
}
