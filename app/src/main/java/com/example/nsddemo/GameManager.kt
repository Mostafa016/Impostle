package com.example.nsddemo

class GameManager(gameRepository: GameRepository) {
    fun startRound(): GameData {
        return GameData()
    }
}