package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.Input;
import org.abstractica.javablocks.blocks.basic.Output;
import org.abstractica.javablocks.blocks.connection.PingPongProtocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.abstractica.javablocks.design.Kinds.*;

/**
 * The typed way to write a {@link Design}. Every call records nodes and wires as plain data;
 * Java objects go into a {@link Catalogue} under a name, by default {@code <node>.function},
 * {@code <node>.predicate}, {@code <node>.protocol}, {@code <node>.object} or {@code <node>.value}.
 *
 * <pre>{@code
 * Designer d = new Designer();
 * var source = d.hostSource("source", myInput);
 * var driver = d.<String>thread("driver");
 * var shout  = d.push("shout", (String s) -> s + "!");
 * var sink   = d.hostSink("sink", myOutput);
 * d.wire(source.out(), driver.in());   // a pull wire
 * d.wire(driver.out(), shout.in());    // push wires
 * d.wire(shout.out(), sink.in());
 * Pipeline p = d.design().build(d.catalogue());
 * }</pre>
 *
 * A node whose methods take no typed argument ({@code thread}, {@code buffer}, {@code distributor},
 * {@code trashcan}) needs its item type written out, e.g. {@code d.<String>thread("driver")}: Java
 * cannot infer it from the wires made later, and a {@code ThreadNode<Object>} cannot push into a
 * {@code PushIn<String>}.
 *
 * <p>The compiler accepts only push-to-push and pull-to-pull wires; what it cannot see (a slot left
 * unwired, a single slot wired twice) is refused when the Design is built.
 */
public class Designer
{
	private final List<Design.Node> nodes = new ArrayList<>();
	private final List<Design.Wire> wires = new ArrayList<>();
	private final Set<String> ids = new HashSet<>();
	private final Catalogue catalogue;
	/** Prefixed to every automatic catalogue name: empty here, {@code <map>.} in a map's nested Designer. */
	private final String prefix;

	public Designer()
	{
		this(new Catalogue(), "");
	}

	/** A Designer writing into {@code catalogue}, its automatic names starting with {@code prefix}. */
	Designer(Catalogue catalogue, String prefix)
	{
		this.catalogue = catalogue;
		this.prefix = prefix;
	}

	/** The Design written so far, as plain data. */
	public Design design()
	{
		return new Design(nodes, wires);
	}

	/** A copy of the catalogue filled so far: the objects the Design names. */
	public Catalogue catalogue()
	{
		return new Catalogue(catalogue);
	}

	// --- wires ---

	/** A push wire: {@code from} (a slot holding an Output) will push into {@code to}. */
	public <E> void wire(PushOut<E> from, PushIn<? super E> to)
	{
		wires.add(new Design.Wire(from.port(), to.port()));
	}

	/** A pull wire: {@code to} (a slot holding an Input) will pull from {@code from}. */
	public <E> void wire(PullOut<? extends E> from, PullIn<E> to)
	{
		wires.add(new Design.Wire(from.port(), to.port()));
	}

	// --- kinds ---

	public <E> ThreadNode<E> thread(String id)
	{
		node(id, THREAD, Map.of());
		return new ThreadNode<>(id);
	}

	public <F, T> PushNode<F, T> push(String id, Function<F, T> function)
	{
		return push(id, auto(id, FUNCTION), function);
	}

	/** A push block whose function is stored in the catalogue under {@code name}. */
	public <F, T> PushNode<F, T> push(String id, String name, Function<F, T> function)
	{
		node(id, PUSH, named(id, FUNCTION, name, function));
		return new PushNode<>(id);
	}

	public <F, T> PullNode<F, T> pull(String id, Function<F, T> function)
	{
		return pull(id, auto(id, FUNCTION), function);
	}

	/** A pull block whose function is stored in the catalogue under {@code name}. */
	public <F, T> PullNode<F, T> pull(String id, String name, Function<F, T> function)
	{
		node(id, PULL, named(id, FUNCTION, name, function));
		return new PullNode<>(id);
	}

	public <E> IfBlockNode<E> ifBlock(String id, Predicate<E> predicate)
	{
		return ifBlock(id, auto(id, PREDICATE), predicate);
	}

	/** An if block whose predicate is stored in the catalogue under {@code name}. */
	public <E> IfBlockNode<E> ifBlock(String id, String name, Predicate<E> predicate)
	{
		node(id, IF_BLOCK, named(id, PREDICATE, name, predicate));
		return new IfBlockNode<>(id);
	}

	public <E> BufferNode<E> buffer(String id, int capacity)
	{
		node(id, BUFFER, Map.of(CAPACITY, capacity));
		return new BufferNode<>(id);
	}

	/** A constant; a value that is not a string, number or boolean is stored in the catalogue. */
	public <E> ConstantNode<E> constant(String id, E value)
	{
		node(id, CONSTANT, value(id, value));
		return new ConstantNode<>(id);
	}

	/** A variable holding {@code value} at first; stored like a constant's value. */
	public <E> VariableNode<E> variable(String id, E value)
	{
		node(id, VARIABLE, value(id, value));
		return new VariableNode<>(id);
	}

	public <E> DistributorNode<E> distributor(String id)
	{
		node(id, DISTRIBUTOR, Map.of());
		return new DistributorNode<>(id);
	}

	public <E> TrashcanNode<E> trashcan(String id)
	{
		node(id, TRASHCAN, Map.of());
		return new TrashcanNode<>(id);
	}

	public KeyboardNode keyboard(String id)
	{
		node(id, KEYBOARD, Map.of());
		return new KeyboardNode(id);
	}

	public ConsoleNode console(String id)
	{
		node(id, CONSOLE, Map.of());
		return new ConsoleNode(id);
	}

	/** A host's own Output object, stored in the catalogue as {@code <id>.object}. */
	public <E> HostSinkNode<E> hostSink(String id, Output<E> sink)
	{
		node(id, HOST_SINK, named(id, OBJECT, auto(id, OBJECT), sink));
		return new HostSinkNode<>(id);
	}

	/** A host's own Input object, stored in the catalogue as {@code <id>.object}. */
	public <E> HostSourceNode<E> hostSource(String id, Input<E> source)
	{
		node(id, HOST_SOURCE, named(id, OBJECT, auto(id, OBJECT), source));
		return new HostSourceNode<>(id);
	}

	public <E> PingPongNode<E> pingPong(String id, PingPongProtocol<?, E, ?> protocol)
	{
		return pingPong(id, auto(id, PROTOCOL), protocol);
	}

	/** A ping-pong block whose protocol is stored in the catalogue under {@code name}. */
	public <E> PingPongNode<E> pingPong(String id, String name, PingPongProtocol<?, E, ?> protocol)
	{
		node(id, PING_PONG, named(id, PROTOCOL, name, protocol));
		return new PingPongNode<>(id);
	}

	/**
	 * A map: sorts the items pushed in by {@code key} and, on the first item with an unseen key, builds a
	 * fresh copy of the nested Design {@code perKey} writes, for that key. The copy's threads start with
	 * the pipeline and stop with it, or when its handler is removed from the map.
	 *
	 * <pre>{@code
	 * var peers = d.map("peers", (String msg) -> msg.split(":")[0], perKey -> {
	 *     var show = perKey.hostSinkPerKey("show", key -> new SimpleHandler(key));
	 *     perKey.wire(perKey.entry(), show.in());
	 * });
	 * }</pre>
	 *
	 * Node ids inside the nested Design are its own; its automatic catalogue names start with
	 * {@code <id>.}, e.g. {@code peers.show.object}.
	 */
	public <K, E> MapNode<K, E> map(String id, Function<E, K> key, Consumer<PerKeyDesigner<K, E>> perKey)
	{
		return map(id, auto(id, FUNCTION), key, perKey);
	}

	/** A map whose key function is stored in the catalogue under {@code name}. */
	public <K, E> MapNode<K, E> map(String id, String name, Function<E, K> key, Consumer<PerKeyDesigner<K, E>> perKey)
	{
		requireFree(id);
		PerKeyDesigner<K, E> nested = new PerKeyDesigner<>(catalogue, prefix + id + ".");
		perKey.accept(nested);
		Map<String, Object> settings = new HashMap<>(named(id, FUNCTION, name, key));
		settings.put(PER_KEY, nested.design());
		node(id, MAP, settings);
		return new MapNode<>(id);
	}

	// --- recording ---

	/** The automatic catalogue name for {@code key} of node {@code id}. */
	final String auto(String id, String key)
	{
		return prefix + id + "." + key;
	}

	final void node(String id, String kind, Map<String, Object> settings)
	{
		requireFree(id);
		ids.add(id);
		nodes.add(new Design.Node(id, kind, settings));
	}

	private void requireFree(String id)
	{
		Objects.requireNonNull(id, "id");
		if (ids.contains(id))
		{
			throw new IllegalArgumentException("node id '" + id + "' is already used");
		}
	}

	/** Puts {@code object} in the catalogue under {@code name}; refused before anything changes. */
	final Map<String, Object> named(String id, String key, String name, Object object)
	{
		requireFree(id);
		catalogue.put(name, object);
		return Map.of(key, name);
	}

	private Map<String, Object> value(String id, Object value)
	{
		if (isPlain(value))
		{
			return Map.of(VALUE, value);
		}
		return named(id, VALUE_NAME, auto(id, VALUE), value);
	}
}
