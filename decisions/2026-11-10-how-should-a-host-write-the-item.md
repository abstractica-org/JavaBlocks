---
id: 2026-11-10-how-should-a-host-write-the-item
title: How should a host write the item type of a thread, buffer, distributor or trashcan node?
status: ruled
stakes: low
confidence: 80
opened: 2026-11-10
ruled: 2026-11-11
repo: javablocks
by: d50818be-0fda-494c-b996-7f3746f145f0
recommends: write-the-type-in-angle-brackets-as-built
---

## Problem

The pipeline builder (the Designer, goal 2) is built with the names you ruled. One line of the sketch you ruled on does not compile as written: `var driver = d.thread("driver"); d.wire(driver.out(), ping.in());`. A thread node carries the type of the items it moves, so that the compiler can refuse a wire that pushes the wrong type, which is why the ruling chose four typed port types. When the node is created, though, Java has nothing to infer the type from: `thread("driver")` takes no typed argument, and Java never looks ahead at later lines. So it picks the widest type, Object, and then refuses the wire, because a thread of Objects cannot push into a block that takes Strings. Push, pull, if, constant, variable, host and ping-pong nodes don't have this problem, because their argument (the function, the value, the object) gives the type. Only four kinds do: thread, buffer, distributor and trashcan.

As built, a host writes the type out: `var driver = d.<String>thread("driver");`. Every test and the Designer's documentation do this. The run kept it rather than loosen the types, and asks you here because the call syntax is part of the public shape.

## Options

### Write the type in angle brackets, as built

**Pros:**
- Already built, tested and documented: `d.<String>thread("driver")`, `d.<Integer>buffer("queue", 10)`.
- Works for every type, including generic ones such as `List<String>`.
- Keeps the compile-time push/pull type safety the ruling chose four port types for.

**Cons:**
- The angle-bracket syntax before the method name is unfamiliar to many Java programmers.
- It differs from the sketch you ruled on.

### Pass the type as an argument

**Pros:**
- Reads more naturally: `d.thread("driver", String.class)`.
- Same type safety.

**Cons:**
- Fails for generic types: there is no `List<String>.class`, so those still need the angle brackets or an unchecked cast.
- An argument that does nothing at run time, only to steer the compiler.
- One more overload per kind, or the angle-bracket form goes away.

### Untyped wires: the sketch compiles as written

**Pros:**
- Exactly the sketch: `d.thread("driver")` with no type.
- The shortest host code.

**Cons:**
- The wire methods would accept any push-to-push pair whatever its item types, so pushing Strings into a block that expects Longs compiles and fails at run time inside a worker thread.
- Gives up part of what the names ruling chose four port types for. Push-versus-pull is still checked, item types are not.

## Recommendation

Keep the angle brackets, as built. It is the only option that keeps full type safety for every type, generic ones included, and it costs nothing more to build. Passing a Class reads better for simple types but breaks down for generic ones and adds a dead argument. Untyped wires make the sketch compile, but they bring back the run-time failures in worker threads that the typed ports exist to prevent. The unfamiliar syntax shows up only on four kinds, and the Designer's documentation explains it. Confidence 80. If you'd rather have the Class argument as an extra overload beside the angle brackets, say so in the ruling and a run adds it.

## If nothing is decided

The builder stays as built: hosts write `d.<String>thread("driver")` for those four kinds. Nothing is blocked.

## Ruling

**Write the type in angle brackets, as built** (`write-the-type-in-angle-brackets-as-built`). Tog, 2026-11-11, from the project pages.
