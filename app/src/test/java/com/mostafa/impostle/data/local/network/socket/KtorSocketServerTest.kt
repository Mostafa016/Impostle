package com.mostafa.impostle.data.local.network.socket

import android.util.Log
import app.cash.turbine.test
import com.mostafa.impostle.data.local.network.WifiHelper
import com.mostafa.impostle.data.local.network.socket.server.KtorSocketServer
import com.mostafa.impostle.data.local.network.socket.server.ServerListeningState
import com.mostafa.impostle.data.util.KtorSocketUtil
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
import io.ktor.utils.io.ByteChannel
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress

@ExperimentalCoroutinesApi
class KtorSocketServerTest {
    private lateinit var server: KtorSocketServer

    @MockK
    private lateinit var wifiHelper: WifiHelper

    @MockK(relaxed = true)
    private lateinit var mockServerSocket: ServerSocket

    @MockK
    private lateinit var mockSocketBuilder: SocketBuilder

    @MockK
    private lateinit var mockTcpBuilder: TcpSocketBuilder

    @MockK
    private lateinit var mockLocalAddress: io.ktor.network.sockets.SocketAddress

    // Mock Sockets
    @MockK(relaxed = true)
    private lateinit var mockClientSocket1: Socket

    @MockK
    private lateinit var mockClientAddress1: io.ktor.network.sockets.SocketAddress

    @MockK(relaxed = true)
    private lateinit var mockClientSocket2: Socket

    @MockK
    private lateinit var mockClientAddress2: io.ktor.network.sockets.SocketAddress

    // REAL In-Memory Channels (The Fix)
    // Server Reads from here -> Test Writes to here
    private lateinit var client1ReadChannel: ByteChannel

    // Server Writes to here -> Test Reads from here
    private lateinit var client1WriteChannel: ByteChannel

    private lateinit var client2ReadChannel: ByteChannel
    private lateinit var client2WriteChannel: ByteChannel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(StandardTestDispatcher())

        // Initialize Real Channels
        client1ReadChannel = ByteChannel(autoFlush = true)
        client1WriteChannel = ByteChannel(autoFlush = true)
        client2ReadChannel = ByteChannel(autoFlush = true)
        client2WriteChannel = ByteChannel(autoFlush = true)

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<Throwable>()) } returns 0
        every { Log.i(any(), any()) } returns 0

        mockkStatic("io.ktor.network.sockets.BuildersKt")
        mockkStatic("io.ktor.network.sockets.SocketsKt")
        mockkStatic("io.ktor.network.sockets.JavaSocketAddressUtilsKt")

        // Server Setup
        every { aSocket(any<SelectorManager>()) } returns mockSocketBuilder
        every { mockSocketBuilder.tcp() } returns mockTcpBuilder
        coEvery { mockTcpBuilder.bind(any(), any(), any()) } returns mockServerSocket

        every { mockServerSocket.localAddress } returns mockLocalAddress
        every { mockLocalAddress.toJavaAddress() } returns InetSocketAddress(12345)
        every { mockServerSocket.isClosed } returns false

        // Wifi
        every { wifiHelper.ipAddress } returns "192.168.1.100"

        // Client 1 Setup
        every { mockClientSocket1.remoteAddress } returns mockClientAddress1
        every { mockClientAddress1.toString() } returns "Client1_IP"
        // Return REAL channels
        every { mockClientSocket1.openReadChannel() } returns client1ReadChannel
        every { mockClientSocket1.openWriteChannel(any()) } returns client1WriteChannel
        every { mockClientSocket1.close() } answers {
            client1ReadChannel.close()
            client1WriteChannel.close()
        }
        every { mockClientSocket1.isClosed } returns false

        // Client 2 Setup
        every { mockClientSocket2.remoteAddress } returns mockClientAddress2
        every { mockClientAddress2.toString() } returns "Client2_IP"
        every { mockClientSocket2.openReadChannel() } returns client2ReadChannel
        every { mockClientSocket2.openWriteChannel(any()) } returns client2WriteChannel
        every { mockClientSocket2.close() } answers {
            client2ReadChannel.close()
            client2WriteChannel.close()
        }
        every { mockClientSocket2.isClosed } returns false

        server = KtorSocketServer(wifiHelper)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN Wifi IP WHEN startListening THEN binds to port and updates state`() =
        runTest {
            coEvery { mockServerSocket.accept() } coAnswers { awaitCancellation() }

            server.listeningState.test {
                assertEquals(ServerListeningState.Idle, awaitItem())

                val job = launch { server.startListening() }
                advanceUntilIdle()

                val state = awaitItem()
                assertTrue(state is ServerListeningState.Listening)
                assertEquals(12345, (state as ServerListeningState.Listening).port)

                coVerify { mockTcpBuilder.bind("192.168.1.100", 0, any()) }
                job.cancel()
            }
        }

    @Test
    fun `GIVEN listening WHEN client connects THEN emits Connected event`() =
        runTest {
            var clientReturned = false
            coEvery { mockServerSocket.accept() } coAnswers {
                if (!clientReturned) {
                    clientReturned = true
                    mockClientSocket1
                } else {
                    awaitCancellation()
                }
            }

            // Simulate Client sending Handshake packet
            // We act as the client writing to the server's input stream
            with(KtorSocketUtil) {
                client1ReadChannel.writePacket("Handshake")
            }

            server.connectionEvents.test {
                val job = launch { server.startListening() }
                advanceUntilIdle()

                val event = awaitItem()
                assertTrue(event is ConnectionEvent.Connected)
                assertEquals("Client1_IP", (event as ConnectionEvent.Connected).id)

                job.cancel()
            }
        }

    @Test
    fun `GIVEN multiple clients connected WHEN sendToAll called THEN writes to all`() =
        runTest {
            val clients = listOf(mockClientSocket1, mockClientSocket2).iterator()
            coEvery { mockServerSocket.accept() } coAnswers {
                if (clients.hasNext()) clients.next() else awaitCancellation()
            }

            // Launch server
            val job = launch { server.startListening() }
            advanceUntilIdle()

            // Act
            val result = server.sendToAll("Broadcast Message")

            // Assert
            assertTrue(result)

            // Verify Real Data was written
            // We act as the client reading what the server wrote
            with(KtorSocketUtil) {
                val msg1 = client1WriteChannel.readPacket()
                val msg2 = client2WriteChannel.readPacket()
                assertEquals("Broadcast Message", msg1)
                assertEquals("Broadcast Message", msg2)
            }

            job.cancel()
        }

    @Test
    fun `GIVEN client connected WHEN client sends data THEN emits Message event`() =
        runTest {
            var clientReturned = false
            coEvery { mockServerSocket.accept() } coAnswers {
                if (!clientReturned) {
                    clientReturned = true
                    mockClientSocket1
                } else {
                    awaitCancellation()
                }
            }

            server.messageEvents.test {
                val job = launch { server.startListening() }
                advanceUntilIdle()

                // Simulate Client sending "Hello Server"
                with(KtorSocketUtil) {
                    client1ReadChannel.writePacket("Hello Server")
                }
                advanceUntilIdle()

                val event = awaitItem()
                assertTrue(event is MessageEvent.Received)
                assertEquals("Client1_IP", event.clientId)
                assertEquals("Hello Server", event.data)

                job.cancel()
            }
        }

    @Test
    fun `GIVEN client connected WHEN concurrent writes to same client THEN emits messages in order`() =
        runTest {
            var clientReturned = false
            coEvery { mockServerSocket.accept() } coAnswers {
                if (!clientReturned) {
                    clientReturned = true
                    mockClientSocket1
                } else {
                    awaitCancellation()
                }
            }

            server.messageEvents.test {
                val job = launch { server.startListening() }
                advanceUntilIdle()

                // Simulate Client sending "Hello Server"
                repeat(10) { count ->
                    launch {
                        server.sendToClient("Client1_IP", "Hello Server $count")
                    }
                }
                advanceUntilIdle()

                repeat(10) { count ->
                    val event = awaitItem()
                    assertTrue(event is MessageEvent.Sent)
                    assertEquals("Client1_IP", event.clientId)
                    assertEquals("Hello Server $count", event.data)
                }

                job.cancel()
            }

            advanceUntilIdle()
        }
}
