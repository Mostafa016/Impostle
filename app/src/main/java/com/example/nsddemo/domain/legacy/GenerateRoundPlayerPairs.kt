package com.example.nsddemo.domain.legacy

import com.example.nsddemo.domain.model.Player


/**
 * Generates a [List] of [Pair]s of [Player]s such that each [Player] asks and is asked exactly once.
 */
fun generateRoundPlayerPairs(askingPlayers: List<Player>): List<Pair<Player, Player>> {
    val askedPlayers = askingPlayers.toMutableList()
    val chosenAskingPlayers = mutableListOf<Player>()
    return askingPlayers.shuffled().mapIndexed { i, askingPlayer ->
        chosenAskingPlayers.add(askingPlayer)
        val askingPlayerIndex = askedPlayers.indexOf(askingPlayer)
        val askedPlayer = if (i == askingPlayers.lastIndex - 1) {
            val commonPlayers = askedPlayers.filter { it !in chosenAskingPlayers }
            if (commonPlayers.size == 1) {
                // To handle the following case: A B C D
                // Wrong Choice: D to B, B to A, {A to D}, C to C
                // Correct Choice: D to B, B to A, {A to C}, C to D
                // {} means indicates the pair choice that can lead to an incorrect last pair
                askedPlayers.find { it == commonPlayers.first() }!!
            } else {
                askedPlayers.find { it != askingPlayer }!!
            }
        } else {
            askedPlayers.filterIndexed { j, _ -> j != askingPlayerIndex }.random()
        }
        askedPlayers.remove(askedPlayer)
        return@mapIndexed (askingPlayer to askedPlayer)
    }
}
