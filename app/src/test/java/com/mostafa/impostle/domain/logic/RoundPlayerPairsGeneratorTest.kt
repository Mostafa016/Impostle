package com.mostafa.impostle.domain.logic

import com.mostafa.impostle.domain.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoundPlayerPairsGeneratorTest {
    private fun createPlayers(count: Int): List<Player> =
        (1..count).map {
            Player(id = "p$it", name = "Player $it", color = "", isConnected = true)
        }

    @Test
    fun `GIVEN 2 players WHEN generate THEN returns simple swap`() {
        val players = createPlayers(2)
        val pairs = RoundPlayerPairsGenerator.generate(players)

        assertEquals(2, pairs.size)

        // Validation: No self-asking
        pairs.forEach { (asker, asked) ->
            assertNotEquals("Player cannot ask themselves", asker, asked)
        }

        // Validation: Unique Constraints
        val askers = pairs.map { it.first }.toSet()
        val asked = pairs.map { it.second }.toSet()

        assertEquals("All players must ask", 2, askers.size)
        assertEquals("All players must be asked", 2, asked.size)
    }

    @Test
    fun `GIVEN 5 players WHEN generate THEN returns valid chain`() {
        val players = createPlayers(5)
        val pairs = RoundPlayerPairsGenerator.generate(players)

        assertEquals("Should produce 5 pairs", 5, pairs.size)

        // 1. Check Self-Reflexivity
        pairs.forEach { (asker, asked) ->
            assertNotEquals("Self-asking detected for ${asker.name}", asker, asked)
        }

        // 2. Check Coverage (Everyone asks exactly once)
        val askers = pairs.map { it.first.id }
        val distinctAskers = askers.toSet()
        assertEquals("Duplicate askers found", 5, distinctAskers.size)
        assertTrue(askers.containsAll(players.map { it.id }))

        // 3. Check Coverage (Everyone is asked exactly once)
        val asked = pairs.map { it.second.id }
        val distinctAsked = asked.toSet()
        assertEquals("Duplicate targets found", 5, distinctAsked.size)
        assertTrue(asked.containsAll(players.map { it.id }))
    }

    @Test
    fun `GIVEN many iterations WHEN generate THEN always valid`() {
        // Run 100 times to ensure random shuffling doesn't produce an edge case crash
        // or invalid state (like the last pair being self-referential).
        val players = createPlayers(4)

        repeat(100) {
            val pairs = RoundPlayerPairsGenerator.generate(players)

            assertEquals(4, pairs.size)
            pairs.forEach { (asker, asked) ->
                assertNotEquals(asker, asked)
            }
        }
    }
}
