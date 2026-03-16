package com.example.nsddemo.data.local.network.repository

import android.util.Log
import app.cash.turbine.test
import com.example.nsddemo.data.local.network.LoopbackDataSource
import com.example.nsddemo.data.local.network.nsd.registration.NetworkRegistration
import com.example.nsddemo.data.local.network.nsd.registration.NsdRegistrationState
import com.example.nsddemo.data.local.network.socket.ConnectionEvent
import com.example.nsddemo.data.local.network.socket.MessageEvent
import com.example.nsddemo.data.local.network.socket.server.ServerListeningState
import com.example.nsddemo.data.local.network.socket.server.SocketServer
import com.example.nsddemo.data.repository.HostServerNetworkRepository
import com.example.nsddemo.domain.model.ClientMessage
import com.example.nsddemo.domain.model.NetworkJson
import com.example.nsddemo.domain.model.PlayerConnectionEvent
import com.example.nsddemo.domain.model.ServerMessage
import com.example.nsddemo.domain.model.ServerState
import io.mockk.MockKAnnotations
import io.mockk.coEvery
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
class HostServerNetworkRepositoryTest {
    private lateinit var repository: HostServerNetworkRepository

    @MockK(relaxed = true)
    private lateinit var networkRegistration: NetworkRegistration

    @MockK(relaxed = true)
    private lateinit var socketServer: SocketServer

    private lateinit var loopbackDataSource: LoopbackDataSource

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

        // Socket Mocks
        every { socketServer.listeningState } returns serverListeningStateFlow
        every { socketServer.connectionEvents } returns socketConnectionEvents
        every { socketServer.messageEvents } returns socketMessageEvents
        coEvery { socketServer.sendToAll(any()) } returns true

        // Registration Mocks
        every { networkRegistration.registrationState } returns registrationStateFlow

        loopbackDataSource = LoopbackDataSource()
        repository =
            HostServerNetworkRepository(networkRegistration, socketServer, loopbackDataSource)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    //region --- Startup & Connection Logic ---
    @Test
    fun `GIVEN Happy Path WHEN start called THEN Socket binds and NSD registers`() =
        runTest {
            repository.serverState.test {
                assertEquals(ServerState.Idle, awaitItem())

                val job = launch { repository.start("GAME123") }
                runCurrent()

                // 1. Socket starts listening
                coVerify { socketServer.startListening() }

                serverListeningStateFlow.value = ServerListeningState.Listening(12345)
                runCurrent()

                // 2. NSD Registration triggered
                verify { networkRegistration.registerService("GAME123", 12345) }
                registrationStateFlow.value = NsdRegistrationState.Registered("Impostle_GAME123")

                advanceUntilIdle()

                // 3. Final State
                val state = awaitItem()
                assertTrue(state is ServerState.Running)
                assertEquals(12345, (state as ServerState.Running).port)

                job.cancel()
            }
        }

    @Test
    fun `GIVEN Socket fails to bind WHEN start called THEN emits Error state`() =
        runTest {
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

    //endregion

    //region --- Message Processing & Session Management ---

    @Test
    fun `GIVEN Remote Client sends RegisterPlayer WHEN received THEN emits PlayerConnected`() =
        runTest {
            val clientIp = "192.168.1.50"
            val playerId = "UUID-Bob"
            val playerMsg = ClientMessage.RegisterPlayer("Bob", playerId)
            val jsonMsg = NetworkJson.encodeToString<ClientMessage>(playerMsg)

            repository.playerConnectionEvents.test {
                // 1. Simulate Socket Message
                socketMessageEvents.emit(MessageEvent.Received(clientIp, jsonMsg))

                // 2. Assert Event
                val event = awaitItem()
                assertTrue(event is PlayerConnectionEvent.PlayerConnected)
                val connected = event as PlayerConnectionEvent.PlayerConnected

                // CRITICAL: The ID should be the Domain ID (UUID), not the IP
                assertEquals(playerId, connected.id)
                assertEquals("Bob", connected.playerName)
            }
        }

    @Test
    fun `GIVEN Registered Player WHEN disconnect event happens THEN emits PlayerDisconnected`() =
        runTest {
            val clientIp = "192.168.1.50"
            val playerId = "UUID-Bob"

            val registerMsg = ClientMessage.RegisterPlayer("Bob", playerId)
            val jsonMsg = NetworkJson.encodeToString<ClientMessage>(registerMsg)

            repository.playerConnectionEvents.test {
                // 1. Register the player WHILE collecting
                // This ensures the incomingMessages flow runs and updates the SessionManager
                socketMessageEvents.emit(MessageEvent.Received(clientIp, jsonMsg))

                // Consume the resulting PlayerConnected event to ensure processing is done
                val connectedEvent = awaitItem()
                assertTrue(connectedEvent is PlayerConnectionEvent.PlayerConnected)

                // 2. Simulate Socket Disconnect
                socketConnectionEvents.emit(ConnectionEvent.Disconnected(clientIp))

                // 3. Assert Disconnection with the correct UUID
                val disconnectEvent = awaitItem()
                assertTrue(disconnectEvent is PlayerConnectionEvent.PlayerDisconnected)
                assertEquals(
                    playerId,
                    (disconnectEvent as PlayerConnectionEvent.PlayerDisconnected).id,
                )
            }
        }

    @Test
    fun `GIVEN Unknown Client (No Register) WHEN disconnect event happens THEN event is ignored`() =
        runTest {
            val unknownIp = "192.168.1.99"

            repository.playerConnectionEvents.test {
                // Simulate Disconnect without prior Registration
                socketConnectionEvents.emit(ConnectionEvent.Disconnected(unknownIp))

                expectNoEvents()
            }
        }

    @Test
    fun `GIVEN Malformed JSON WHEN received THEN message is dropped`() =
        runTest {
            repository.incomingMessages.test {
                // Send garbage
                socketMessageEvents.emit(MessageEvent.Received("123", "{ Garbage }"))

                // Send valid to ensure stream is still alive
                val validMsg = ClientMessage.RegisterPlayer("A", "B")
                socketMessageEvents.emit(
                    MessageEvent.Received(
                        "123",
                        NetworkJson.encodeToString<ClientMessage>(validMsg),
                    ),
                )

                // We should only receive the valid one
                val result = awaitItem()
                assertEquals("B", result.first)
            }
        }

    //endregion

    //region --- Loopback & Merge Logic ---

    @Test
    fun `GIVEN Local Host sends message via Loopback WHEN collected THEN Repo emits with Player ID`() =
        runTest {
            // Arrange
            val hostId = "UUID-Host"
            val localMsg = ClientMessage.RegisterPlayer("HostPlayer", hostId)

            repository.incomingMessages.test {
                // Act: Simulate Host UI sending to Loopback
                // Note: The loopback ID "LOCAL_HOST_CLIENT_ID" is used for transport,
                // but the repo maps it to the ID inside the message.
                loopbackDataSource.clientToServer.emit(
                    LoopbackDataSource.LOCAL_HOST_CLIENT_ID to localMsg,
                )

                // Assert
                val item = awaitItem()
                assertEquals("Should return the UUID from the message", hostId, item.first)
                assertEquals(localMsg, item.second)
            }
        }

    @Test
    fun `GIVEN Both Local and Remote clients send messages WHEN collected THEN Repo emits both`() =
        runTest {
            // Arrange
            val hostId = "UUID-Host"
            val remoteId = "UUID-Remote"

            val localMsg = ClientMessage.RegisterPlayer("Host", hostId)
            val remoteMsg = ClientMessage.RegisterPlayer("Remote", remoteId)
            val remoteJson = NetworkJson.encodeToString<ClientMessage>(remoteMsg)

            repository.incomingMessages.test {
                // Act 1: Local
                loopbackDataSource.clientToServer.emit(LoopbackDataSource.LOCAL_HOST_CLIENT_ID to localMsg)

                // Act 2: Remote
                socketMessageEvents.emit(MessageEvent.Received("1.2.3.4", remoteJson))

                // Assert
                val item1 = awaitItem()
                val item2 = awaitItem()

                // Verify IDs match the UUIDs in the packets
                assertEquals(hostId, item1.first)
                assertEquals(remoteId, item2.first)
            }
        }

    @Test
    fun `GIVEN Server Broadcasts message WHEN called THEN sends to Socket AND Loopback`() =
        runTest {
            // Arrange
            val broadcastMsg = ServerMessage.StartVote

            // Listen to Loopback
            loopbackDataSource.serverToClient.test {
                // Act
                repository.sendToAllPlayers(broadcastMsg)

                // Assert 1: Local Loopback received it
                val localItem = awaitItem()
                assertEquals(
                    "Loopback should receive Pair(LOCAL_HOST_ID, Message)",
                    LoopbackDataSource.LOCAL_HOST_CLIENT_ID to broadcastMsg,
                    localItem,
                )

                // Assert 2: Remote Socket received JSON
                coVerify(exactly = 1) {
                    socketServer.sendToAll(
                        withArg { jsonString ->
                            val expectedJson =
                                NetworkJson.encodeToString<ServerMessage>(broadcastMsg)
                            assertEquals(expectedJson, jsonString)
                        },
                    )
                }
            }
        }

    //endregion
}
