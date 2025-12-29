package com.example.nsddemo.data.local.network.nsd.resolution

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
import java.net.InetAddress

@ExperimentalCoroutinesApi
class NsdNetworkResolutionTest {

    private lateinit var nsdManagerMock: NsdManager
    private lateinit var nsdNetworkResolution: NsdNetworkResolution
    private lateinit var resolveListenerSlot: CapturingSlot<NsdManager.ResolveListener>

    @Before
    fun setUp() {
        nsdManagerMock = mockk(relaxed = true)
        resolveListenerSlot = slot()

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<Throwable>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0

        every {
            nsdManagerMock.resolveService(
                any(),
                capture(resolveListenerSlot)
            )
        } just runs

        nsdNetworkResolution = NsdNetworkResolution(nsdManagerMock)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `GIVEN Idle WHEN resolveService called THEN state becomes Resolving`() = runTest {
        nsdNetworkResolution.resolutionState.test {
            assertEquals(NsdResolutionState.Idle, awaitItem())

            val serviceInfo = mockk<NsdServiceInfo>()
            nsdNetworkResolution.resolveServiceWithGameCode(serviceInfo, "ABCD")

            assertEquals(NsdResolutionState.Resolving, awaitItem())
        }
    }

    @Test
    fun `GIVEN Resolving WHEN resolution succeeds with matching code THEN state becomes Success`() =
        runTest {
            nsdNetworkResolution.resolutionState.test {
                awaitItem() // Idle
                val targetCode = "ABCD"
                val inputService = mockk<NsdServiceInfo>()

                // Act 1: Start
                nsdNetworkResolution.resolveServiceWithGameCode(inputService, targetCode)
                assertEquals(NsdResolutionState.Resolving, awaitItem())

                // Act 2: Simulate Success Callback
                val resolvedService = mockk<NsdServiceInfo>()
                val mockAddress = mockk<InetAddress>()
                every { mockAddress.hostAddress } returns "192.168.1.100"

                every { resolvedService.serviceName } returns "${NSDConstants.BASE_SERVICE_NAME}_$targetCode"
                every { resolvedService.host } returns mockAddress
                every { resolvedService.port } returns 8080

                resolveListenerSlot.captured.onServiceResolved(resolvedService)

                // Assert
                val successState = awaitItem()
                assertTrue(successState is NsdResolutionState.Success)
                assertEquals("192.168.1.100", (successState as NsdResolutionState.Success).host)
                assertEquals(8080, successState.port)
            }
        }

    @Test
    fun `GIVEN Resolving WHEN resolution succeeds but code mismatch THEN state becomes Failed`() =
        runTest {
            nsdNetworkResolution.resolutionState.test {
                awaitItem() // Idle
                val targetCode = "ABCD"
                val inputService = mockk<NsdServiceInfo>()

                // Act 1: Start
                nsdNetworkResolution.resolveServiceWithGameCode(inputService, targetCode)
                assertEquals(NsdResolutionState.Resolving, awaitItem())

                // Act 2: Simulate Success Callback but with a Renamed Service
                val resolvedService = mockk<NsdServiceInfo>()
                every { resolvedService.serviceName } returns "${NSDConstants.BASE_SERVICE_NAME}_ABCD (1)"
                // Note: The logic splits by "_", getting "ABCD (1)". This != "ABCD".

                resolveListenerSlot.captured.onServiceResolved(resolvedService)

                // Assert: We expect a FAILURE now, not silence.
                val state = awaitItem()
                assertTrue(state is NsdResolutionState.Failed)
                assertEquals("Service name mismatch", (state as NsdResolutionState.Failed).error)
            }
        }

    @Test
    fun `GIVEN Resolving WHEN resolution fails THEN state becomes Failed`() = runTest {
        nsdNetworkResolution.resolutionState.test {
            awaitItem() // Idle
            val inputService = mockk<NsdServiceInfo>()

            // Act 1: Start
            nsdNetworkResolution.resolveServiceWithGameCode(inputService, "ABCD")
            awaitItem() // Resolving

            // Act 2: Fail
            val errorCode = NsdManager.FAILURE_ALREADY_ACTIVE
            resolveListenerSlot.captured.onResolveFailed(inputService, errorCode)

            // Assert
            val state = awaitItem()
            assertTrue(state is NsdResolutionState.Failed)
            assertTrue((state as NsdResolutionState.Failed).error.isNotEmpty())
        }
    }
}