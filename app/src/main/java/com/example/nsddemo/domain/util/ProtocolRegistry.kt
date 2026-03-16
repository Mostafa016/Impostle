package com.example.nsddemo.domain.util

import com.example.nsddemo.domain.model.ClientMessage
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.Message
import com.example.nsddemo.domain.model.ServerMessage
import kotlin.reflect.KClass

object ProtocolRegistry {
    // The Map allows O(1) lookup during logging
    private val definitions =
        mapOf<KClass<out Message>, MessageDefinition>(
            // =================================================================
            // PHASE 1: LOBBY & SETUP
            // =================================================================
            ClientMessage.RegisterPlayer::class to
                MessageDefinition(
                    code = "C_REG",
                    description = "Player requests to join lobby or reconnect",
                    direction = MessageDirection.ClientToServer,
                    expectedPhase = GamePhase.Lobby,
                ),
            ServerMessage.PlayerList::class to
                MessageDefinition(
                    code = "S_LST",
                    description = "Host broadcasts updated player list",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = GamePhase.Lobby,
                ),
            ClientMessage.RequestKickPlayer::class to
                MessageDefinition(
                    code = "C_KCK",
                    description = "Host requests to kick player (in lobby or while paused)",
                    direction = MessageDirection.ClientToServer,
                    expectedPhase = GamePhase.Lobby,
                ),
            ClientMessage.RequestSelectCategory::class to
                MessageDefinition(
                    code = "C_CAT",
                    description = "Host requests to change category",
                    direction = MessageDirection.ClientToServer,
                    expectedPhase = GamePhase.Lobby,
                ),
            ServerMessage.CategorySelected::class to
                MessageDefinition(
                    code = "S_CAT",
                    description = "Server confirms category change",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = GamePhase.Lobby,
                ),
            ClientMessage.RequestStartGame::class to
                MessageDefinition(
                    code = "C_STRT",
                    description = "Host requests to start the game",
                    direction = MessageDirection.ClientToServer,
                    expectedPhase = GamePhase.Lobby,
                ),
            // =================================================================
            // PHASE 2: ROLE DISTRIBUTION
            // =================================================================
            ServerMessage.RoleAssigned::class to
                MessageDefinition(
                    code = "S_ROLE", // Acts as the "Game Started" signal
                    description = "Server assigns Word/Imposter roles (Unicast)",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = GamePhase.RoleDistribution,
                ),
            ClientMessage.ConfirmRoleReceived::class to
                MessageDefinition(
                    code = "C_RDY",
                    description = "Player confirms they saw their role",
                    direction = MessageDirection.ClientToServer,
                    expectedPhase = GamePhase.RoleDistribution,
                ),
            ServerMessage.PlayerReady::class to
                MessageDefinition(
                    code = "S_RDY",
                    description = "Server updates count of ready players",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = GamePhase.RoleDistribution,
                ),
            // =================================================================
            // PHASE 3: GAMEPLAY (IN ROUND)
            // =================================================================
            ServerMessage.Question::class to
                MessageDefinition(
                    code = "S_QST",
                    description = "Server assigns next asker/asked pair",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = GamePhase.InRound,
                ),
            ClientMessage.EndTurn::class to
                MessageDefinition(
                    code = "C_TRN",
                    description = "Active player finishes their question/turn",
                    direction = MessageDirection.ClientToServer,
                    expectedPhase = GamePhase.InRound,
                ),
            ServerMessage.RoundEnd::class to
                MessageDefinition(
                    code = "S_RND",
                    description = "Server declares the round finished",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = GamePhase.InRound, // Transition to ReplayChoice
                ),
            // =================================================================
            // PHASE 4: ROUND REPLAY CHOICE
            // =================================================================
            ClientMessage.RequestReplayRound::class to
                MessageDefinition(
                    code = "C_RPLR",
                    description = "Host requests another round of questions",
                    direction = MessageDirection.ClientToServer,
                    expectedPhase = GamePhase.RoundReplayChoice,
                ),
            ServerMessage.ReplayRound::class to
                MessageDefinition(
                    code = "S_RPLR",
                    description = "Server confirms new round starting",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = GamePhase.RoundReplayChoice,
                ),
            ClientMessage.RequestStartVote::class to
                MessageDefinition(
                    code = "C_REQV",
                    description = "Host requests to proceed to voting",
                    direction = MessageDirection.ClientToServer,
                    expectedPhase = GamePhase.RoundReplayChoice,
                ),
            ServerMessage.StartVote::class to
                MessageDefinition(
                    code = "S_VOTE",
                    description = "Server triggers voting phase",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = GamePhase.RoundReplayChoice,
                ),
            // =================================================================
            // PHASE 5: VOTING & IMPOSTER GUESS
            // =================================================================
            ClientMessage.SubmitVote::class to
                MessageDefinition(
                    code = "C_VOTE",
                    description = "Player submits a vote for a suspect",
                    direction = MessageDirection.ClientToServer,
                    expectedPhase = GamePhase.GameVoting,
                ),
            ServerMessage.PlayerVoted::class to
                MessageDefinition(
                    code = "S_VOTD",
                    description = "Server notifies that a player has voted",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = GamePhase.GameVoting,
                ),
            ServerMessage.VotesAfterLeaver::class to
                MessageDefinition(
                    code = "S_VUPT",
                    description = "Server updates votes list after a player is kicked to avoid invalid votes",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = GamePhase.Paused,
                ),
            ServerMessage.ScoresAfterLeaver::class to
                MessageDefinition(
                    code = "S_SUPT",
                    description = "Server updates scores list after a player is kicked to avoid invalid scores",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = GamePhase.Paused,
                ),
            ServerMessage.VoteResult::class to
                MessageDefinition(
                    code = "S_RES",
                    description = "Server reveals imposter and scores",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = GamePhase.GameVoting, // Transition to Results
                ),
            ServerMessage.StartImposterGuess::class to
                MessageDefinition(
                    code = "S_IGSS",
                    description = "Server starts imposter guess phase with word options",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = GamePhase.ImposterGuess,
                ),
            ClientMessage.SubmitImposterGuess::class to
                MessageDefinition(
                    code = "C_IGSS",
                    description = "Imposter submits guessed word",
                    direction = MessageDirection.ClientToServer,
                    expectedPhase = GamePhase.ImposterGuess,
                ),
            // =================================================================
            // PHASE 6: RESULTS & END GAME CHOICE
            // =================================================================
            ClientMessage.RequestContinueToGameChoice::class to
                MessageDefinition(
                    code = "C_CONT",
                    description = "Host proceeds from Scoreboard to Choice screen",
                    direction = MessageDirection.ClientToServer,
                    expectedPhase = GamePhase.GameResults,
                ),
            ServerMessage.ContinueToGameChoice::class to
                MessageDefinition(
                    code = "S_CONT",
                    description = "Server moves everyone to Choice screen",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = GamePhase.GameResults,
                ),
            ClientMessage.RequestReplayGame::class to
                MessageDefinition(
                    code = "C_RPLG",
                    description = "Host requests to restart game (keep scores)",
                    direction = MessageDirection.ClientToServer,
                    expectedPhase = GamePhase.GameReplayChoice,
                ),
            ServerMessage.ReplayGame::class to
                MessageDefinition(
                    code = "S_RPLG",
                    description = "Server resets game to Lobby",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = GamePhase.GameReplayChoice,
                ),
            ClientMessage.RequestEndGame::class to
                MessageDefinition(
                    code = "C_END",
                    description = "Host ends session entirely",
                    direction = MessageDirection.ClientToServer,
                    expectedPhase = GamePhase.GameReplayChoice,
                ),
            ServerMessage.EndGame::class to
                MessageDefinition(
                    code = "S_END",
                    description = "Server terminates session",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = GamePhase.GameReplayChoice,
                ),
            // =================================================================
            // SYSTEM / ASYNC MESSAGES
            // =================================================================
            ServerMessage.PlayerReconnected::class to
                MessageDefinition(
                    code = "S_RECN",
                    description = "Notification that a player re-joined",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = null, // Can happen anytime during game
                ),
            ServerMessage.PlayerDisconnected::class to
                MessageDefinition(
                    code = "S_DISC",
                    description = "Notification that a player lost connection",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = null, // Can happen anytime during game
                ),
            ServerMessage.GameResumed::class to
                MessageDefinition(
                    code = "S_RSMD",
                    description = "Notification to unpause (resume) the game on clients",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = null, // Can happen anytime during game
                ),
            ServerMessage.ReconnectionFullStateSync::class to
                MessageDefinition(
                    code = "S_SYNC",
                    description = "Full game state dump for rejoining player",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = GamePhase.Idle,
                ),
            ServerMessage.GameFull::class to
                MessageDefinition(
                    code = "S_FULL",
                    description = "Error: Lobby is full",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = GamePhase.Lobby,
                ),
            ServerMessage.GameAlreadyStarted::class to
                MessageDefinition(
                    code = "S_LATE",
                    description = "Error: Cannot join mid-game",
                    direction = MessageDirection.ServerToClient,
                    expectedPhase = GamePhase.Lobby,
                ),
        )

    fun get(message: Message): MessageDefinition =
        definitions[message::class]
            ?: MessageDefinition(
                code = "UNKN",
                description = "Unknown Message: ${message::class.simpleName}",
                direction = MessageDirection.ClientToServer,
                expectedPhase = null,
            )
}
