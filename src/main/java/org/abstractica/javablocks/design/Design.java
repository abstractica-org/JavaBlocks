package org.abstractica.javablocks.design;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A pipeline described as plain data: nodes (an id, a kind and settings) and wires from port to
 * port. It holds no Java objects: a function, predicate, protocol or host object appears only as a
 * name, and {@link #build(Catalogue)} looks the names up in a {@link Catalogue}.
 *
 * <p>A Design can be written by a {@link Designer} or by hand (or read from elsewhere); either way
 * {@code build} checks it in full and refuses it, listing every problem, before anything is made.
 *
 * @param nodes the nodes, in the order they are built and their threads started
 * @param wires the wires, each in the direction data flows
 */
public record Design(List<Node> nodes, List<Wire> wires)
{
	public Design
	{
		nodes = List.copyOf(nodes);
		wires = List.copyOf(wires);
	}

	/**
	 * Builds the pipeline this Design describes, through the library's existing factories.
	 *
	 * @param catalogue the Java objects the Design names
	 * @throws IllegalArgumentException listing every problem, if the Design or the catalogue is not
	 *                                  buildable
	 */
	public Pipeline build(Catalogue catalogue)
	{
		return PipelineBuilder.build(this, catalogue);
	}

	/**
	 * A block or host object.
	 *
	 * @param id       unique within the Design
	 * @param kind     one of the kinds, e.g. {@code "thread"}, {@code "push"}, {@code "buffer"}
	 * @param settings plain data only: strings, numbers, booleans; catalogue names are strings
	 */
	public record Node(String id, String kind, Map<String, Object> settings)
	{
		public Node
		{
			Objects.requireNonNull(id, "id");
			Objects.requireNonNull(kind, "kind");
			settings = Map.copyOf(settings);
		}
	}

	/**
	 * A connection, in the direction data flows: a push wire from the slot that holds an Output to
	 * the block that is one; a pull wire from the block that is an Input to the slot that holds one.
	 */
	public record Wire(Port from, Port to)
	{
		public Wire
		{
			Objects.requireNonNull(from, "from");
			Objects.requireNonNull(to, "to");
		}

		@Override
		public String toString()
		{
			return from + " -> " + to;
		}
	}

	/** A named connection point of a node, e.g. {@code driver.out}. */
	public record Port(String node, String name)
	{
		public Port
		{
			Objects.requireNonNull(node, "node");
			Objects.requireNonNull(name, "name");
		}

		@Override
		public String toString()
		{
			return node + "." + name;
		}
	}
}
