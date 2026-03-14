package com.example.nsddemo.domain.e2e

import com.example.nsddemo.data.repository.GameSessionRepositoryImpl
import com.example.nsddemo.domain.e2e.fakes.FakeClientNetworkRepository
import com.example.nsddemo.domain.e2e.fakes.FakeServerNetworkRepository
import com.example.nsddemo.domain.e2e.fakes.FakeWordRepository
import com.example.nsddemo.domain.e2e.fakes.InMemoryNetworkRouter
import com.example.nsddemo.domain.engine.GameClient
import com.example.nsddemo.domain.engine.GameServer
import com.example.nsddemo.domain.logic.SessionManager
import com.example.nsddemo.domain.model.ClientEvent
import com.example.nsddemo.domain.model.GameCategory
import com.example.nsddemo.domain.model.GameData
import com.example.nsddemo.domain.model.GameMode
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.strategy.QuestionGameModeStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * A self-contained headless player for E2E tests.
 *
 * Wires a **real** [GameClient] + optional [GameServer] using fake network repositories backed
 * by [InMemoryNetworkRouter]. No mocks, no Hilt, no Android runtime required.
 *
 * Usage inside `runTest`:
 * ```
 * val router = InMemoryNetworkRouter()
 * val alice = HeadlessPlayer("alice", "Alice", GAME_CODE, router, isHost = true)
 * val bob   = HeadlessPlayer("bob",   "Bob",   GAME_CODE, router, isHost = false)
 *
 * alice.startIn(this)
 * bob.startIn(this)
 * advanceUntilIdle()
 *
 * alice.joinGame()
 * bob.joinGame()
 * advanceUntilIdle()
 * ```
 *
 * @param playerId   Stable player UUID — must be unique per game, can be reused across
 *                   connect/reconnect cycles (same ID triggers reconnection logic).
 * @param name       Display name sent in [ClientMessage.RegisterPlayer].
 * @param gameCode   The game lobby code shared by all players in one game.
 * @param router     Shared [InMemoryNetworkRouter] for this game.
 * @param isHost     When `true`, a [GameServer] is also wired up for this player.
 */
class HeadlessPlayer(
    val playerId: String,
    val name: String,
    val gameCode: String,
    router: InMemoryNetworkRouter,
    isHost: Boolean,
) {
    // ─── Real implementations — zero mocks ──────────────────────────────────

    private val sessionRepo = GameSessionRepositoryImpl()

    val fakeClientRepo = FakeClientNetworkRepository(router, gameCode, playerId)
    val gameClient = GameClient(sessionRepo, fakeClientRepo)

    // Host-only: wire a full GameServer with real strategy + session manager
    private val fakeServerRepo: FakeServerNetworkRepository? =
        if (isHost) FakeServerNetworkRepository(router, gameCode) else null

    val gameServer: GameServer? = fakeServerRepo?.let { serverRepo ->
        val wordRepo = FakeWordRepository()
        val strategy = QuestionGameModeStrategy(wordRepo)
        GameServer(
            serverNetworkRepository = serverRepo,
            strategies = mapOf(GameMode.Question to strategy),
            sessionManager = SessionManager()
        )
    }

    // ─── Public assertion surface ────────────────────────────────────────────

    /** Live view of this player's [GameData] as seen from their own device. */
    val gameData: StateFlow<GameData> = sessionRepo.gameData

    /** Live view of this player's current [GamePhase] as seen from their own device. */
    val gamePhase: StateFlow<GamePhase> = sessionRepo.gameState

    /** Domain events emitted to this player (kick, lobby-closed, player-left…). */
    val clientEvents: SharedFlow<ClientEvent> = gameClient.clientEvent

    // ─── Lifecycle ───────────────────────────────────────────────────────────
    private val activeJobs = mutableListOf<Job>()

    /**
     * Launches the server (if host) and client coroutines inside [scope].
     *
     * Both [GameServer.start] and [GameClient.start] suspend indefinitely while
     * collecting from their respective flows, so they must each run in their own
     * coroutine. Pass the `runTest` block's `this` scope here.
     *
     * After calling this, use [advanceUntilIdle] to let the engine process the
     * startup handshake before sending the first [joinGame] message.
     */
    fun startIn(scope: CoroutineScope) {
        if (gameServer != null) {
            activeJobs += scope.launch { gameServer.start(gameCode, playerId) }
        }
        activeJobs += scope.launch { gameClient.start(gameCode, playerId) }
    }

    /**
     * Kills the infinite collect loops. Call this at the very end of your E2E tests
     * so that `runTest` doesn't hang waiting for them to finish.
     */
    fun stop() {
        activeJobs.forEach { it.cancel() }
        activeJobs.clear()
    }
    // ─── Readable test actions ───────────────────────────────────────────────

    suspend fun joinGame() = gameClient.registerPlayer(name, playerId)
    suspend fun selectCategory(cat: GameCategory) = gameClient.selectCategory(cat)
    suspend fun startGame() = gameClient.startGame()
    suspend fun confirmRole() = gameClient.confirmRole()
    suspend fun endTurn() = gameClient.endTurn()
    suspend fun replayRound() = gameClient.replayRound()
    suspend fun startVote() = gameClient.startVote()
    suspend fun submitVote(targetId: String) = gameClient.submitVote(targetId)
    suspend fun submitImposterGuess(guessedWord: String) =
        gameClient.submitImposterGuess(guessedWord)

    suspend fun continueToGameChoice() = gameClient.continueToGameChoice()
    suspend fun replayGame() = gameClient.replayGame()
    suspend fun endGame() = gameClient.endGame()
    suspend fun kickPlayer(targetId: String) = gameClient.kickPlayer(targetId)
}
