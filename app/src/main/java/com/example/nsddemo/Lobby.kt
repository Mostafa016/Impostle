package com.example.nsddemo


abstract class Lobby(gameState: GameState, players: List<Player>) {
    protected lateinit var score: Map<Player, Int>
}

class ClientLobby(gameState: GameState, players: List<Player>, serverIPAddress: String) :
    Lobby(gameState, players) {

}

class ServerLobby(
    gameState: GameState,
    players: List<Player>,
    playerIPAddresses: Map<Player, String>
) : Lobby(gameState, players) {

}