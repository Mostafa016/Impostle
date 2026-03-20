package com.mostafa.impostle.domain.logic

import com.mostafa.impostle.domain.model.Player

object RoundPlayerPairsGenerator {
    fun generate(players: List<Player>): List<Pair<Player, Player>> {
        val playerCount = players.size

        if (playerCount < MIN_NUMBER_TO_GENERATE_PAIRS) {
            throw IllegalArgumentException("Requires at least two players to generate pairs (found $playerCount).")
        }

        val shuffledPlayers = players.shuffled()
        val askedPlayers = shuffledPlayers.drop(1) + shuffledPlayers.first()

        return shuffledPlayers.zip(askedPlayers)
    }

    private const val MIN_NUMBER_TO_GENERATE_PAIRS = 2
}
