package com.example.nsddemo.data.local.network.nsd.discovery

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import app.cash.turbine.test
import com.example.nsddemo.data.util.NSDConstants
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class NsdNetworkDiscoveryTest {
    private lateinit var nsdManagerMock: NsdManager
    private lateinit var nsdNetworkDiscovery: NsdNetworkDiscovery
    private lateinit var discoveryListenerSlot: CapturingSlot<NsdManager.DiscoveryListener>

    @Before
    fun setUp() {
        nsdManagerMock = mockk(relaxed = true)
        discoveryListenerSlot = slot()

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0

        // Capture the listener whenever discoverServices is called
        every {
            nsdManagerMock.discoverServices(
                any<String>(),
                any(),
                capture(discoveryListenerSlot),
            )
        } just runs

        nsdNetworkDiscovery = NsdNetworkDiscovery(nsdManagerMock)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `GIVEN Idle WHEN startDiscovery is called THEN state becomes Discovering`() =
        runTest {
            nsdNetworkDiscovery.discoveryProcessState.test {
                assertEquals(NsdDiscoveryState.Idle, awaitItem())

                // Act
                nsdNetworkDiscovery.startDiscovery("ABCD")

                // Simulate OS callback
                discoveryListenerSlot.captured.onDiscoveryStarted(NSDConstants.SERVICE_TYPE)

                // Assert
                val state = awaitItem()
                assertTrue(state is NsdDiscoveryState.Discovering)
                assertEquals(
                    NSDConstants.SERVICE_TYPE,
                    (state as NsdDiscoveryState.Discovering).serviceType,
                )
            }
        }

    @Test
    fun `GIVEN Discovering WHEN matching service found THEN emits Found event`() =
        runTest {
            // Arrange
            val targetCode = "ABCD"
            nsdNetworkDiscovery.startDiscovery(targetCode)
            // Simulate start
            discoveryListenerSlot.captured.onDiscoveryStarted(NSDConstants.SERVICE_TYPE)

            nsdNetworkDiscovery.discoveredServiceEvent.test {
                // Act: Simulate finding a valid service
                val validService = mockk<NsdServiceInfo>()
                every { validService.serviceType } returns NSDConstants.SERVICE_TYPE
                every { validService.serviceName } returns "${NSDConstants.BASE_SERVICE_NAME}_$targetCode"

                discoveryListenerSlot.captured.onServiceFound(validService)

                // Assert
                val event = awaitItem()
                assertTrue(event is NsdDiscoveryEvent.Found)
                assertEquals(validService, event.serviceInfo)
            }
        }

    @Test
    fun `GIVEN Discovering WHEN wrong game code found THEN ignores event`() =
        runTest {
            // Arrange
            val targetCode = "ABCD"
            nsdNetworkDiscovery.startDiscovery(targetCode)
            discoveryListenerSlot.captured.onDiscoveryStarted(NSDConstants.SERVICE_TYPE)

            nsdNetworkDiscovery.discoveredServiceEvent.test {
                // Act: Simulate finding a service for a DIFFERENT game
                val wrongCodeService = mockk<NsdServiceInfo>()
                every { wrongCodeService.serviceType } returns NSDConstants.SERVICE_TYPE
                every { wrongCodeService.serviceName } returns "${NSDConstants.BASE_SERVICE_NAME}_XYZ9"

                discoveryListenerSlot.captured.onServiceFound(wrongCodeService)

                // Assert: No events should be emitted
                expectNoEvents()
            }
        }

    @Test
    fun `GIVEN Discovering WHEN malformed service name found THEN ignores event`() =
        runTest {
            // Arrange
            nsdNetworkDiscovery.startDiscovery("ABCD")
            discoveryListenerSlot.captured.onDiscoveryStarted(NSDConstants.SERVICE_TYPE)

            nsdNetworkDiscovery.discoveredServiceEvent.test {
                // Act: Service name has no underscore (e.g. "ImpostleGame")
                val malformedService = mockk<NsdServiceInfo>()
                every { malformedService.serviceType } returns NSDConstants.SERVICE_TYPE
                every { malformedService.serviceName } returns "JustRandomString"

                discoveryListenerSlot.captured.onServiceFound(malformedService)

                // Assert
                expectNoEvents()
            }
        }

    @Test
    fun `GIVEN Discovering WHEN wrong service type found THEN ignores event`() =
        runTest {
            // Arrange
            nsdNetworkDiscovery.startDiscovery("ABCD")
            discoveryListenerSlot.captured.onDiscoveryStarted(NSDConstants.SERVICE_TYPE)

            nsdNetworkDiscovery.discoveredServiceEvent.test {
                // Act: Wrong protocol (e.g., _http._tcp)
                val wrongTypeService = mockk<NsdServiceInfo>()
                every { wrongTypeService.serviceType } returns "_printer._tcp."
                every { wrongTypeService.serviceName } returns "${NSDConstants.BASE_SERVICE_NAME}_ABCD"

                discoveryListenerSlot.captured.onServiceFound(wrongTypeService)

                // Assert
                expectNoEvents()
            }
        }

    @Test
    fun `GIVEN Service Found WHEN service lost THEN emits Lost event`() =
        runTest {
            // Arrange
            val gameCode = "ABCD"
            nsdNetworkDiscovery.startDiscovery(gameCode)

            nsdNetworkDiscovery.discoveredServiceEvent.test {
                // Act
                val lostService = mockk<NsdServiceInfo>()
                every { lostService.serviceName } returns "${NSDConstants.BASE_SERVICE_NAME}_$gameCode"
                every { lostService.serviceType } returns NSDConstants.SERVICE_TYPE

                discoveryListenerSlot.captured.onServiceLost(lostService)

                // Assert
                val event = awaitItem()
                assertTrue(event is NsdDiscoveryEvent.Lost)
                assertEquals(lostService, event.serviceInfo)
            }
        }

    @Test
    fun `GIVEN Discovering WHEN start fails THEN state becomes Failed`() =
        runTest {
            nsdNetworkDiscovery.discoveryProcessState.test {
                assertEquals(NsdDiscoveryState.Idle, awaitItem())

                nsdNetworkDiscovery.startDiscovery("ABCD")

                val errorCode = NsdManager.FAILURE_INTERNAL_ERROR
                discoveryListenerSlot.captured.onStartDiscoveryFailed(
                    NSDConstants.SERVICE_TYPE,
                    errorCode,
                )

                val state = awaitItem()
                assertTrue(state is NsdDiscoveryState.Failed)
            }
        }
}
