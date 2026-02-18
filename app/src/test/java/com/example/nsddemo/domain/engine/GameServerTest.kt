package com.example.nsddemo.domain.engine

import android.util.Log
import app.cash.turbine.test
import com.example.nsddemo.domain.logic.SessionManager
import com.example.nsddemo.domain.model.ClientMessage
import com.example.nsddemo.domain.model.Envelope
import com.example.nsddemo.domain.model.GameData
import com.example.nsddemo.domain.model.GameMode
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.GameStateTransition
import com.example.nsddemo.domain.model.PlayerConnectionEvent
import com.example.nsddemo.domain.model.RoundData
import com.example.nsddemo.domain.model.ServerMessage
import com.example.nsddemo.domain.model.ServerState
import com.example.nsddemo.domain.model.SystemEvent
import com.example.nsddemo.domain.repository.ServerNetworkRepository
import com.example.nsddemo.domain.strategy.GameModeStrategy
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class GameServerTest {

    // SUT
    private lateinit var gameServer: GameServer

    // Dependencies
    @MockK
    private lateinit var repo: ServerNetworkRepository

    @MockK
    private lateinit var strategy: GameModeStrategy

    @MockK
    private lateinit var sessionManager: SessionManager

    // Flow Controllers
    private val incomingMessagesFlow = MutableSharedFlow<Pair<String, ClientMessage>>()
    private val connectionEventsFlow = MutableSharedFlow<PlayerConnectionEvent>()
    private val repoStateFlow = MutableStateFlow<ServerState>(ServerState.Idle)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(StandardTestDispatcher())

        // Mock Logging
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } answers {
            // Print the error if the test fails so we see why
            println("ERROR: ${secondArg<String>()} Exception: ${thirdArg<Throwable>()}")
            0
        }
        every { Log.w(any(), any<Throwable>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.i(any(), any()) } returns 0

        // 1. Setup Repository Mocks
        every { repo.incomingMessages } returns incomingMessagesFlow
        every { repo.playerConnectionEvents } returns connectionEventsFlow
        every { repo.serverState } returns repoStateFlow
        coEvery { repo.start(any()) } just runs
        coEvery { repo.stop() } just runs
        coEvery { repo.sendToPlayer(any(), any()) } just runs
        coEvery { repo.sendToAllPlayers(any()) } just runs

        // 2. Setup Strategy Mocks (Initial State)
        every { strategy.roundData } returns RoundData.Idle

        // 3. Initialize SUT
        gameServer = GameServer(repo, mapOf(GameMode.Question to strategy), sessionManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    //region --- Startup Logic ---

    @Test
    fun `GIVEN Server Starts WHEN Repo becomes Running THEN Server State matches`() = runTest {
        gameServer.serverState.test {
            assertEquals(ServerState.Idle, awaitItem())

            val job = launch { gameServer.start("CODE", "123") }
            runCurrent()

            repoStateFlow.value = ServerState.Running(1234, "CODE")

            val state = awaitItem()
            assertTrue(state is ServerState.Running)

            job.cancel()
        }
    }

    @Test
    fun `GIVEN Server Starts WHEN Repo stays Idle (Timeout) THEN repo stops`() = runTest {
        // 1. Launch the start function (it suspends)
        val job = launch { gameServer.start("CODE", "123") }

        // 2. Advance time PAST the timeout (10,000 + 1)
        advanceTimeBy(GameServer.TIMEOUT_MS + 1)

        // 3. Verify stop was called
        coVerify(exactly = 1) { repo.stop() }

        job.cancel()
    }

    //endregion

    //region --- Message Routing Logic ---

    @Test
    fun `GIVEN RegisterPlayer Message WHEN Received THEN Routes to SessionManager`() = runTest {
        val playerId = "p1"
        val msg = ClientMessage.RegisterPlayer("Alice", playerId)

        coEvery { sessionManager.registerPlayer(any(), any(), msg) } returns
                GameStateTransition.Valid(GameData())

        val job = launch { gameServer.start("CODE", "123") }
        repoStateFlow.value = ServerState.Running(1234, "CODE")
        runCurrent()

        incomingMessagesFlow.emit(playerId to msg)
        advanceUntilIdle()

        coVerify(exactly = 1) { sessionManager.registerPlayer(any(), any(), msg) }
        coVerify(exactly = 0) { strategy.handleAction(any(), any(), any(), any()) }

        job.cancel()
    }

    @Test
    fun `GIVEN Gameplay Message WHEN Received THEN Routes to Strategy`() = runTest {
        val playerId = "p1"
        val msg = ClientMessage.SubmitVote("p2")

        coEvery { strategy.handleAction(any(), any(), msg, playerId) } returns
                GameStateTransition.Valid(GameData())

        val job = launch { gameServer.start("CODE", "123") }
        repoStateFlow.value = ServerState.Running(1234, "CODE")
        runCurrent()

        incomingMessagesFlow.emit(playerId to msg)
        advanceUntilIdle()

        coVerify(exactly = 1) { strategy.handleAction(any(), any(), msg, playerId) }
        coVerify(exactly = 0) { sessionManager.registerPlayer(any(), any(), any()) }

        job.cancel()
    }

    @Test
    fun `GIVEN PlayerDisconnected Event WHEN Received THEN Routes to SessionManager`() = runTest {
        val playerId = "p1"
        val event = PlayerConnectionEvent.PlayerDisconnected(playerId)
        val expectedSystemEvent = SystemEvent.PlayerDisconnected(playerId)

        coEvery { sessionManager.handleSystemEvent(any(), any(), expectedSystemEvent) } returns
                GameStateTransition.Valid(GameData())

        val job = launch { gameServer.start("CODE", "123") }
        repoStateFlow.value = ServerState.Running(1234, "CODE")
        runCurrent()

        connectionEventsFlow.emit(event)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            sessionManager.handleSystemEvent(any(), any(), expectedSystemEvent)
        }

        job.cancel()
    }

    @Test
    fun `GIVEN PlayerConnected Event WHEN Received THEN Ignored`() = runTest {
        val event = PlayerConnectionEvent.PlayerConnected("p1", "Alice")

        val job = launch { gameServer.start("CODE", "123") }
        runCurrent()

        connectionEventsFlow.emit(event)
        advanceUntilIdle()

        coVerify(exactly = 0) { sessionManager.handleSystemEvent(any(), any(), any()) }
        coVerify(exactly = 0) { strategy.handleAction(any(), any(), any(), any()) }

        job.cancel()
    }

    //endregion

    //region --- State Transition & Sending Logic ---

    @Test
    fun `GIVEN Valid Transition with Broadcast WHEN Handled THEN Updates State and Sends`() =
        runTest {
            val playerId = "p1"
            val msg = ClientMessage.SubmitVote("p2")
            val broadcastMsg = ServerMessage.VoteResult(emptyMap(), "imp", emptyMap())

            // Mock Strategy returning New Game Code and New Phase
            coEvery { strategy.handleAction(any(), any(), msg, playerId) } returns
                    GameStateTransition.Valid(
                        newGameData = GameData(gameCode = "UPDATED"),
                        newPhase = GamePhase.GameResults,
                        envelopes = listOf(Envelope.Broadcast(broadcastMsg))
                    )

            val job = launch { gameServer.start("CODE", "123") }
            repoStateFlow.value = ServerState.Running(1234, "CODE")
            runCurrent()

            incomingMessagesFlow.emit(playerId to msg)
            advanceUntilIdle()

            // 1. Verify Message Sent
            coVerify(exactly = 1) { repo.sendToAllPlayers(broadcastMsg) }

            // 2. Verify State Updated (By triggering another action and checking inputs)
            val nextMsg = ClientMessage.RequestReplayGame
            coEvery {
                strategy.handleAction(
                    any(),
                    any(),
                    nextMsg,
                    playerId
                )
            } returns GameStateTransition.Valid(GameData())

            incomingMessagesFlow.emit(playerId to nextMsg)
            advanceUntilIdle()

            coVerify {
                strategy.handleAction(
                    withArg { assertEquals("UPDATED", it.gameCode) }, // Data was updated
                    withArg { assertEquals(GamePhase.GameResults, it) }, // Phase was updated
                    nextMsg,
                    playerId
                )
            }

            job.cancel()
        }

    @Test
    fun `GIVEN Invalid Transition WHEN Handled THEN Sends Messages But Preserves State`() =
        runTest {
            val playerId = "p1"
            val msg = ClientMessage.RegisterPlayer("A", "p1")
            val errorMsg = ServerMessage.GameFull

            // Mock SessionManager returning Invalid transition with an envelope
            coEvery { sessionManager.registerPlayer(any(), any(), msg) } returns
                    GameStateTransition.Invalid(
                        reason = "Full",
                        envelopes = listOf(Envelope.Unicast("p1", errorMsg))
                    )

            val job = launch { gameServer.start("CODE", "123") }
            repoStateFlow.value = ServerState.Running(1234, "CODE")
            runCurrent()

            // Capture initial state by triggering a dummy action first if needed,
            // but here we just check that the SECOND action receives the DEFAULT state
            // (i.e. state didn't change to "UPDATED" or anything else)

            incomingMessagesFlow.emit(playerId to msg)
            advanceUntilIdle()

            // 1. Verify Error Message Sent
            coVerify(exactly = 1) { repo.sendToPlayer("p1", errorMsg) }

            // 2. Verify State is UNCHANGED (still default Lobby/Empty Data)
            val checkMsg = ClientMessage.RequestStartGame
            coEvery {
                strategy.handleAction(
                    any(),
                    any(),
                    checkMsg,
                    playerId
                )
            } returns GameStateTransition.Valid(GameData())

            incomingMessagesFlow.emit(playerId to checkMsg)
            advanceUntilIdle()

            coVerify {
                strategy.handleAction(
                    withArg { assertEquals("CODE", it.gameCode) }, // Still default, not changed
                    withArg { assertEquals(GamePhase.Lobby, it) }, // Still Lobby
                    checkMsg,
                    playerId
                )
            }

            job.cancel()
        }

    @Test
    fun `GIVEN Transition with Multiple Envelopes WHEN Handled THEN Sends All`() = runTest {
        val playerId = "p1"
        val msg = ClientMessage.SubmitVote("p2")
        val broadcastMsg = ServerMessage.VoteResult(emptyMap(), "imp", emptyMap())
        val unicastMsg = ServerMessage.PlayerVoted("p1", "p2")

        coEvery { strategy.handleAction(any(), any(), msg, playerId) } returns
                GameStateTransition.Valid(
                    newGameData = GameData(),
                    envelopes = listOf(
                        Envelope.Broadcast(broadcastMsg),
                        Envelope.Unicast(playerId, unicastMsg)
                    )
                )

        val job = launch { gameServer.start("CODE", "123") }
        repoStateFlow.value = ServerState.Running(1234, "CODE")
        runCurrent()

        incomingMessagesFlow.emit(playerId to msg)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.sendToAllPlayers(broadcastMsg) }
        coVerify(exactly = 1) { repo.sendToPlayer(playerId, unicastMsg) }

        job.cancel()
    }

    @Test
    fun `GIVEN Transition with No Phase Change WHEN Handled THEN Phase Persists`() = runTest {
        val playerId = "p1"
        val msg = ClientMessage.SubmitVote("p2")

        // Return Valid transition but newPhase is null
        coEvery { strategy.handleAction(any(), any(), msg, playerId) } returns
                GameStateTransition.Valid(
                    newGameData = GameData(),
                    newPhase = null // Explicitly null
                )

        val job = launch { gameServer.start("CODE", "123") }
        repoStateFlow.value = ServerState.Running(1234, "CODE")
        runCurrent()

        incomingMessagesFlow.emit(playerId to msg)
        advanceUntilIdle()

        // Trigger next event to check state
        val checkMsg = ClientMessage.EndTurn
        coEvery {
            strategy.handleAction(
                any(),
                any(),
                checkMsg,
                playerId
            )
        } returns GameStateTransition.Valid(GameData())

        incomingMessagesFlow.emit(playerId to checkMsg)
        advanceUntilIdle()

        coVerify {
            strategy.handleAction(
                any(),
                withArg { assertEquals(GamePhase.Lobby, it) }, // Should still be Lobby (Default)
                checkMsg,
                playerId
            )
        }

        job.cancel()
    }
    //endregion

    @Test
    fun `GIVEN Stop Called THEN Repo Stops`() = runTest {
        gameServer.stop()
        coVerify(exactly = 1) { repo.stop() }
    }
}