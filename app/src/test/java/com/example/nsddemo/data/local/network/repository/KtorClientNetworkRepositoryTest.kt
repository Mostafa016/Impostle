package com.example.nsddemo.data.local.network.repository

import android.net.nsd.NsdServiceInfo
import android.util.Log
import app.cash.turbine.test
import com.example.nsddemo.data.local.network.nsd.discovery.NetworkDiscovery
import com.example.nsddemo.data.local.network.nsd.discovery.NsdDiscoveryEvent
import com.example.nsddemo.data.local.network.nsd.discovery.NsdDiscoveryState
import com.example.nsddemo.data.local.network.nsd.resolution.NetworkResolution
import com.example.nsddemo.data.local.network.nsd.resolution.NsdResolutionState
import com.example.nsddemo.data.local.network.socket.ConnectionEvent
import com.example.nsddemo.data.local.network.socket.client.SocketClient
import com.example.nsddemo.data.repository.KtorClientNetworkRepository
import com.example.nsddemo.data.util.ClientState
import com.example.nsddemo.data.util.NSDConstants
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class KtorClientNetworkRepositoryTest {

    private lateinit var repository: KtorClientNetworkRepository

    @MockK(relaxed = true)
    private lateinit var networkDiscovery: NetworkDiscovery

    @MockK(relaxed = true)
    private lateinit var networkResolution: NetworkResolution

    @MockK(relaxed = true)
    private lateinit var socketClient: SocketClient

    // Flow Mocks to control state from tests
    private val discoveryStateFlow = MutableStateFlow<NsdDiscoveryState>(NsdDiscoveryState.Idle)
    private val discoveredServiceFlow = MutableSharedFlow<NsdDiscoveryEvent>()
    private val resolutionStateFlow = MutableStateFlow<NsdResolutionState>(NsdResolutionState.Idle)
    private val socketConnectionEvents =
        MutableSharedFlow<ConnectionEvent>(replay = 1) // Replay needed for synchronous checks

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(StandardTestDispatcher())

        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.wtf(any(), any<String>()) } returns 0

        // Wire up mocks
        every { networkDiscovery.discoveryProcessState } returns discoveryStateFlow
        every { networkDiscovery.discoveredServiceEvent } returns discoveredServiceFlow
        every { networkResolution.resolutionState } returns resolutionStateFlow
        every { socketClient.connectionEvents } returns socketConnectionEvents

        repository = KtorClientNetworkRepository(networkDiscovery, networkResolution, socketClient)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN Happy Path WHEN connect is called THEN sequence executes successfully`() = runTest {
        // Arrange
        val gameCode = "ABCD"
        val mockService = mockk<NsdServiceInfo>()
        val host = "192.168.1.1"
        val port = 8080

        // Mock startSession to run forever (simulate an active connection)
        coEvery { socketClient.startSession(host, port) } coAnswers { awaitCancellation() }

        repository.clientState.test {
            assertEquals(ClientState.Idle, awaitItem())

            val job = launch { repository.connect(gameCode) }

            // 1. Discovery Starts
            discoveryStateFlow.value = NsdDiscoveryState.Discovering(NSDConstants.SERVICE_TYPE)
            assertEquals(ClientState.Discovering, awaitItem())

            // 2. Service Found
            discoveredServiceFlow.emit(NsdDiscoveryEvent.Found(mockService))

            // 3. Resolution Starts
            assertEquals(ClientState.Resolving, awaitItem())

            // 4. Resolution Success
            resolutionStateFlow.value = NsdResolutionState.Success(host, port)
            assertEquals(ClientState.Connecting, awaitItem())

            // 5. Socket Connects
            socketConnectionEvents.emit(ConnectionEvent.Connected(host))
            assertEquals(ClientState.Connected, awaitItem())

            job.cancel() // Cancel the infinite connection loop
        }

        verify { networkDiscovery.startDiscovery(gameCode) }
        verify { networkResolution.resolveServiceWithGameCode(mockService, gameCode) }
        verify { networkDiscovery.stopDiscovery() }
        coVerify { socketClient.startSession(host, port) }
    }

    @Test
    fun `GIVEN Discovery hangs (Service never found) WHEN connect called THEN emits Timeout Error`() =
        runTest {
            repository.clientState.test {
                assertEquals(ClientState.Idle, awaitItem())

                val job = launch { repository.connect("ABCD") }

                // FIX: Use runCurrent() to start the coroutine without advancing virtual time.
                // This lets execution reach the first suspension point inside performDiscovery().
                runCurrent()

                // 1. Verify Discovery Started
                verify { networkDiscovery.startDiscovery("ABCD") }

                // 2. Simulate OS reporting "Discovery Started" (Passes the first timeout check)
                discoveryStateFlow.value = NsdDiscoveryState.Discovering(NSDConstants.SERVICE_TYPE)

                // Verify Repo updates state to Discovering
                assertEquals(ClientState.Discovering, awaitItem())

                // 3. Now Simulate the Hang: The OS never sends a "Found Service" event.
                // Fast forward time past the timeout limit.
                advanceTimeBy(KtorClientNetworkRepository.TIMEOUT_MS + 1)

                // 4. Assert Timeout Error
                val error = awaitItem()
                assertTrue("Expected Error state but got $error", error is ClientState.Error)
                assertTrue((error as ClientState.Error).canRetry)

                verify { networkDiscovery.stopDiscovery() }
                job.join()
            }
        }

    @Test
    fun `GIVEN Discovery startup hangs WHEN connect called THEN emits Timeout Error`() = runTest {
        repository.clientState.test {
            assertEquals(ClientState.Idle, awaitItem())
            val job = launch { repository.connect("ABCD") }
            runCurrent()

            // We do NOT update discoveryStateFlow. It remains Idle.

            // Fast forward time
            advanceTimeBy(KtorClientNetworkRepository.TIMEOUT_MS + 1)

            val error = awaitItem()
            assertTrue(error is ClientState.Error)

            job.join()
        }
    }

    @Test
    fun `GIVEN Discovery fails immediately WHEN connect called THEN emits Error`() = runTest {
        repository.clientState.test {
            assertEquals(ClientState.Idle, awaitItem())
            val job = launch { repository.connect("ABCD") }

            // Simulate immediate failure from OS
            discoveryStateFlow.value = NsdDiscoveryState.Failed("NSD Busy")

            val error = awaitItem()
            assertTrue(error is ClientState.Error)
            assertEquals("NSD Busy", (error as ClientState.Error).message)

            job.join()
        }
    }

    @Test
    fun `GIVEN Resolution Fails WHEN connect called THEN emits Error state`() = runTest {
        repository.clientState.test {
            assertEquals(ClientState.Idle, awaitItem())
            val job = launch { repository.connect("ABCD") }

            // 1. Discovery Success
            discoveryStateFlow.value = NsdDiscoveryState.Discovering(NSDConstants.SERVICE_TYPE)
            assertEquals(ClientState.Discovering, awaitItem())
            val mockService = mockk<NsdServiceInfo>()
            discoveredServiceFlow.emit(NsdDiscoveryEvent.Found(mockService))

            // 2. Resolution Starts
            assertEquals(ClientState.Resolving, awaitItem())

            // 3. Resolution Fails
            resolutionStateFlow.value = NsdResolutionState.Failed("Resolve Failed")

            // Assert
            val error = awaitItem()
            assertTrue(error is ClientState.Error)
            assertEquals("Resolve Failed", (error as ClientState.Error).message)

            verify { networkDiscovery.stopDiscovery() }
            job.join()
        }
    }

    @Test
    fun `GIVEN Socket Connection Fails WHEN connect called THEN emits Error state`() = runTest {
        // Arrange
        val host = "1.2.3.4"
        val port = 123
        coEvery { socketClient.startSession(host, port) } returns Unit
        // Prepare the error event the repo expects to find
        socketConnectionEvents.emit(ConnectionEvent.Error(host, "Connection Refused"))

        repository.clientState.test {
            assertEquals(ClientState.Idle, awaitItem())
            val job = launch { repository.connect("ABCD") }

            // Fast forward Discovery/Resolution
            discoveryStateFlow.value = NsdDiscoveryState.Discovering(NSDConstants.SERVICE_TYPE)
            assertEquals(ClientState.Discovering, awaitItem())
            discoveredServiceFlow.emit(NsdDiscoveryEvent.Found(mockk()))
            assertEquals(ClientState.Resolving, awaitItem())
            resolutionStateFlow.value = NsdResolutionState.Success(host, port)
            assertEquals(ClientState.Connecting, awaitItem())

            // Assert
            val error = awaitItem()
            assertTrue(error is ClientState.Error)
            assertTrue((error as ClientState.Error).message.contains("Connection Refused"))

            job.join()
        }
    }

    @Test
    fun `WHEN disconnect is called THEN resources are cleaned up`() = runTest {
        repository.disconnect()

        verify { networkDiscovery.stopDiscovery() }
        coVerify { socketClient.disconnect() }

        repository.clientState.test {
            assertEquals(ClientState.Disconnected, awaitItem())
        }
    }
}