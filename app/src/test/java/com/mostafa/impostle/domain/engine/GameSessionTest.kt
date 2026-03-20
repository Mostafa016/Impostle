package com.mostafa.impostle.domain.engine

import android.util.Log
import com.mostafa.impostle.data.repository.LoopbackClientNetworkRepository
import com.mostafa.impostle.data.repository.RemoteClientNetworkRepository
import com.mostafa.impostle.domain.model.ClientState
import com.mostafa.impostle.domain.model.ServerState
import com.mostafa.impostle.domain.model.SessionState
import com.mostafa.impostle.domain.repository.GameSessionRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.inject.Provider

@ExperimentalCoroutinesApi
class GameSessionTest {
    // SUT
    private lateinit var gameSession: GameSession

    // Mocks
    private lateinit var clientFactory: GameClient.GameClientFactory
    private lateinit var serverProvider: Provider<GameServer>
    private lateinit var remoteRepo: RemoteClientNetworkRepository
    private lateinit var remoteRepoProvider: Provider<RemoteClientNetworkRepository>
    private lateinit var loopbackRepo: LoopbackClientNetworkRepository
    private lateinit var loopbackRepoProvider: Provider<LoopbackClientNetworkRepository>
    private lateinit var sessionRepo: GameSessionRepository

    // Mocked Instances (Returned by Factory/Provider)
    private lateinit var mockGameClient: GameClient
    private lateinit var mockGameServer: GameServer

    // State Flows to control internal logic
    private val serverStateFlow = MutableStateFlow<ServerState>(ServerState.Idle)
    private val clientStateFlow = MutableStateFlow<ClientState>(ClientState.Idle)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(StandardTestDispatcher())

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

        // 1. Create Mocks
        clientFactory = mockk()
        serverProvider = mockk()
        remoteRepo = mockk()
        remoteRepoProvider = mockk()
        loopbackRepo = mockk()
        loopbackRepoProvider = mockk()
        sessionRepo = mockk(relaxed = true) // Relaxed for gameData/Phase flows

        mockGameClient = mockk()
        mockGameServer = mockk()

        // 2. Setup Behavior
        // Server Setup
        every { serverProvider.get() } returns mockGameServer
        every { mockGameServer.serverState } returns serverStateFlow
        coEvery { mockGameServer.start(any(), any()) } returns Unit
        coEvery { mockGameServer.stop() } just runs

        // Client Setup
        every { clientFactory.create(any()) } returns mockGameClient
        every { loopbackRepoProvider.get() } returns loopbackRepo
        every { remoteRepoProvider.get() } returns remoteRepo
        every { mockGameClient.clientState } returns clientStateFlow
        coEvery { mockGameClient.start(any(), any()) } returns Unit
        coEvery { mockGameClient.stop() } just runs

        // Repo Setup
        coEvery { sessionRepo.reset() } just runs

        // 3. Init SUT
        gameSession =
            GameSession(
                clientFactory,
                serverProvider,
                remoteRepoProvider,
                loopbackRepoProvider,
                sessionRepo,
            )
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    // ==========================================
    // 1. HOST SESSION TESTS
    // ==========================================

    @Test
    fun `GIVEN Host Start WHEN Server and Client Connect THEN Session is Running`() =
        runTest {
            // Arrange
            val job = launch { gameSession.startHostSession("CODE", "123") }
            runCurrent() // Allow start to proceed to collection

            // Verify "Connecting" state initially
            assertEquals(SessionState.Connecting, gameSession.sessionState.first())

            // Act: Simulate Success
            serverStateFlow.value = ServerState.Running(1234, "CODE")
            clientStateFlow.value = ClientState.Connected
            advanceUntilIdle()

            // Assert
            assertTrue(gameSession.sessionState.first() is SessionState.Running)

            // Verify Correct Dependencies used
            verify { clientFactory.create(loopbackRepo) } // Host uses Loopback
            verify { serverProvider.get() } // Host creates Server

            // Verify Start calls
            coVerify { mockGameServer.start("CODE", "123") }
            coVerify { mockGameClient.start("CODE", "123") }

            job.cancel()
        }

    @Test
    fun `GIVEN Host Start WHEN Server Fails THEN Session Error and Cleanup`() =
        runTest {
            val job = launch { gameSession.startHostSession("CODE", "123") }
            runCurrent()

            // Act: Server fails
            serverStateFlow.value = ServerState.Error("Port in use")
            clientStateFlow.value = ClientState.Connected
            advanceUntilIdle()

            // Assert
            val state = gameSession.sessionState.first()
            assertTrue(state is SessionState.Error)
            assertEquals("Port in use", (state as SessionState.Error).reason)

            // Verify Cleanup
            coVerify { mockGameServer.stop() }
            coVerify { mockGameClient.stop() }

            job.cancel()
        }

    @Test
    fun `GIVEN Host Start WHEN Server Ready but Client Fails THEN Session Error`() =
        runTest {
            val job = launch { gameSession.startHostSession("CODE", "123") }
            runCurrent()

            // Act: Server OK, Client Fails
            serverStateFlow.value = ServerState.Running(1234, "CODE")
            clientStateFlow.value = ClientState.Error("Loopback failed")
            advanceUntilIdle()

            // Assert
            val state = gameSession.sessionState.first()
            assertTrue(state is SessionState.Error)
            assertEquals("Loopback failed", (state as SessionState.Error).reason)

            coVerify { mockGameServer.stop() } // Cleanup should still happen

            job.cancel()
        }

    @Test
    fun `GIVEN Host Start WHEN Timeout Reached THEN Session Error`() =
        runTest {
            val job = launch { gameSession.startHostSession("CODE", "123") }
            runCurrent()

            // Act: Wait longer than timeout without states changing
            advanceTimeBy(GameServer.TIMEOUT_MS + 1000)

            // Assert
            val state = gameSession.sessionState.first()
            assertTrue(state is SessionState.Error)
            assertTrue((state as SessionState.Error).reason.contains("timed out"))

            coVerify { mockGameServer.stop() }

            job.cancel()
        }

    // ==========================================
    // 2. CLIENT (JOIN) SESSION TESTS
    // ==========================================

    @Test
    fun `GIVEN Join Start WHEN Client Connects THEN Session is Running`() =
        runTest {
            val job = launch { gameSession.startJoinSession("CODE", "123") }
            runCurrent()

            assertEquals(SessionState.Connecting, gameSession.sessionState.first())

            // Act: Client Connects
            clientStateFlow.value = ClientState.Connected
            advanceUntilIdle()

            // Assert
            assertTrue(gameSession.sessionState.first() is SessionState.Running)

            // Verify Correct Dependencies
            coVerify { clientFactory.create(remoteRepo) } // Join uses Remote
            verify(exactly = 0) { serverProvider.get() } // Join does NOT create Server

            job.cancel()
        }

    @Test
    fun `GIVEN Join Start WHEN Client Fails THEN Session Error`() =
        runTest {
            val job = launch { gameSession.startJoinSession("CODE", "123") }
            runCurrent()

            // Act
            clientStateFlow.value = ClientState.Error("Host not found")
            advanceUntilIdle()

            // Assert
            val state = gameSession.sessionState.first()
            assertTrue(state is SessionState.Error)
            assertEquals("Host not found", (state as SessionState.Error).reason)

            job.cancel()
        }

    @Test
    fun `GIVEN Join Start WHEN Timeout Reached THEN Session Error`() =
        runTest {
            val job = launch { gameSession.startJoinSession("CODE", "123") }
            runCurrent()

            // Act
            advanceTimeBy(GameClient.TIMEOUT_MS + 1)

            // Assert
            val state = gameSession.sessionState.first()
            assertTrue(state is SessionState.Error)
            assertTrue((state as SessionState.Error).reason.contains("timed out"))

            job.cancel()
        }

    // ==========================================
    // 3. CLEANUP TESTS
    // ==========================================

    @Test
    fun `GIVEN Running Session WHEN Cleanup Called THEN Stops All and Resets`() =
        runTest {
            // Arrange: Start a host session first to populate variables
            val job = launch { gameSession.startHostSession("CODE", "123") }
            runCurrent()
            serverStateFlow.value = ServerState.Running(1, "C")
            clientStateFlow.value = ClientState.Connected
            advanceUntilIdle()
            job.cancel() // Stop the startup job, session is now "Active" in memory

            // Act
            gameSession.reset()

            // Assert
            coVerify { mockGameServer.stop() }
            coVerify { mockGameClient.stop() }

            assertEquals(SessionState.Idle, gameSession.sessionState.first())
            assertNull(gameSession.activeClient)
        }
}
