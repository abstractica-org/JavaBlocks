package org.abstractica.javablocks.blocks.basic.impl;

import org.abstractica.javablocks.blocks.basic.MapBlock;
import org.abstractica.javablocks.blocks.basic.MapHandlerBlock;
import org.abstractica.javablocks.blocks.basic.MapHandlerBlockFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapBlockImpl<Key, E, Handler extends MapHandlerBlock<Key, E>>
		extends AbstractBlock
		implements MapBlock<Key, E, Handler>
{
	private final MapHandlerBlockFactory<Key, E, Handler> handlerFactory;
	private final Map<Key, Handler> map;

	public MapBlockImpl(MapHandlerBlockFactory<Key, E, Handler> handlerFactory)
	{
		this.handlerFactory = handlerFactory;
		this.map = new HashMap<>();
	}

	@Override
	public int getSize()
	{
		return map.size();
	}

	@Override
	public Handler removeHandler(Key key)
	{
		Handler handler;
		synchronized (map)
		{
			handler = map.remove(key);
		}
		if(handler != null)
		{
			handlerFactory.destroyHandlerBlock(handler);
		}
		return handler;
	}

	@Override
	public Key extractKey(E item)
	{
		return handlerFactory.extractKey(item);
	}

	@Override
	public Iterable<Handler> getAllHandlers()
	{
		synchronized (map)
		{
			int size = map.size();
			List<Handler> handlers = new ArrayList<>(size);
			for(Handler handler : map.values())
			{
				handlers.add(handler);
			}
			return handlers;
		}
	}

	@Override
	public void put(E item) throws Exception
	{
		Key key = handlerFactory.extractKey(item);
		Handler handler;
		synchronized(map)
		{
			handler = map.get(key);
			if(handler == null)
			{
				handler = handlerFactory.createHandlerBlockFor(key);
				map.put(key, handler);
			}
		}
		handler.put(item);
	}
}
