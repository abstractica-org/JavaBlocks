package org.abstractica.javablocks.blocks.basic;

import org.abstractica.javablocks.testsupport.Sink;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Characterizes {@code v0.2.0} IfBlock behaviour — exclusive routing by predicate. */
class IfBlockCharacterizationTest
{
	@Test
	void routesByPredicateOnTheCallersThread() throws InterruptedException
	{
		Sink<Integer> evens = new Sink<>();
		Sink<Integer> odds = new Sink<>();
		IfBlock<Integer> ifBlock = BasicBlocks.getIfBlock(n -> n % 2 == 0);
		ifBlock.setTrueOutput(evens);
		ifBlock.setFalseOutput(odds);

		for (int i = 1; i <= 5; i++) ifBlock.put(i);

		assertEquals(List.of(2, 4), evens.items());
		assertEquals(List.of(1, 3, 5), odds.items());
		assertTrue(evens.threads().stream().allMatch(t -> t == Thread.currentThread()));
	}

	@Test
	void throwsWhenHalfConnectedEvenIfTheConnectedSideWouldBeTaken()
	{
		Sink<Integer> evens = new Sink<>();
		IfBlock<Integer> ifBlock = BasicBlocks.getIfBlock(n -> n % 2 == 0);
		ifBlock.setTrueOutput(evens);

		RuntimeException e = assertThrows(RuntimeException.class, () -> ifBlock.put(2));
		assertEquals("Block not fully connected!", e.getMessage());
		assertEquals(List.of(), evens.items());
	}

	@Test
	void throwsWhenUnconnected()
	{
		IfBlock<Integer> ifBlock = BasicBlocks.getIfBlock(n -> true);
		assertThrows(RuntimeException.class, () -> ifBlock.put(1));
	}
}
