package org.abstractica.javablocks.design;

import org.abstractica.javablocks.blocks.basic.Input;
import org.abstractica.javablocks.blocks.basic.Output;
import org.abstractica.javablocks.blocks.connection.PingPongProtocol;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * var driver = d.thread("driver");
 * var shout  = d.push("shout", (String s) -> s + "!");
 * var sink   = d.hostSink("sink", myOutput);
 * d.wire(source.out(), driver.in());   // a pull wire
 * d.wire(driver.out(), shout.in());    // push wires
 * d.wire(shout.out(), sink.in());
 * Pipeline p = d.design().build(d.catalogue());
 * }</pre>
 *
 * The compiler accepts only push-to-push and pull-to-pull wires; what it cannot see (a slot left
 * unwired, a single slot wired twice) is refused when the Design is built.
 */
public final class Designer
{
	private final List<Design.Node> nodes = new ArrayList<>();
	private final List<Design.Wire> wires = new ArrayList<>();
	private final Set<String> ids = new HashSet<>();
	private final Catalogue catalogue = new Catalogue();

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
		return push(id, id + "." + FUNCTION, function);
	}

	/** A push block whose function is stored in the catalogue under {@code name}. */
	public <F, T> PushNode<F, T> push(String id, String name, Function<F, T> function)
	{
		node(id, PUSH, named(id, FUNCTION, name, function));
		return new PushNode<>(id);
	}

	public <F, T> PullNode<F, T> pull(String id, Function<F, T> function)
	{
		return pull(id, id + "." + FUNCTION, function);
	}

	/** A pull block whose function is stored in the catalogue under {@code name}. */
	public <F, T> PullNode<F, T> pull(String id, String name, Function<F, T> function)
	{
		node(id, PULL, named(id, FUNCTION, name, function));
		return new PullNode<>(id);
	}

	public <E> IfBlockNode<E> ifBlock(String id, Predicate<E> predicate)
	{
		return ifBlock(id, id + "." + PREDICATE, predicate);
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
		node(id, HOST_SINK, named(id, OBJECT, id + "." + OBJECT, sink));
		return new HostSinkNode<>(id);
	}

	/** A host's own Input object, stored in the catalogue as {@code <id>.object}. */
	public <E> HostSourceNode<E> hostSource(String id, Input<E> source)
	{
		node(id, HOST_SOURCE, named(id, OBJECT, id + "." + OBJECT, source));
		return new HostSourceNode<>(id);
	}

	public <E> PingPongNode<E> pingPong(String id, PingPongProtocol<?, E, ?> protocol)
	{
		return pingPong(id, id + "." + PROTOCOL, protocol);
	}

	/** A ping-pong block whose protocol is stored in the catalogue under {@code name}. */
	public <E> PingPongNode<E> pingPong(String id, String name, PingPongProtocol<?, E, ?> protocol)
	{
		node(id, PING_PONG, named(id, PROTOCOL, name, protocol));
		return new PingPongNode<>(id);
	}

	// --- recording ---

	private void node(String id, String kind, Map<String, Object> settings)
	{
		requireFree(id);
		ids.add(id);
		nodes.add(new Design.Node(id, kind, settings));
	}

	private void requireFree(String id)
	{
		if (ids.contains(id))
		{
			throw new IllegalArgumentException("node id '" + id + "' is already used");
		}
	}

	/** Puts {@code object} in the catalogue under {@code name}; refused before anything changes. */
	private Map<String, Object> named(String id, String key, String name, Object object)
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
		return named(id, VALUE_NAME, id + "." + VALUE, value);
	}
}
