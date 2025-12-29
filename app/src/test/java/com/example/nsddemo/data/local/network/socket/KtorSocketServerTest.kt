package com.example.nsddemo.data.local.network.socket

import android.util.Log
import app.cash.turbine.test
import com.example.nsddemo.data.local.network.WifiHelper
import com.example.nsddemo.data.local.network.socket.server.KtorSocketServer
import com.example.nsddemo.data.local.network.socket.server.ServerListeningState
import com.example.nsddemo.data.util.KtorSocketUtil
import com.example.nsddemo.data.util.KtorSocketUtil.readPacket
import com.example.nsddemo.data.util.KtorSocketUtil.writePacket
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.SocketBuilder
import io.ktor.network.sockets.TcpSocketBuilder
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.isClosed
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.sockets.toJavaAddress
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress

@ExperimentalCoroutinesApi
class KtorSocketServerTest {

    private lateinit var server: KtorSocketServer

    @MockK
    private lateinit var wifiHelper: WifiHelper

    @MockK
    private lateinit var mockServerSocket: ServerSocket

    @MockK
    private lateinit var mockSocketBuilder: SocketBuilder

    @MockK
    private lateinit var mockTcpBuilder: TcpSocketBuilder

    // FIX 1: Use the Ktor interface type here
    @MockK
    private lateinit var mockLocalAddress: io.ktor.network.sockets.SocketAddress

    // Mock Client Objects
    @MockK
    private lateinit var mockClientSocket1: Socket

    @MockK
    private lateinit var mockClientRead1: ByteReadChannel

    @MockK
    private lateinit var mockClientWrite1: ByteWriteChannel

    // FIX 1: Use Ktor interface type for clients too
    @MockK
    private lateinit var mockClientAddress1: io.ktor.network.sockets.SocketAddress

    @MockK
    private lateinit var mockClientSocket2: Socket

    @MockK
    private lateinit var mockClientRead2: ByteReadChannel

    @MockK
    private lateinit var mockClientWrite2: ByteWriteChannel

    @MockK
    private lateinit var mockClientAddress2: io.ktor.network.sockets.SocketAddress

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(StandardTestDispatcher())

        // 1. Mock KtorSocketUtil (The Critical Fix)
        mockkObject(KtorSocketUtil)

        // 1. Mock Logs
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

        // 2. Mock Ktor Statics
        mockkStatic("io.ktor.network.sockets.BuildersKt")
        mockkStatic("io.ktor.network.sockets.SocketsKt")
        mockkStatic("io.ktor.network.sockets.JavaSocketAddressUtilsKt") // Critical for toJavaAddress
        mockkStatic("io.ktor.utils.io.ByteReadChannelKt")
        mockkStatic("io.ktor.utils.io.ByteWriteChannelKt")

        // 3. Server Setup Behavior
        every { aSocket(any<SelectorManager>()) } returns mockSocketBuilder
        every { mockSocketBuilder.tcp() } returns mockTcpBuilder
        every { mockTcpBuilder.bind(any<String>(), any()) } returns mockServerSocket

        every { mockServerSocket.localAddress } returns mockLocalAddress

        // FIX 2: Explicitly mock the extension function call to return a REAL Java address
        // This prevents MockK from trying to run the real code inside 'toJavaAddress()'
        every { mockLocalAddress.toJavaAddress() } returns InetSocketAddress(12345)

        every { mockServerSocket.close() } returns Unit
        every { mockServerSocket.isClosed } returns false

        // Wifi Setup
        every { wifiHelper.ipAddress } returns "192.168.1.100"

        // Client 1 Defaults
        every { mockClientSocket1.remoteAddress } returns mockClientAddress1
        // Allow toString() on the address mock itself
        every { mockClientAddress1.toString() } returns "Client1_IP"
        every { mockClientSocket1.openReadChannel() } returns mockClientRead1
        every { mockClientSocket1.openWriteChannel(any()) } returns mockClientWrite1
        every { mockClientSocket1.close() } returns Unit
        every { mockClientSocket1.isClosed } returns false

        // Client 2 Defaults
        every { mockClientSocket2.remoteAddress } returns mockClientAddress2
        every { mockClientAddress2.toString() } returns "Client2_IP"
        every { mockClientSocket2.openReadChannel() } returns mockClientRead2
        every { mockClientSocket2.openWriteChannel(any()) } returns mockClientWrite2
        every { mockClientSocket2.close() } returns Unit
        every { mockClientSocket2.isClosed } returns false
        // ... existing setup code ...

        // Fix: Mock low-level byte operations so the real writePacket() works
        coEvery { mockClientWrite1.writeInt(any()) } returns Unit
        coEvery { mockClientWrite1.writeFully(any<ByteArray>()) } returns Unit
        coEvery { mockClientWrite1.flush() } returns Unit

        coEvery { mockClientWrite2.writeInt(any()) } returns Unit
        coEvery { mockClientWrite2.writeFully(any<ByteArray>()) } returns Unit
        coEvery { mockClientWrite2.flush() } returns Unit

        server = KtorSocketServer(wifiHelper)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN Wifi IP WHEN startListening THEN binds to port and updates state`() = runTest {
        // Arrange
        // Simulate accept() waiting forever (no clients connecting yet)
        coEvery { mockServerSocket.accept() } coAnswers { awaitCancellation() }

        server.listeningState.test {
            assertEquals(ServerListeningState.Idle, awaitItem())

            // Act: Launch in background so it doesn't block the test
            val job = launch { server.startListening() }
            advanceUntilIdle() // Wait for the coroutine to start and reach the bind call

            // Assert
            val state = awaitItem()
            assertTrue("Expected Listening but got $state", state is ServerListeningState.Listening)
            assertEquals(12345, (state as ServerListeningState.Listening).port)

            verify { mockTcpBuilder.bind("192.168.1.100", 0) }

            job.cancel()
        }
    }

    @Test
    fun `GIVEN listening WHEN client connects THEN emits Connected event`() = runTest {
        // Arrange
        // 1. Return Client 1
        // 2. Then wait forever (don't accept a second one immediately)
        var clientReturned = false
        coEvery { mockServerSocket.accept() } coAnswers {
            if (!clientReturned) {
                clientReturned = true
                mockClientSocket1
            } else {
                awaitCancellation()
            }
        }

        // CRITICAL FIX: The code logic waits for a line read BEFORE emitting Connected.
        // So we must provide a line of data.
        var messageRead = false
        coEvery { mockClientRead1.readPacket() } coAnswers {
            if (!messageRead) {
                messageRead = true
                "Handshake"
            } else {
                awaitCancellation()
            }
        }

        server.connectionEvents.test {
            // Act
            val job = launch { server.startListening() }
            advanceUntilIdle()

            // Assert
            val event = awaitItem()
            assertTrue(event is ConnectionEvent.Connected)
            assertEquals("Client1_IP", (event as ConnectionEvent.Connected).id)

            job.cancel()
        }
    }

    @Test
    fun `GIVEN multiple clients connected WHEN sendToAll called THEN writes to all`() = runTest {
        // Arrange
        val clients = listOf(mockClientSocket1, mockClientSocket2).iterator()
        coEvery { mockServerSocket.accept() } coAnswers {
            if (clients.hasNext()) {
                clients.next()
            } else {
                awaitCancellation()
            }
        }

        // Keep read loops alive
        coEvery { with(KtorSocketUtil) { mockClientRead1.readPacket() } } coAnswers { awaitCancellation() }
        coEvery { with(KtorSocketUtil) { mockClientRead2.readPacket() } } coAnswers { awaitCancellation() }

        // Act
        val job = launch { server.startListening() }
        advanceUntilIdle()

        val result = server.sendToAll("Broadcast Message")

        // Assert
        assertTrue(result.isSuccess)

        // Fix: Verify that flush() was called.
        // This proves writePacket finished its work.
        coVerify { mockClientWrite1.writePacket("Broadcast Message") }
        coVerify { mockClientWrite2.writePacket("Broadcast Message") }

        job.cancel()
    }

    @Test
    fun `GIVEN client connected WHEN client sends data THEN emits Message event`() = runTest {
        // Arrange
        var clientReturned = false
        coEvery { mockServerSocket.accept() } coAnswers {
            if (!clientReturned) {
                clientReturned = true
                mockClientSocket1
            } else {
                awaitCancellation()
            }
        }

        // Return "Hello Server", then wait
        var messageRead = false
        coEvery { mockClientRead1.readPacket() } coAnswers {
            if (!messageRead) {
                messageRead = true
                "Hello Server"
            } else {
                awaitCancellation()
            }
        }

        server.messageEvents.test {
            // Act
            val job = launch { server.startListening() }
            advanceUntilIdle()

            // Assert
            val event = awaitItem()
            assertTrue(event is MessageEvent.Received)
            assertEquals("Client1_IP", event.clientId)
            assertEquals("Hello Server", event.data)

            job.cancel()
        }
    }
}