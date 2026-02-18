package com.example.nsddemo.data.local.network.socket

import android.util.Log
import app.cash.turbine.test
import com.example.nsddemo.data.local.network.socket.client.KtorSocketClient
import com.example.nsddemo.data.util.KtorSocketUtil
import com.example.nsddemo.data.util.KtorSocketUtil.readPacket
import com.example.nsddemo.data.util.KtorSocketUtil.writePacket
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.SocketBuilder
import io.ktor.network.sockets.TcpSocketBuilder
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.isClosed
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class KtorSocketClientTest {

    private lateinit var client: KtorSocketClient

    @MockK
    private lateinit var mockSocket: Socket

    @MockK
    private lateinit var mockReadChannel: ByteReadChannel

    @MockK
    private lateinit var mockWriteChannel: ByteWriteChannel

    @MockK
    private lateinit var mockSocketBuilder: SocketBuilder

    @MockK
    private lateinit var mockTcpBuilder: TcpSocketBuilder

    @MockK
    private lateinit var mockRemoteAddress: SocketAddress

    @MockK
    private lateinit var mockLocalAddress: SocketAddress

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(StandardTestDispatcher())

        // 1. Mock KtorSocketUtil (The Critical Fix)
        mockkObject(KtorSocketUtil)

        // 1. Mock Log
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<Throwable>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0


        // 2. Mock Ktor Static Builders & Extensions
        mockkStatic("io.ktor.network.sockets.BuildersKt")
        mockkStatic("io.ktor.network.sockets.SocketsKt")
        mockkStatic("io.ktor.utils.io.ByteReadChannelKt")
        mockkStatic("io.ktor.utils.io.ByteWriteChannelKt")

        // 3. Setup the Socket Chain (aSocket -> tcp -> connect)
        every { aSocket(any<SelectorManager>()) } returns mockSocketBuilder
        every { mockSocketBuilder.tcp() } returns mockTcpBuilder

        // Default Socket Behavior
        every { mockSocket.openReadChannel() } returns mockReadChannel
        every { mockSocket.openWriteChannel(any()) } returns mockWriteChannel
        every { mockSocket.remoteAddress } returns mockRemoteAddress
        every { mockSocket.localAddress } returns mockLocalAddress
        every { mockRemoteAddress.toString() } returns "192.168.1.100:8080"
        every { mockSocket.isClosed } returns false
        every { mockSocket.close() } returns Unit



        client = KtorSocketClient()
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN valid host WHEN connect called THEN emits Connected event`() = runTest {
        // Arrange
        coEvery { mockTcpBuilder.connect(any(), any(), any()) } returns mockSocket
        // Simulate reading indefinitely so the loop doesn't crash immediately
        coEvery { mockReadChannel.readPacket() } coAnswers { awaitCancellation() }

        client.connectionEvents.test {
            // Act
            // CRITICAL FIX: Launch connect in background because it suspends indefinitely
            val job = launch { client.startSession("localhost", 8080) }

            // Assert
            val event = awaitItem()
            assertTrue(event is ConnectionEvent.Connected)
            assertEquals("localhost", (event as ConnectionEvent.Connected).id)

            job.cancel()
        }
    }

    @Test
    fun `GIVEN connect throws exception WHEN connect called THEN emits Error event`() = runTest {
        // Arrange
        coEvery {
            mockTcpBuilder.connect(
                any(),
                any(),
                any()
            )
        } throws Exception("Connection refused")

        client.connectionEvents.test {
            // Act
            client.startSession("localhost", 8080)
            // Assert
            val event = awaitItem()
            assertTrue(event is ConnectionEvent.Error)
            assertEquals("Connection refused", (event as ConnectionEvent.Error).message)
        }
    }

    @Test
    fun `GIVEN connected WHEN sendToServer called THEN writes string with newline`() = runTest {
        // Arrange
        coEvery { mockTcpBuilder.connect(any(), any(), any()) } returns mockSocket
        coEvery { mockReadChannel.readPacket() } coAnswers { awaitCancellation() }
        coEvery { mockWriteChannel.writePacket(any()) } returns Unit

        // Fix: Mock low-level byte operations so the real writePacket() works
        coEvery { mockWriteChannel.writeInt(any()) } returns Unit
        coEvery { mockWriteChannel.writeFully(any<ByteArray>()) } returns Unit
        coEvery { mockWriteChannel.flush() } returns Unit

        // Act: Start connection in background
        val job = launch { client.startSession("localhost", 8080) }
        advanceUntilIdle() // Ensure connection is established

        client.messageEvents.test {
            // Act: Send message
            val success = client.sendToServer("Hello World")

            // Assert
            assertTrue(success)

            // 1. Verify MessageEvent.Sent was emitted locally
            val event = awaitItem()
            assertTrue(event is MessageEvent.Sent)
            assertEquals("Hello World", event.data)

            // 2. Verify actual ByteWriteChannel interaction
            coVerify { mockWriteChannel.writePacket("Hello World") }

            job.cancel()
        }
    }

    @Test
    fun `GIVEN data with newline WHEN sendToServer called THEN returns false and does not write`() =
        runTest {
            // Arrange
            coEvery { mockTcpBuilder.connect(any(), any(), any()) } returns mockSocket
            coEvery { mockReadChannel.readPacket() } coAnswers { awaitCancellation() }

            val job = launch { client.startSession("localhost", 8080) }
            advanceUntilIdle()

            // Act
            val success = client.sendToServer("Malicious\nPayload")

            // Assert
            assertFalse(success)
            coVerify(exactly = 0) { mockWriteChannel.writePacket(any()) }

            job.cancel()
        }

    @Test
    fun `GIVEN connected WHEN socket receives data THEN emits Received event`() = runTest {
        // Arrange
        coEvery { mockTcpBuilder.connect(any(), any(), any()) } returns mockSocket

        // Use state to return data once, then suspend
        var firstCall = true
        coEvery { mockReadChannel.readPacket() } coAnswers {
            if (firstCall) {
                firstCall = false
                "Welcome"
            } else {
                awaitCancellation()
            }
        }

        client.messageEvents.test {
            // Act
            val job = launch { client.startSession("localhost", 8080) }

            // Assert
            val event = awaitItem()
            assertTrue(event is MessageEvent.Received)
            assertEquals("Welcome", event.data)

            job.cancel()
        }
    }

    @Test
    fun `GIVEN connected WHEN server closes socket (null read) THEN emits Disconnected`() =
        runTest {
            // Arrange
            coEvery { mockTcpBuilder.connect(any(), any(), any()) } returns mockSocket
            // Simulate immediate disconnect
            coEvery { mockReadChannel.readPacket() } returns null

            client.connectionEvents.test {
                // Act
                val job = launch { client.startSession("localhost", 8080) }

                // Assert
                // 1. First event is Connected (from setupClient)
                val connected = awaitItem()
                assertTrue(connected is ConnectionEvent.Connected)

                // 2. Second event is Disconnected (triggered by null read -> throw Cancellation -> catch)
                val disconnected = awaitItem()
                assertTrue(
                    "Expected Disconnected but got $disconnected",
                    disconnected is ConnectionEvent.Disconnected
                )

                job.cancel()
            }
        }
}