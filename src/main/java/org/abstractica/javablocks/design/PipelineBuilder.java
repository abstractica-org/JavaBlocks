package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.BasicBlocks;
import org.abstractica.javablocks.blocks.basic.Block;
import org.abstractica.javablocks.blocks.basic.DistributorBlock;
import org.abstractica.javablocks.blocks.basic.IfBlock;
import org.abstractica.javablocks.blocks.basic.Input;
import org.abstractica.javablocks.blocks.basic.Output;
import org.abstractica.javablocks.blocks.basic.PullBlock;
import org.abstractica.javablocks.blocks.basic.PushBlock;
import org.abstractica.javablocks.blocks.basic.ThreadBlock;
import org.abstractica.javablocks.blocks.connection.PingPongBlock;
import org.abstractica.javablocks.blocks.connection.PingPongProtocol;
import org.abstractica.javablocks.blocks.connection.impl.PingPongBlockImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.abstractica.javablocks.design.Kinds.*;

/** Checks a Design against the kind table and a catalogue, then builds it. */
final class PipelineBuilder
{
	private final Design design;
	private final Catalogue catalogue;
	private final Map<String, Design.Node> nodes = new LinkedHashMap<>();
	private final List<String> problems = new ArrayList<>();
	/** For each ping-pong node, the node its {@code send} is wired to. */
	private final Map<String, String> senders = new HashMap<>();
	private final Map<String, Object> built = new LinkedHashMap<>();
	/** The handler factories of the map nodes built here. */
	private final List<PerKeyCopies<?, ?>> maps = new ArrayList<>();
	/** True when this is a map's nested Design. */
	private final boolean nested;
	/** The key a nested Design's copy is built for; null otherwise. */
	private final Object key;

	private PipelineBuilder(Design design, Catalogue catalogue, boolean nested, Object key)
	{
		this.design = design;
		this.catalogue = catalogue;
		this.nested = nested;
		this.key = key;
	}

	static Pipeline build(Design design, Catalogue catalogue)
	{
		// A copy: the maps read it on later keys, from whichever thread brings them.
		PipelineBuilder b = new PipelineBuilder(design, new Catalogue(catalogue), false, null);
		b.check();
		if (!b.problems.isEmpty())
		{
			throw new IllegalArgumentException("the Design cannot be built:\n  - " + String.join("\n  - ", b.problems));
		}
		b.construct();
		b.connect();
		return new Pipeline(b.built, b.maps);
	}

	/**
	 * Builds one copy of a map's nested Design for {@code key}, not started; returns its entry's handler,
	 * which holds the copy. The nested Design was checked when the outer one was built.
	 */
	static PerKeyCopies.Handler<?, ?> buildCopy(Design design, Catalogue catalogue, Object key)
	{
		PipelineBuilder b = new PipelineBuilder(design, catalogue, true, key);
		b.check();
		if (!b.problems.isEmpty())
		{
			throw new IllegalStateException("checked when the pipeline was built: " + b.problems);
		}
		b.construct();
		b.connect();
		Pipeline copy = new Pipeline(b.built, b.maps);
		for (Design.Node n : b.nodes.values())
		{
			if (n.kind().equals(ENTRY))
			{
				PerKeyCopies.Handler<?, ?> handler = (PerKeyCopies.Handler<?, ?>) b.built.get(n.id());
				handler.copy = copy;
				return handler;
			}
		}
		throw new IllegalStateException("checked when the pipeline was built: a nested Design has an entry");
	}

	// --- checks: every problem is collected, nothing is built while any remains ---

	private void check()
	{
		for (Design.Node n : design.nodes())
		{
			if (nodes.putIfAbsent(n.id(), n) != null)
			{
				problems.add("node id '" + n.id() + "' is used twice");
				continue;
			}
			KindSpec kind = Kinds.get(n.kind());
			if (kind == null)
			{
				problems.add("node '" + n.id() + "' has unknown kind '" + n.kind() + "' (known: " + Kinds.names() + ")");
				continue;
			}
			if (kind.perKeyOnly() && !nested)
			{
				problems.add("node '" + n.id() + "': kind '" + n.kind() + "' is allowed only inside a map's nested Design");
			}
			checkSettings(n, kind);
		}
		if (nested && design.nodes().stream().filter(n -> n.kind().equals(ENTRY)).count() != 1)
		{
			problems.add("a map's nested Design needs exactly one node of kind '" + ENTRY + "'");
		}
		Map<Design.Port, Integer> wiresAt = new HashMap<>();
		for (Design.Wire w : design.wires())
		{
			PortSpec from = port(w.from(), w);
			PortSpec to = port(w.to(), w);
			if (from == null || to == null) continue;
			boolean push = from.slot() == Slot.PUSH_OUT && to.slot() == Slot.PUSH_IN;
			boolean pull = from.slot() == Slot.PULL_OUT && to.slot() == Slot.PULL_IN;
			if (!push && !pull)
			{
				problems.add("wire " + w + " is neither a push wire (a slot holding an Output to a block that is one)"
						+ " nor a pull wire (a block that is an Input to a slot holding one)");
				continue;
			}
			// The single side of a wire is the slot that holds a reference: from for push, to for pull.
			Design.Port holder = push ? w.from() : w.to();
			PortSpec holderSpec = push ? from : to;
			int count = wiresAt.merge(holder, 1, Integer::sum);
			if (count == 2 && !holderSpec.many())
			{
				problems.add("port " + holder + " takes one wire but has more");
			}
			if (push && w.from().name().equals("send") && nodes.get(w.from().node()).kind().equals(PING_PONG))
			{
				senders.put(w.from().node(), w.to().node());
			}
		}
		for (Design.Node n : nodes.values())
		{
			KindSpec kind = Kinds.get(n.kind());
			if (kind == null) continue;
			kind.ports().forEach((name, spec) ->
			{
				if (spec.required() && !wiresAt.containsKey(new Design.Port(n.id(), name)))
				{
					problems.add("port " + n.id() + "." + name + " must be wired");
				}
			});
		}
		checkSenderCycles();
	}

	private void checkSettings(Design.Node n, KindSpec kind)
	{
		Set<String> allowed = new HashSet<>(kind.catalogueSettings().keySet());
		allowed.addAll(kind.plainSettings().keySet());
		if (kind.holdsValue())
		{
			allowed.add(VALUE);
			allowed.add(VALUE_NAME);
			boolean plain = n.settings().containsKey(VALUE);
			boolean named = n.settings().containsKey(VALUE_NAME);
			if (plain == named)
			{
				problems.add("node '" + n.id() + "' needs exactly one of the settings '" + VALUE + "' and '" + VALUE_NAME + "'");
			}
			if (plain && !isPlain(n.settings().get(VALUE)))
			{
				problems.add("node '" + n.id() + "': setting '" + VALUE + "' must be a string, number or boolean");
			}
			if (named)
			{
				catalogueEntry(n, VALUE_NAME, Object.class);
			}
		}
		for (String key : n.settings().keySet())
		{
			if (!allowed.contains(key))
			{
				problems.add("node '" + n.id() + "' has unknown setting '" + key + "'");
			}
		}
		kind.plainSettings().forEach((key, type) ->
		{
			Object v = n.settings().get(key);
			if (!type.isInstance(v))
			{
				problems.add("node '" + n.id() + "' needs setting '" + key + "' as " + type.getSimpleName());
			}
		});
		kind.catalogueSettings().forEach((key, type) -> catalogueEntry(n, key, type));
		if (nested && n.settings().get(CAPACITY) instanceof Integer c && c < 1)
		{
			// The buffer refuses this itself (J6), but a nested Design is built only when a key arrives:
			// checked here, it is refused when the pipeline is built.
			problems.add("node '" + n.id() + "': buffer capacity must be at least 1, was " + c);
		}
		if (n.kind().equals(MAP) && n.settings().get(PER_KEY) instanceof Design perKey)
		{
			PipelineBuilder inner = new PipelineBuilder(perKey, catalogue, true, null);
			inner.check();
			for (String p : inner.problems)
			{
				problems.add("in map '" + n.id() + "': " + p);
			}
		}
	}

	private void catalogueEntry(Design.Node n, String key, Class<?> type)
	{
		Object name = n.settings().get(key);
		if (!(name instanceof String s))
		{
			problems.add("node '" + n.id() + "' needs setting '" + key + "', a catalogue name");
			return;
		}
		if (!catalogue.contains(s))
		{
			problems.add("node '" + n.id() + "': the catalogue has no entry '" + s + "'");
			return;
		}
		if (!type.isInstance(catalogue.get(s)))
		{
			problems.add("node '" + n.id() + "': catalogue entry '" + s + "' is not a " + type.getSimpleName());
		}
	}

	private PortSpec port(Design.Port p, Design.Wire w)
	{
		Design.Node n = nodes.get(p.node());
		if (n == null)
		{
			problems.add("wire " + w + " names unknown node '" + p.node() + "'");
			return null;
		}
		KindSpec kind = Kinds.get(n.kind());
		if (kind == null) return null; // already reported
		PortSpec spec = kind.ports().get(p.name());
		if (spec == null)
		{
			problems.add("wire " + w + ": a " + n.kind() + " has no port '" + p.name() + "' (its ports: "
					+ kind.ports().keySet() + ")");
		}
		return spec;
	}

	/** A ping-pong block is built after its sender, so senders must not form a cycle. */
	private void checkSenderCycles()
	{
		for (String start : senders.keySet())
		{
			Set<String> seen = new HashSet<>();
			String at = start;
			while (at != null && seen.add(at))
			{
				at = senders.get(at);
			}
			if (at != null && at.equals(start))
			{
				problems.add("ping-pong node '" + start + "' is, through its senders, its own sender");
			}
		}
	}

	// --- building: every check has passed ---

	private void construct()
	{
		for (Design.Node n : nodes.values())
		{
			if (!n.kind().equals(PING_PONG))
			{
				built.put(n.id(), create(n));
			}
		}
		for (Design.Node n : nodes.values())
		{
			if (n.kind().equals(PING_PONG))
			{
				pingPong(n.id());
			}
		}
		// Keep the Design's node order.
		Map<String, Object> ordered = new LinkedHashMap<>();
		for (String id : nodes.keySet()) ordered.put(id, built.get(id));
		built.clear();
		built.putAll(ordered);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private Object pingPong(String id)
	{
		Object done = built.get(id);
		if (done != null) return done;
		String senderId = senders.get(id);
		if (nodes.get(senderId).kind().equals(PING_PONG))
		{
			pingPong(senderId);
		}
		Output sender = target(new Design.Port(senderId, findSendWire(id).to().name()));
		PingPongProtocol protocol = (PingPongProtocol) catalogue.get((String) nodes.get(id).settings().get(PROTOCOL));
		Object block = new PingPongBlockImpl(protocol, sender);
		built.put(id, block);
		return block;
	}

	private Design.Wire findSendWire(String pingPongId)
	{
		Design.Port send = new Design.Port(pingPongId, "send");
		for (Design.Wire w : design.wires())
		{
			if (w.from().equals(send)) return w;
		}
		throw new IllegalStateException("checked above: " + send + " is wired");
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private Object create(Design.Node n)
	{
		Map<String, Object> s = n.settings();
		try
		{
			return switch (n.kind())
			{
				case CONSTANT -> BasicBlocks.getConstantBlock(value(s));
				case VARIABLE -> BasicBlocks.getVariableBlock(value(s));
				case IF_BLOCK -> BasicBlocks.getIfBlock((Predicate) catalogue.get((String) s.get(PREDICATE)));
				case BUFFER -> BasicBlocks.getBufferBlock((Integer) s.get(CAPACITY));
				case THREAD -> BasicBlocks.getThreadBlock();
				case DISTRIBUTOR -> BasicBlocks.getDistributorBlock();
				case PULL -> BasicBlocks.getPullBlock((Function) catalogue.get((String) s.get(FUNCTION)));
				case PUSH -> BasicBlocks.getPushBlock((Function) catalogue.get((String) s.get(FUNCTION)));
				case TRASHCAN -> BasicBlocks.getTrashcanBlock();
				case KEYBOARD -> BasicBlocks.getKeyboardBlock();
				case CONSOLE -> BasicBlocks.getConsoleBlock();
				case HOST_SINK, HOST_SOURCE -> catalogue.get((String) s.get(OBJECT));
				case MAP ->
				{
					PerKeyCopies<?, ?> copies = new PerKeyCopies<>(
							(Function) catalogue.get((String) s.get(FUNCTION)), (Design) s.get(PER_KEY), catalogue);
					maps.add(copies);
					yield BasicBlocks.getMapBlock(copies);
				}
				case ENTRY -> new PerKeyCopies.Handler<>(key);
				case HOST_SINK_PER_KEY -> perKeyObject(n);
				default -> throw new IllegalStateException("kind '" + n.kind() + "' is in the table but not built");
			};
		} catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException("node '" + n.id() + "': " + e.getMessage(), e);
		}
	}

	/** A hostSinkPerKey node's object: its function, applied to this copy's key. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private Object perKeyObject(Design.Node n)
	{
		Object made;
		try
		{
			made = ((Function) catalogue.get((String) n.settings().get(OBJECT))).apply(key);
		} catch (RuntimeException e)
		{
			throw new IllegalStateException("node '" + n.id() + "': its function failed for key '" + key + "'", e);
		}
		if (!(made instanceof Output))
		{
			throw new IllegalStateException("node '" + n.id() + "': its function gave " + made
					+ " for key '" + key + "', not an Output");
		}
		return made;
	}

	private Object value(Map<String, Object> s)
	{
		return s.containsKey(VALUE) ? s.get(VALUE) : catalogue.get((String) s.get(VALUE_NAME));
	}

	/** The Output a push wire ends at. */
	@SuppressWarnings("rawtypes")
	private Output target(Design.Port p)
	{
		Object o = built.get(p.node());
		if (p.name().equals("timer"))
		{
			return ((PingPongBlock<?>) o).getTimerConnector();
		}
		return (Output) o;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void connect()
	{
		for (Design.Wire w : design.wires())
		{
			Object from = built.get(w.from().node());
			Object to = built.get(w.to().node());
			String kindFrom = nodes.get(w.from().node()).kind();
			switch (w.from().name())
			{
				case "info" -> ((Block) from).setInfoReporter(target(w.to()));
				case "debug" -> ((Block) from).setDebugReporter(target(w.to()));
				case "error" -> ((Block) from).setErrorReporter(target(w.to()));
				case "send" -> { } // given to the ping-pong block's constructor
				case "whenTrue" -> ((IfBlock) from).setTrueOutput(target(w.to()));
				case "whenFalse" -> ((IfBlock) from).setFalseOutput(target(w.to()));
				case "out" ->
				{
					switch (kindFrom)
					{
						case THREAD -> ((ThreadBlock) from).setOutput(target(w.to()));
						case PUSH -> ((PushBlock) from).setOutput(target(w.to()));
						case DISTRIBUTOR -> ((DistributorBlock) from).addOutput(target(w.to()));
						case ENTRY -> ((PerKeyCopies.Handler) from).setTarget(target(w.to()));
						default -> pullInto(w, from, to); // a block that is an Input
					}
				}
				default -> throw new IllegalStateException("checked above: " + w);
			}
		}
	}

	/** A pull wire: the slot at {@code w.to()} is given the Input {@code from}. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private void pullInto(Design.Wire w, Object from, Object to)
	{
		switch (nodes.get(w.to().node()).kind())
		{
			case THREAD -> ((ThreadBlock) to).setInput((Input) from);
			case PULL -> ((PullBlock) to).setInput((Input) from);
			default -> throw new IllegalStateException("checked above: " + w);
		}
	}
}
