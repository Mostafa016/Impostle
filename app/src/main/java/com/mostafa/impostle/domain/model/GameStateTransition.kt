package com.mostafa.impostle.domain.model

sealed class GameStateTransition(
    open val envelopes: List<Envelope> = emptyList(),
) {
    data class Valid(
        val newGameData: GameData,
        val newPhase: GamePhase? = null,
        override val envelopes: List<Envelope> = emptyList(),
    ) : GameStateTransition(envelopes)

    data class Invalid(
        val reason: String,
        override val envelopes: List<Envelope> = emptyList(),
    ) : GameStateTransition(envelopes)
}

sealed interface Envelope {
    data class Broadcast(
        val message: ServerMessage,
    ) : Envelope

    data class Unicast(
        val recipientId: String,
        val message: ServerMessage,
    ) : Envelope
}
