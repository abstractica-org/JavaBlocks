package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.Input;
import org.abstractica.javablocks.blocks.basic.Output;
import org.abstractica.javablocks.blocks.connection.PingPongProtocol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The one definition of every kind a Design may hold: its ports, which of them must be wired,
 * and its settings. The checks and the build both read it, so they cannot drift apart.
 */
final class Kinds
{
	private Kinds() {}

	enum Slot
	{
		/** Holds an Output: a push wire starts here. */
		PUSH_OUT,
		/** Is an Output: a push wire ends here. */
		PUSH_IN,
		/** Is an Input: a pull wire starts here. */
		PULL_OUT,
		/** Holds an Input: a pull wire ends here. */
		PULL_IN
	}

	/** A port: its slot, whether it must be wired, whether it takes more than one wire. */
	record PortSpec(Slot slot, boolean required, boolean many) {}

	/**
	 * A kind: its ports, whether it has reporter ports, its catalogue settings (key to the type the
	 * catalogue object must have) and its plain settings (key to the plain type).
	 */
	record KindSpec(String name,
	                Map<String, PortSpec> ports,
	                boolean reporting,
	                Map<String, Class<?>> catalogueSettings,
	                Map<String, Class<?>> plainSettings,
	                boolean holdsValue,
	                boolean perKeyOnly)
	{
	}

	static final String CONSTANT = "constant";
	static final String VARIABLE = "variable";
	static final String IF_BLOCK = "ifBlock";
	static final String BUFFER = "buffer";
	static final String THREAD = "thread";
	static final String DISTRIBUTOR = "distributor";
	static final String PULL = "pull";
	static final String PUSH = "push";
	static final String TRASHCAN = "trashcan";
	static final String KEYBOARD = "keyboard";
	static final String CONSOLE = "console";
	static final String HOST_SINK = "hostSink";
	static final String HOST_SOURCE = "hostSource";
	static final String PING_PONG = "pingPong";
	static final String MAP = "map";
	static final String UDP_SOCKET = "udpSocket";
	/** Only inside a map's nested Design: this key's messages enter here. */
	static final String ENTRY = "entry";
	/** Only inside a map's nested Design: a host Output made from the key. */
	static final String HOST_SINK_PER_KEY = "hostSinkPerKey";

	/** Settings keys. */
	static final String VALUE = "value";
	static final String VALUE_NAME = "valueName";
	static final String FUNCTION = "function";
	static final String PREDICATE = "predicate";
	static final String PROTOCOL = "protocol";
	static final String OBJECT = "object";
	static final String CAPACITY = "capacity";
	static final String PORT = "port";
	static final String MAX_PACKET_SIZE = "maxPacketSize";
	/** A map's nested Design: the one setting that is not a string, number or boolean. */
	static final String PER_KEY = "perKey";

	/** The reporter ports every reporting kind has; never required. */
	static final Set<String> REPORTER_PORTS = Set.of("info", "debug", "error");

	/** The plain types a setting value may have. */
	static boolean isPlain(Object value)
	{
		return value instanceof String
				|| value instanceof Integer
				|| value instanceof Long
				|| value instanceof Double
				|| value instanceof Boolean;
	}

	private static final Map<String, KindSpec> KINDS = new LinkedHashMap<>();

	private static PortSpec pushOut(boolean required) { return new PortSpec(Slot.PUSH_OUT, required, false); }
	private static PortSpec pullIn(boolean required) { return new PortSpec(Slot.PULL_IN, required, false); }
	private static final PortSpec PUSH_IN = new PortSpec(Slot.PUSH_IN, false, true);
	private static final PortSpec PULL_OUT = new PortSpec(Slot.PULL_OUT, false, true);

	private static void kind(String name, boolean reporting, Map<String, PortSpec> ports,
	                         Map<String, Class<?>> catalogueSettings, Map<String, Class<?>> plainSettings,
	                         boolean holdsValue)
	{
		kind(name, reporting, ports, catalogueSettings, plainSettings, holdsValue, false);
	}

	private static void kind(String name, boolean reporting, Map<String, PortSpec> ports,
	                         Map<String, Class<?>> catalogueSettings, Map<String, Class<?>> plainSettings,
	                         boolean holdsValue, boolean perKeyOnly)
	{
		Map<String, PortSpec> all = new LinkedHashMap<>(ports);
		if (reporting)
		{
			for (String r : REPORTER_PORTS) all.put(r, pushOut(false));
		}
		KINDS.put(name, new KindSpec(name, Map.copyOf(all), reporting,
				Map.copyOf(catalogueSettings), Map.copyOf(plainSettings), holdsValue, perKeyOnly));
	}

	static
	{
		kind(CONSTANT, true, Map.of("out", PULL_OUT), Map.of(), Map.of(), true);
		kind(VARIABLE, true, Map.of("in", PUSH_IN, "out", PULL_OUT), Map.of(), Map.of(), true);
		kind(IF_BLOCK, true, Map.of("in", PUSH_IN, "whenTrue", pushOut(true), "whenFalse", pushOut(true)),
				Map.of(PREDICATE, Predicate.class), Map.of(), false);
		kind(BUFFER, true, Map.of("in", PUSH_IN, "out", PULL_OUT), Map.of(), Map.of(CAPACITY, Integer.class), false);
		kind(THREAD, true, Map.of("in", pullIn(true), "out", pushOut(true)), Map.of(), Map.of(), false);
		kind(DISTRIBUTOR, true, Map.of("in", PUSH_IN, "out", new PortSpec(Slot.PUSH_OUT, false, true)),
				Map.of(), Map.of(), false);
		kind(PULL, true, Map.of("in", pullIn(true), "out", PULL_OUT), Map.of(FUNCTION, Function.class), Map.of(), false);
		kind(PUSH, true, Map.of("in", PUSH_IN, "out", pushOut(true)), Map.of(FUNCTION, Function.class), Map.of(), false);
		kind(TRASHCAN, false, Map.of("in", PUSH_IN), Map.of(), Map.of(), false);
		kind(KEYBOARD, false, Map.of("out", PULL_OUT), Map.of(), Map.of(), false);
		kind(CONSOLE, false, Map.of("in", PUSH_IN), Map.of(), Map.of(), false);
		kind(HOST_SINK, false, Map.of("in", PUSH_IN), Map.of(OBJECT, Output.class), Map.of(), false);
		kind(HOST_SOURCE, false, Map.of("out", PULL_OUT), Map.of(OBJECT, Input.class), Map.of(), false);
		kind(PING_PONG, true, Map.of("in", PUSH_IN, "timer", PUSH_IN, "send", pushOut(true)),
				Map.of(PROTOCOL, PingPongProtocol.class), Map.of(), false);
		kind(MAP, true, Map.of("in", PUSH_IN), Map.of(FUNCTION, Function.class), Map.of(PER_KEY, Design.class), false);
		kind(UDP_SOCKET, true, Map.of("in", PUSH_IN, "out", PULL_OUT), Map.of(),
				Map.of(PORT, Integer.class, MAX_PACKET_SIZE, Integer.class), false);
		kind(ENTRY, false, Map.of("out", pushOut(true)), Map.of(), Map.of(), false, true);
		kind(HOST_SINK_PER_KEY, false, Map.of("in", PUSH_IN), Map.of(OBJECT, Function.class), Map.of(), false, true);
	}

	/** The kind's spec, or null when no such kind exists. */
	static KindSpec get(String kind)
	{
		return KINDS.get(kind);
	}

	static Set<String> names()
	{
		return KINDS.keySet();
	}
}
