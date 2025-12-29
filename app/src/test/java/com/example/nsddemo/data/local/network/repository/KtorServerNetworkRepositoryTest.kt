package com.example.nsddemo.data.local.network.repository


import android.util.Log
import app.cash.turbine.test
import com.example.nsddemo.data.local.network.nsd.registration.NetworkRegistration
import com.example.nsddemo.data.local.network.nsd.registration.NsdRegistrationState
import com.example.nsddemo.data.local.network.socket.ConnectionEvent
import com.example.nsddemo.data.local.network.socket.MessageEvent
import com.example.nsddemo.data.local.network.socket.server.ServerListeningState
import com.example.nsddemo.data.local.network.socket.server.SocketServer
import com.example.nsddemo.data.repository.KtorServerNetworkRepository
import com.example.nsddemo.data.util.PlayerConnectionEvent
import com.example.nsddemo.data.util.ServerState
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.domain.util.ClientMessage
import com.example.nsddemo.domain.util.NetworkJson
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class KtorServerNetworkRepositoryTest {

    private lateinit var repository: KtorServerNetworkRepository

    @MockK(relaxed = true)
    private lateinit var networkRegistration: NetworkRegistration

    @MockK(relaxed = true)
    private lateinit var socketServer: SocketServer

    // Flow Mocks
    private val registrationStateFlow =
        MutableStateFlow<NsdRegistrationState>(NsdRegistrationState.Idle)
    private val serverListeningStateFlow =
        MutableStateFlow<ServerListeningState>(ServerListeningState.Idle)
    private val socketConnectionEvents = MutableSharedFlow<ConnectionEvent>()
    private val socketMessageEvents = MutableSharedFlow<MessageEvent>()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(StandardTestDispatcher())

        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.wtf(any(), any<String>()) } returns 0

        every { networkRegistration.registrationState } returns registrationStateFlow
        every { socketServer.listeningState } returns serverListeningStateFlow
        every { socketServer.connectionEvents } returns socketConnectionEvents
        every { socketServer.messageEvents } returns socketMessageEvents

        repository = KtorServerNetworkRepository(networkRegistration, socketServer)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN Happy Path WHEN start called THEN Socket binds and NSD registers`() = runTest {
        repository.serverState.test {
            assertEquals(ServerState.Idle, awaitItem())

            val job = launch { repository.start("GAME123") }
            runCurrent() // Runs up to socketServer.startListening()

            // 1. Socket starts listening
            coVerify { socketServer.startListening() }

            // Update state to trigger the next step in repository
            serverListeningStateFlow.value = ServerListeningState.Listening(12345)

            // Advance the dispatcher so the repository reacts to the state change
            runCurrent()

            // 2. NSD Registration triggered
            verify { networkRegistration.registerService("GAME123", 12345) }
            registrationStateFlow.value = NsdRegistrationState.Registered("Impostle_GAME123")

            // Advance again for the final state update
            advanceUntilIdle()

            // 3. Final State
            val state = awaitItem()
            assertTrue(state is ServerState.Running)
            assertEquals(12345, (state as ServerState.Running).port)

            job.cancel()
        }
    }

    @Test
    fun `GIVEN The Ghost Connection Scenario WHEN messages arrive THEN players are matched correctly`() =
        runTest {
            val clientA = "Ghost_IP"
            val clientB = "Real_Player_IP"
            val playerBName = "Alice"

            // JSON payload for RegisterPlayer
            val registerJson =
                NetworkJson.encodeToString<ClientMessage>(ClientMessage.RegisterPlayer(playerBName))

            repository.playerConnectionEvents.test {
                // 1. Ghost Connects (Socket Event only)
                socketConnectionEvents.emit(ConnectionEvent.Connected(clientA))

                // NOTE: In the old 'zip' code, the repository would be waiting here for a message from ClientA.
                // In the new code, this event is ignored for game logic (but kept for socket mgmt inside SocketServer).
                // So we expect NO events yet.
                expectNoEvents()

                // 2. Real Player Connects (Socket Event)
                socketConnectionEvents.emit(ConnectionEvent.Connected(clientB))
                expectNoEvents() // Still nothing, haven't sent name yet

                // 3. Real Player Sends Name
                socketMessageEvents.emit(MessageEvent.Received(clientB, registerJson))

                // 4. Assert
                val event = awaitItem()
                assertTrue(
                    "Should be PlayerConnected",
                    event is PlayerConnectionEvent.PlayerConnected
                )
                val connection = event as PlayerConnectionEvent.PlayerConnected

                // THE FIX VERIFICATION:
                // Ensure Alice is mapped to ClientB ("Real_Player_IP"), NOT ClientA ("Ghost_IP")
                assertEquals(clientB, connection.id)
                assertEquals(playerBName, connection.playerName)
            }
        }

    @Test
    fun `GIVEN Associated Player WHEN disconnect event happens THEN cleanup occurs`() = runTest {
        val clientId = "192.168.1.50"
        val player = Player("Bob", "Red")

        // Manually associate player (Simulating Domain Layer action)
        repository.associatePlayerWithClient(clientId, player)

        repository.playerConnectionEvents.test {
            // Act: Socket reports disconnect
            socketConnectionEvents.emit(ConnectionEvent.Disconnected(clientId))

            // Assert: Repository emits Domain Disconnect Event
            val event = awaitItem()
            assertTrue(event is PlayerConnectionEvent.PlayerDisconnected)
            val disconnect = event as PlayerConnectionEvent.PlayerDisconnected

            assertEquals(player, disconnect.player)
            assertEquals(clientId, disconnect.id)

            // Verify Map Cleanup
            assertTrue(repository.playerToClientId.value.isEmpty())
        }
    }

    @Test
    fun `GIVEN Unknown Client WHEN disconnect event happens THEN event is ignored`() = runTest {
        val unknownId = "192.168.1.99"

        // No association made

        repository.playerConnectionEvents.test {
            socketConnectionEvents.emit(ConnectionEvent.Disconnected(unknownId))
            expectNoEvents() // Should filter out nulls
        }
    }

    @Test
    fun `GIVEN Socket fails to bind WHEN start called THEN emits Error state`() = runTest {
        repository.serverState.test {
            assertEquals(ServerState.Idle, awaitItem())
            val job = launch { repository.start("GAME") }

            // Simulate Socket Error
            serverListeningStateFlow.value = ServerListeningState.Error("Address in use")

            val state = awaitItem()
            assertTrue(state is ServerState.Error)
            assertEquals("Address in use", (state as ServerState.Error).message)

            job.cancel()
        }
    }

    @Test
    fun `GIVEN Malformed JSON WHEN received THEN message is ignored (Does not crash)`() = runTest {
        val clientID = "123"
        repository.incomingMessages.test {
            // 1. Send Bad Data
            socketMessageEvents.emit(MessageEvent.Received(clientID, "{ Garbage Data }"))

            // 2. Send Good Data
            val goodMsg = ClientMessage.RegisterPlayer("Bob")
            val goodJson = NetworkJson.encodeToString<ClientMessage>(goodMsg)
            socketMessageEvents.emit(MessageEvent.Received(clientID, goodJson))

            // 3. Assert only good data arrives
            val result = awaitItem()
            assertEquals("Bob", (result.second as ClientMessage.RegisterPlayer).playerName)
            // If the flow crashed on step 1, awaitItem() would time out or throw
        }
    }
}