package com.example.nsddemo.domain.engine

import android.util.Log
import app.cash.turbine.test
import com.example.nsddemo.domain.logic.ClientStateReducer
import com.example.nsddemo.domain.model.ClientEvent
import com.example.nsddemo.domain.model.ClientMessage
import com.example.nsddemo.domain.model.ClientState
import com.example.nsddemo.domain.model.GameCategory
import com.example.nsddemo.domain.model.GameData
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.ServerMessage
import com.example.nsddemo.domain.repository.ClientNetworkRepository
import com.example.nsddemo.domain.repository.GameSessionRepository
import com.example.nsddemo.domain.util.GameFlowRegistry
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
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
class GameClientTest {

    // ... SUT and Dependencies ...
    private lateinit var gameClient: GameClient

    @MockK
    private lateinit var sessionRepo: GameSessionRepository

    @MockK
    private lateinit var networkRepo: ClientNetworkRepository

    // Flow Mocks
    private val incomingMessagesFlow = MutableSharedFlow<Pair<String, ServerMessage>>()
    private val clientStateFlow = MutableStateFlow<ClientState>(ClientState.Idle)

    // NEW: Flows for the Session Repository properties
    private val repoGameDataFlow = MutableStateFlow(GameData())
    private val repoGameStateFlow = MutableStateFlow<GamePhase>(GamePhase.Idle)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(StandardTestDispatcher())

        // ... Static Mocks ...
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
        mockkObject(ClientStateReducer)
        mockkObject(GameFlowRegistry)

        // --- FIX 2: Mock Repository Properties BEFORE init ---
        // GameClient reads these immediately upon construction
        every { sessionRepo.gameData } returns repoGameDataFlow
        every { sessionRepo.gameState } returns repoGameStateFlow

        // --- Repository Function Mocks ---
        every { networkRepo.incomingMessages } returns incomingMessagesFlow
        every { networkRepo.clientState } returns clientStateFlow

        // --- FIX 1: Return Boolean instead of 'just runs' ---
        coEvery { networkRepo.sendToServer(any()) } returns true

        coEvery { networkRepo.connect(any()) } just runs
        coEvery { networkRepo.disconnect() } just runs

        // Mock update logic
        coEvery { sessionRepo.updateGameData(any()) } answers {
            val transform = firstArg<(GameData) -> GameData>()
            transform(GameData())
            Unit
        }
        coEvery { sessionRepo.updateGamePhase(any()) } just runs
        coEvery { sessionRepo.reset() } just runs

        // NOW it is safe to create the class
        gameClient = GameClient(sessionRepo, networkRepo)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    // ==========================================
    // 1. STARTUP & CONNECTION
    // ==========================================

    @Test
    fun `GIVEN Start Called WHEN Connected within Timeout THEN Listening Starts`() = runTest {
        // Arrange
        val job = launch { gameClient.start("CODE", "123") }

        // Act: Simulate successful connection
        clientStateFlow.value = ClientState.Connected
        advanceUntilIdle()

        // Assert
        coVerify { networkRepo.connect("CODE") }
        // Verify stop was NOT called
        coVerify(exactly = 0) { sessionRepo.reset() }

        job.cancel()
    }

    @Test
    fun `GIVEN Start Called WHEN Timeout Reached THEN Stop is Called`() = runTest {
        // Arrange
        val job = launch { gameClient.start("CODE", "123") }

        // Act: Advance time past timeout without changing state
        advanceTimeBy(GameClient.TIMEOUT_MS + 1)

        // Assert
        coVerify { networkRepo.disconnect() }
        coVerify { sessionRepo.reset() }

        job.cancel()
    }

    // ==========================================
    // 2. INCOMING MESSAGE HANDLING
    // ==========================================

    @Test
    fun `GIVEN Server Message WHEN Received THEN Updates Data via Reducer`() = runTest {
        // Arrange
        val msg = ServerMessage.PlayerList(emptyList())
        val dummyData = GameData()

        // Mock Reducer Logic
        // IMPORTANT: Move mocks BEFORE the action that triggers them
        every { ClientStateReducer.reduce(any(), msg) } returns dummyData

        // Start listening
        val job = launch { gameClient.start("CODE", "123") }

        // Ensure the start coroutine gets to the connection check
        runCurrent()

        // Simulate Connection so start() proceeds
        clientStateFlow.value = ClientState.Connected
        runCurrent()

        // Act
        incomingMessagesFlow.emit("server" to msg)
        advanceUntilIdle()

        // Assert
        coVerify { sessionRepo.updateGameData(any()) }
        verify { ClientStateReducer.reduce(any(), msg) }

        job.cancel()
    }

    @Test
    fun `GIVEN Server Message WHEN Transition Exists THEN Updates Phase via Registry`() = runTest {
        // Arrange
        val msg = ServerMessage.StartVote
        val expectedPhase = GamePhase.GameVoting

        val job = launch { gameClient.start("CODE", "123") }
        runCurrent()
        clientStateFlow.value = ClientState.Connected

        // Mock Registry
        every { GameFlowRegistry.getTransitionFor(msg) } returns expectedPhase
        // Mock Reducer to do nothing
        every { ClientStateReducer.reduce(any(), any()) } returns GameData()

        // Act
        incomingMessagesFlow.emit("server" to msg)
        advanceUntilIdle()

        // Assert
        coVerify { sessionRepo.updateGamePhase(expectedPhase) }

        job.cancel()
    }

    @Test
    fun `GIVEN Server Message WHEN No Transition THEN Does Not Update Phase`() = runTest {
        val msg = ServerMessage.PlayerList(emptyList()) // Data only message

        val job = launch { gameClient.start("CODE", "123") }
        clientStateFlow.value = ClientState.Connected

        every { GameFlowRegistry.getTransitionFor(msg) } returns null
        every { ClientStateReducer.reduce(any(), any()) } returns GameData()

        // Act
        incomingMessagesFlow.emit("server" to msg)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { sessionRepo.updateGamePhase(any()) }

        job.cancel()
    }

    // ==========================================
    // 3. EVENT EMISSION
    // ==========================================

    @Test
    fun `GIVEN GameFull Message WHEN Received THEN Emits LobbyFull Event`() = runTest {
        val msg = ServerMessage.GameFull
        every { GameFlowRegistry.getTransitionFor(msg) } returns null
        every { ClientStateReducer.reduce(any(), any()) } returns GameData()

        val job = launch { gameClient.start("CODE", "123") }
        runCurrent()

        clientStateFlow.value = ClientState.Connected

        gameClient.clientEvent.test {
            // Act
            incomingMessagesFlow.emit("server" to msg)

            // Assert
            assertEquals(ClientEvent.LobbyFull, awaitItem())
        }
        job.cancel()
    }

    @Test
    fun `GIVEN PlayerDisconnected Message WHEN Received THEN Emits PlayerLeft Event`() = runTest {
        val playerId = "p1"
        val msg = ServerMessage.PlayerDisconnected(playerId)

        every { GameFlowRegistry.getTransitionFor(msg) } returns null
        every { ClientStateReducer.reduce(any(), any()) } returns GameData()

        val job = launch { gameClient.start("CODE", "123") }
        runCurrent()

        clientStateFlow.value = ClientState.Connected

        gameClient.clientEvent.test {
            // Act
            incomingMessagesFlow.emit("server" to msg)

            // Assert
            val event = awaitItem()
            assertTrue(event is ClientEvent.PlayerLeft)
            assertEquals(playerId, (event as ClientEvent.PlayerLeft).playerId)
        }
        job.cancel()
    }

    // ==========================================
    // 4. OUTGOING ACTIONS (Pass-throughs)
    // ==========================================

    @Test
    fun `GIVEN startGame called WHEN invoked THEN Sends RequestStartGame`() = runTest {
        // Act
        gameClient.startGame()

        // Assert
        coVerify { networkRepo.sendToServer(ClientMessage.RequestStartGame) }
    }

    @Test
    fun `GIVEN registerPlayer called WHEN invoked THEN Sends RegisterPlayer`() = runTest {
        val name = "Alice"
        val id = "123"

        // Act
        gameClient.registerPlayer(name, id)

        // Assert
        coVerify {
            networkRepo.sendToServer(match {
                it is ClientMessage.RegisterPlayer && it.playerName == name && it.playerId == id
            })
        }
    }

    @Test
    fun `GIVEN selectCategory called WHEN invoked THEN Sends RequestSelectCategory`() = runTest {
        val cat = GameCategory.ANIMALS

        // Act
        gameClient.selectCategory(cat)

        // Assert
        coVerify {
            networkRepo.sendToServer(match {
                it is ClientMessage.RequestSelectCategory && it.category == cat
            })
        }
    }

    // ==========================================
    // 5. SHUTDOWN
    // ==========================================

    @Test
    fun `GIVEN Stop Called WHEN invoked THEN Resets Session and Disconnects`() = runTest {
        // Act
        gameClient.stop()

        // Assert
        coVerifyOrder {
            sessionRepo.reset()
            networkRepo.disconnect()
        }
    }
}