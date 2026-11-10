package org.abstractica.javablocks.design;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The Java objects a {@link Design} refers to by name: functions, predicates, protocols, host
 * objects, and constant values that are not plain data.
 */
public final class Catalogue
{
	private final Map<String, Object> entries = new LinkedHashMap<>();

	public Catalogue()
	{
	}

	/** A copy of {@code other}. */
	public Catalogue(Catalogue other)
	{
		entries.putAll(other.entries);
	}

	/**
	 * Adds {@code object} under {@code name}. Adding the same object again is harmless.
	 *
	 * @throws IllegalArgumentException if {@code name} already stands for a different object
	 */
	public Catalogue put(String name, Object object)
	{
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(object, "object");
		Object old = entries.get(name);
		if (old != null && old != object)
		{
			throw new IllegalArgumentException("catalogue name '" + name + "' already stands for another object");
		}
		entries.put(name, object);
		return this;
	}

	public boolean contains(String name)
	{
		return entries.containsKey(name);
	}

	/**
	 * The object under {@code name}.
	 *
	 * @throws IllegalArgumentException if there is none
	 */
	public Object get(String name)
	{
		Object o = entries.get(name);
		if (o == null)
		{
			throw new IllegalArgumentException("no catalogue entry '" + name + "'");
		}
		return o;
	}

	public Set<String> names()
	{
		return Collections.unmodifiableSet(entries.keySet());
	}
}
