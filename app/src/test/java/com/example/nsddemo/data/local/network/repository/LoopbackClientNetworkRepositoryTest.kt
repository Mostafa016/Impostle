package com.example.nsddemo.data.local.network.repository

import app.cash.turbine.test
import com.example.nsddemo.data.local.network.LoopbackDataSource
import com.example.nsddemo.data.repository.LoopbackClientNetworkRepository
import com.example.nsddemo.domain.model.ClientMessage
import com.example.nsddemo.domain.model.ClientState
import com.example.nsddemo.domain.model.ServerMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class LoopbackClientNetworkRepositoryTest {
    // System Under Test
    private lateinit var repository: LoopbackClientNetworkRepository

    // Dependencies
    private lateinit var dataSource: LoopbackDataSource

    @Before
    fun setUp() {
        dataSource = LoopbackDataSource()
        repository = LoopbackClientNetworkRepository(dataSource)
    }

    //region --- Connection State Logic ---

    @Test
    fun `GIVEN Idle Repo WHEN connect called THEN state becomes Connected`() =
        runTest {
            repository.clientState.test {
                // Initial state
                assertEquals(ClientState.Idle, awaitItem())

                // Act
                repository.connect("ABCD")

                // Assert
                assertEquals(ClientState.Connected, awaitItem())
            }
        }

    @Test
    fun `GIVEN Connected Repo WHEN disconnect called THEN state becomes Disconnected`() =
        runTest {
            repository.clientState.test {
                assertEquals(ClientState.Idle, awaitItem())

                repository.connect("ABCD")
                assertEquals(ClientState.Connected, awaitItem())

                // Act
                repository.disconnect()

                // Assert
                assertEquals(ClientState.Disconnected, awaitItem())
            }
        }

    //endregion

    //region --- Sending Logic (Client -> Server) ---

    @Test
    fun `GIVEN Message to send WHEN sendToServer called THEN emits to DataSource with LOCAL_HOST_ID`() =
        runTest {
            // Arrange
            val messageToSend = ClientMessage.RegisterPlayer("HostPlayer", "ID")

            // Monitor the "Pipe" (DataSource) to see what the repo puts into it
            dataSource.clientToServer.test {
                // Act
                repository.sendToServer(messageToSend)

                // Assert
                val event = awaitItem()
                // Verify ID is the constant LOCAL_HOST_ID
                assertEquals(LoopbackDataSource.LOCAL_HOST_CLIENT_ID, event.first)
                // Verify payload
                assertEquals(messageToSend, event.second)
            }
        }

    //endregion

    //region --- Receiving Logic (Server -> Client) ---

    @Test
    fun `GIVEN Server emits to DataSource WHEN collecting incomingMessages THEN repo emits Message`() =
        runTest {
            // Arrange
            val messageFromStats = "" to ServerMessage.StartVote

            repository.incomingMessages.test {
                // Act: Simulate the Server broadcasting to the Loopback pipe
                dataSource.serverToClient.emit(messageFromStats)

                // Assert: The Client Repo picks it up
                val receivedMsg = awaitItem()
                assertEquals(messageFromStats, receivedMsg)
            }
        }

    //endregion
}
