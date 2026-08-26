package org.abstractica.javablocks.blocks.basic;

import org.abstractica.javablocks.blocks.basic.impl.AbstractBlock;
import org.abstractica.javablocks.testsupport.Async;
import org.abstractica.javablocks.testsupport.BlockingSink;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterizes {@code v0.2.0} MapBlock behaviour: demultiplex by key into handlers a factory
 * creates on the first message with an unseen key, on the caller's thread; never destroyed unasked.
 */
class MapBlockCharacterizationTest
{
	/** A handler that records what it received; for one key it can be made to block. */
	static final class Handler extends AbstractBlock implements MapHandlerBlock<String, String>
	{
		final String key;
		final List<String> received = new CopyOnWriteArrayList<>();
		final BlockingSink<String> gate; // null unless this handler blocks

		Handler(String key, BlockingSink<String> gate)
		{
			this.key = key;
			this.gate = gate;
		}

		@Override
		public String getKey()
		{
			return key;
		}

		@Override
		public void put(String item) throws InterruptedException
		{
			if (gate != null) gate.put(item);
			received.add(item);
		}
	}

	/** Messages are "key:payload"; records every create/destroy and the thread that created. */
	static final class Factory implements MapHandlerBlockFactory<String, String, Handler>
	{
		final List<Handler> created = new CopyOnWriteArrayList<>();
		final List<Thread> creatingThreads = new CopyOnWriteArrayList<>();
		final List<Handler> destroyed = new CopyOnWriteArrayList<>();
		String blockingKey;
		BlockingSink<String> gate;

		@Override
		public String extractKey(String item)
		{
			return item.substring(0, item.indexOf(':'));
		}

		@Override
		public Handler createHandlerBlockFor(String key)
		{
			Handler h = new Handler(key, key.equals(blockingKey) ? gate : null);
			created.add(h);
			creatingThreads.add(Thread.currentThread());
			return h;
		}

		@Override
		public void destroyHandlerBlock(Handler handler)
		{
			destroyed.add(handler);
		}
	}

	@Test
	void createsOneHandlerPerNewKeyOnTheCallersThreadAndReusesIt() throws InterruptedException
	{
		Factory factory = new Factory();
		MapBlock<String, String, Handler> map = BasicBlocks.getMapBlock(factory);
		assertEquals(0, map.getSize());

		map.put("a:1");
		map.put("a:2");
		map.put("b:1");
		map.put("a:3");

		assertEquals(2, factory.created.size(), "one handler per distinct key");
		assertEquals(2, map.getSize());
		assertTrue(factory.creatingThreads.stream().allMatch(t -> t == Thread.currentThread()));
		assertEquals(List.of("a", "b"), factory.created.stream().map(Handler::getKey).toList());
		assertEquals(List.of("a:1", "a:2", "a:3"), factory.created.get(0).received);
		assertEquals(List.of("b:1"), factory.created.get(1).received);
		assertEquals(List.of(), factory.destroyed, "nothing is destroyed unasked");
		assertEquals("b", map.extractKey("b:9"), "extractKey delegates to the factory");
	}

	@Test
	void getAllHandlersIsASnapshot() throws InterruptedException
	{
		Factory factory = new Factory();
		MapBlock<String, String, Handler> map = BasicBlocks.getMapBlock(factory);
		map.put("a:1");
		map.put("b:1");

		List<Handler> snapshot = new ArrayList<>();
		map.getAllHandlers().forEach(snapshot::add);
		assertEquals(2, snapshot.size());
		assertTrue(snapshot.containsAll(factory.created));

		((List<?>) map.getAllHandlers()).clear();
		assertEquals(2, map.getSize(), "clearing the returned list does not touch the map");
	}

	@Test
	void removeHandlerDestroysThroughTheFactoryAndOnlyThen() throws InterruptedException
	{
		Factory factory = new Factory();
		MapBlock<String, String, Handler> map = BasicBlocks.getMapBlock(factory);
		map.put("a:1");
		map.put("b:1");

		assertNull(map.removeHandler("zzz"), "unknown key: null, no destroy");
		assertEquals(List.of(), factory.destroyed);

		Handler removed = map.removeHandler("a");
		assertSame(factory.created.get(0), removed);
		assertEquals(List.of(removed), factory.destroyed);
		assertEquals(1, map.getSize());

		map.put("a:2");
		assertEquals(3, factory.created.size(), "a removed key gets a fresh handler next time");
		assertNotSame(removed, factory.created.get(2));
	}

	@Test
	void handlerPutRunsOutsideTheMapLockSoOtherKeysProceed() throws InterruptedException
	{
		Factory factory = new Factory();
		factory.blockingKey = "slow";
		factory.gate = new BlockingSink<>();
		MapBlock<String, String, Handler> map = BasicBlocks.getMapBlock(factory);

		Thread slowProducer = Async.daemon(() -> map.put("slow:1"));
		assertTrue(factory.gate.enteredWithin(1000), "handler 'slow' is blocked inside put");

		Thread fastProducer = Async.daemon(() -> map.put("fast:1"));
		assertTrue(Async.finishesWithin(fastProducer, 1000), "a different key is not held up");
		assertEquals(2, map.getSize());

		factory.gate.release();
		assertTrue(Async.finishesWithin(slowProducer, 1000));
		assertEquals(List.of("slow:1"), factory.created.get(0).received);
	}
}
