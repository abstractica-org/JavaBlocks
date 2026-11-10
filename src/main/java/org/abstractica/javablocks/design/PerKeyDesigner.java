package org.abstractica.javablocks.design;

import java.util.Map;
import java.util.function.Function;

import org.abstractica.javablocks.blocks.basic.Output;

import static org.abstractica.javablocks.design.Kinds.*;

/**
 * Writes a map's nested Design: the pipeline built once for each key. Besides every kind a
 * {@link Designer} has, it has {@link #entry()}, where this key's messages arrive, and
 * {@link #hostSinkPerKey}, a host object made from the key.
 *
 * @param <K> the map's key type
 * @param <E> the type of the items the map sorts
 */
public final class PerKeyDesigner<K, E> extends Designer
{
	PerKeyDesigner(Catalogue catalogue, String prefix)
	{
		super(catalogue, prefix);
		node(ENTRY, ENTRY, Map.of());
	}

	/** This key's messages, as the map receives them. Must be wired. */
	public PushOut<E> entry()
	{
		return new PushOut<>(new Design.Port(ENTRY, "out"));
	}

	/**
	 * A host Output made from the key, once for each copy: {@code make} is called with the key when the
	 * map first sees it. Stored in the catalogue as {@code <map>.<id>.object}.
	 */
	public <T> HostSinkNode<T> hostSinkPerKey(String id, Function<? super K, ? extends Output<T>> make)
	{
		node(id, HOST_SINK_PER_KEY, named(id, OBJECT, auto(id, OBJECT), make));
		return new HostSinkNode<>(id);
	}
}
