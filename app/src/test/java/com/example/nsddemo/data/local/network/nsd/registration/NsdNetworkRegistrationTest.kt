package com.example.nsddemo.data.local.network.nsd.registration

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.example.nsddemo.data.util.NSDConstants
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class NsdNetworkRegistrationTest {

    private lateinit var nsdManagerMock: NsdManager
    private lateinit var nsdNetworkRegistration: NsdNetworkRegistration
    private lateinit var registrationListenerSlot: CapturingSlot<NsdManager.RegistrationListener>

    @Before
    fun setUp() {
        nsdManagerMock = mockk(relaxed = true)
        registrationListenerSlot = slot()

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0

        every {
            nsdManagerMock.registerService(
                any(),
                any(),
                capture(registrationListenerSlot)
            )
        } just runs

        mockkConstructor(NsdServiceInfo::class)

        // Stub the methods that will be called on the mocked NsdServiceInfo.
        // We tell it to expect these calls and just do nothing (`just runs`).
        every { anyConstructed<NsdServiceInfo>().serviceName = any() } just runs
        every { anyConstructed<NsdServiceInfo>().serviceType = any() } just runs
        every { anyConstructed<NsdServiceInfo>().setPort(any()) } just runs

        nsdNetworkRegistration = NsdNetworkRegistration(nsdManagerMock)
    }

    //region --- Happy Path Tests ---
    @Test
    fun `GIVEN Idle state WHEN registerService is successful THEN emits Registered state`() =
        runTest {
            nsdNetworkRegistration.registrationState.test {
                assertEquals("Initial state should be Idle", NsdRegistrationState.Idle, awaitItem())
                val testGameCode = "TESTCODE"
                val testPort = 12345
                // ACT
                nsdNetworkRegistration.registerService(testGameCode, testPort)

                assertEquals(
                    "State should be Registering",
                    NsdRegistrationState.Registering,
                    awaitItem()
                )

                // VERIFY SUT's internal object creation and interaction
                verify {
                    nsdManagerMock.registerService(
                        withArg { serviceInfo ->
                            verify {
                                serviceInfo.serviceName =
                                    "${NSDConstants.BASE_SERVICE_NAME}_$testGameCode"
                            }
                            verify { serviceInfo.serviceType = NSDConstants.SERVICE_TYPE }
                            verify { serviceInfo.setPort(testPort) }
                        },
                        NsdManager.PROTOCOL_DNS_SD,
                        any()
                    )
                }

                // --- SIMULATE AND ASSERT CALLBACK  ---

                // 1. Create a mock for the NsdServiceInfo object that will be passed INTO our listener.
                val callbackServiceInfo = mockk<NsdServiceInfo>()

                // 2. Stub ALL the methods that will be called on this mock inside the onServiceRegistered method.
                //    This is where we provide the "answers" to MockK.
                every { callbackServiceInfo.serviceName } returns "RegisteredServiceName_TESTCODE"
                every { callbackServiceInfo.host } returns mockk() // Can return another mock if we don't care about the InetAddress object.
                every { callbackServiceInfo.port } returns 54321   // Return a dummy port number.

                // 3. Trigger the callback on our captured listener, passing our fully stubbed mock.
                registrationListenerSlot.captured.onServiceRegistered(callbackServiceInfo)

                // --- ASSERT FINAL STATE (This part remains the same) ---

                val registeredState = awaitItem()
                assertTrue(
                    "Final state should be Registered",
                    registeredState is NsdRegistrationState.Registered
                )
                assertEquals(
                    "Service name in state should match callback",
                    "RegisteredServiceName_TESTCODE",
                    (registeredState as NsdRegistrationState.Registered).serviceName
                )
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `GIVEN Registered state WHEN unregisterService is successful THEN emits Unregistered state`() =
        runTest {
            nsdNetworkRegistration.registrationState.test {
                // Arrange
                val callbackServiceInfo = successfullyRegisterTheService()

                // Act
                nsdNetworkRegistration.unregisterService()
                registrationListenerSlot.captured.onServiceUnregistered(callbackServiceInfo)

                // Assert
                val unRegisteringState = awaitItem()
                assertTrue(
                    "State after calling unregisterService should be UnRegistering",
                    unRegisteringState is NsdRegistrationState.UnRegistering
                )

                val unregisteredState = awaitItem()
                assertTrue(
                    "Final state should be UnRegistered",
                    unregisteredState is NsdRegistrationState.UnRegistered
                )

                assertEquals(
                    "Service name in state should match callback",
                    callbackServiceInfo.serviceName,
                    (unregisteredState as NsdRegistrationState.UnRegistered).serviceName
                )
                cancelAndConsumeRemainingEvents()
            }
        }
    //endregion

    //region ---- Error Path Tests ---
    @Test
    fun `GIVEN Idle state WHEN registerService is failure THEN emits Failed state`() = runTest {
        nsdNetworkRegistration.registrationState.test {
            // Arrange
            assertEquals("Initial state should be Idle", NsdRegistrationState.Idle, awaitItem())
            val testGameCode = "TESTCODE"
            val testPort = 12345

            // ACT
            nsdNetworkRegistration.registerService(testGameCode, testPort)

            assertEquals(
                "State should be Registering",
                NsdRegistrationState.Registering,
                awaitItem()
            )

            // VERIFY SUT's internal object creation and interaction
            verify {
                nsdManagerMock.registerService(
                    withArg { serviceInfo ->
                        verify {
                            serviceInfo.serviceName =
                                "${NSDConstants.BASE_SERVICE_NAME}_$testGameCode"
                        }
                        verify { serviceInfo.serviceType = NSDConstants.SERVICE_TYPE }
                        verify { serviceInfo.setPort(testPort) }
                    },
                    NsdManager.PROTOCOL_DNS_SD,
                    any()
                )
            }

            // --- SIMULATE AND ASSERT CALLBACK ---

            // 1. Create a mock for the NsdServiceInfo object that will be passed into our listener.
            val callbackServiceInfo = mockk<NsdServiceInfo>()
            val nsdErrorCode = NsdManager.FAILURE_INTERNAL_ERROR

            // 2. Stub ALL the methods that will be called on this mock inside the onRegistrationFailed method.
            every { callbackServiceInfo.serviceName } returns "${NSDConstants.BASE_SERVICE_NAME}_TESTCODE"
            every { callbackServiceInfo.host } returns mockk()
            every { callbackServiceInfo.port } returns testPort

            // 3. Trigger the callback on our captured listener, passing our fully stubbed mock.
            registrationListenerSlot.captured.onRegistrationFailed(
                callbackServiceInfo,
                nsdErrorCode
            )

            // --- ASSERT FINAL STATE (This part remains the same) ---
            val failedState = awaitItem()
            assertTrue(
                "Final state should be Failed",
                failedState is NsdRegistrationState.Failed
            )
            assertEquals(
                "Error string should match that in NSDConstants",
                NSDConstants.nsdErrorCodeToString(nsdErrorCode),
                (failedState as NsdRegistrationState.Failed).error
            )

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `GIVEN Registered WHEN unregisterService is failure THEN emits Failed state`() =
        runTest {
            nsdNetworkRegistration.registrationState.test {
                // --- Arrange ---
                val callbackServiceInfo = successfullyRegisterTheService()

                // --- Act ---
                nsdNetworkRegistration.unregisterService()
                val nsdErrorCode = NsdManager.FAILURE_OPERATION_NOT_RUNNING
                registrationListenerSlot.captured.onUnregistrationFailed(
                    callbackServiceInfo,
                    nsdErrorCode
                )

                // --- Assert ---
                val numOfCalls = 1
                verify(exactly = numOfCalls) {
                    nsdManagerMock.unregisterService(any())
                }

                val unRegisteringState = awaitItem()
                assertTrue(
                    "State after calling unregisterService should be UnRegistering",
                    unRegisteringState is NsdRegistrationState.UnRegistering
                )
                // Final Assertions
                val failedState = awaitItem()
                assertTrue(
                    "Final state should be Failed",
                    failedState is NsdRegistrationState.Failed
                )
                assertEquals(
                    "rror string should match that in NSDConstants",
                    NSDConstants.nsdErrorCodeToString(nsdErrorCode),
                    (failedState as NsdRegistrationState.Failed).error
                )
                cancelAndConsumeRemainingEvents()
            }
        }
    //endregion

    //region --- Edge Case / Robustness Tests ---
    @Test
    fun `GIVEN Registered WHEN registerService is called THEN it is ignored`() =
        runTest {
            nsdNetworkRegistration.registrationState.test {
                // --- Arrange ---
                successfullyRegisterTheService()
                val testGameCode = "RANDOM"
                val testPort = 11111

                // --- Act ---
                repeat(10) {
                    launch {
                        nsdNetworkRegistration.registerService(testGameCode, testPort)
                    }
                }

                // --- Assert ---
                val numOfCalls = 1
                verify(exactly = numOfCalls) {
                    nsdManagerMock.registerService(any(), any(), any())
                }

                expectNoEvents()
            }
        }

    @Test
    fun `GIVEN UnRegistered WHEN unregisterService is called THEN it is ignored`() = runTest {
        nsdNetworkRegistration.registrationState.test {
            // --- Arrange ---
            successfullyUnregisterTheService()

            // --- Act ---
            repeat(10) {
                launch {
                    nsdNetworkRegistration.unregisterService()
                }
            }

            // --- Assert ---
            val numOfCalls = 1
            verify(exactly = numOfCalls) {
                nsdManagerMock.unregisterService(any())
            }

            expectNoEvents()
        }
    }

    // Unregister when Idle
    @Test
    fun `GIVEN Idle state WHEN unregisterService is called THEN it is ignored`() = runTest {
        nsdNetworkRegistration.registrationState.test {
            // --- Arrange ---
            // Consume Idle state
            val initialState = awaitItem()
            assertTrue(
                "Expected initial state: Idle. Found $initialState",
                initialState is NsdRegistrationState.Idle
            )

            // --- Act ---
            repeat(10) {
                launch {
                    nsdNetworkRegistration.unregisterService()
                }
            }

            // --- Assert ---
            val numOfCalls = 0
            verify(exactly = numOfCalls) {
                nsdManagerMock.unregisterService(any())
            }

            expectNoEvents()
        }
    }

    // Register when unRegistered
    @Test
    fun `GIVEN UnRegistered WHEN registerService is success THEN emits Registered state`() =
        runTest {
            nsdNetworkRegistration.registrationState.test {
                // --- Arrange ---
                successfullyUnregisterTheService()

                // --- Act --- && --- Assert ---
                successfullyRegisterTheService(checkForIdleState = false)

                cancelAndConsumeRemainingEvents()
            }
        }

    // Register and Unregister twice
    @Test
    fun `GIVEN Registered after UnRegistered WHEN unregisterService is success THEN emits UnRegistered state`() =
        runTest {
            nsdNetworkRegistration.registrationState.test {
                // --- Arrange ---
                successfullyUnregisterTheService()
                val callbackServiceInfo =
                    successfullyRegisterTheService(checkForIdleState = false)

                // --- Act ---
                nsdNetworkRegistration.unregisterService()
                registrationListenerSlot.captured.onServiceUnregistered(callbackServiceInfo)

                // --- Assert ---
                val unRegisteringState = awaitItem()
                assertTrue(
                    "State after calling unregisterService should be UnRegistering",
                    unRegisteringState is NsdRegistrationState.UnRegistering
                )

                val unregisteredState = awaitItem()
                assertTrue(
                    "Final state should be UnRegistered",
                    unregisteredState is NsdRegistrationState.UnRegistered
                )

                assertEquals(
                    "Service name in state should match callback",
                    callbackServiceInfo.serviceName,
                    (unregisteredState as NsdRegistrationState.UnRegistered).serviceName
                )

                cancelAndConsumeRemainingEvents()
            }
        }

    // Parallel Registers
    @Test
    fun `GIVEN Idle state WHEN registerService is called concurrently THEN only one succeeds and state becomes Registered`() =
        runTest {
            nsdNetworkRegistration.registrationState.test {
                // --- Arrange ---
                assertEquals("Initial state should be Idle", NsdRegistrationState.Idle, awaitItem())

                // ACT: Launch 10 coroutines that will all try to register at once.
                repeat(10) {
                    launch {
                        nsdNetworkRegistration.registerService("RACE_CODE", 54321)
                    }
                }

                // This is a key command from kotlinx-coroutines-test. It tells the dispatcher
                // to execute all the coroutines that have been queued up until there's no more
                // immediate work to do. This simulates them all "running at once".
                advanceUntilIdle()

                // --- ASSERT ---
                // 1. One (and only one) of the calls should have successfully changed the state to `Registering`.
                // 2. The other 9 calls should have hit the guard clause after the first one changed the state, and returned.

                // Assert that the state transitioned to Registering
                assertEquals(NsdRegistrationState.Registering, awaitItem())

                // Verify that the underlying NsdManager.registerService was called exactly ONCE.
                // This is the most important assertion for proving the race condition was handled.
                verify(exactly = 1) {
                    nsdManagerMock.registerService(
                        withArg { serviceInfo ->
                            verify {
                                serviceInfo.serviceName =
                                    "${NSDConstants.BASE_SERVICE_NAME}_RACE_CODE"
                            }
                            verify { serviceInfo.serviceType = NSDConstants.SERVICE_TYPE }
                            verify { serviceInfo.setPort(54321) }
                        },
                        NsdManager.PROTOCOL_DNS_SD,
                        any()
                    )
                }

                // Now, we can continue the test as normal by simulating the success callback.
                val callbackServiceInfo = mockk<NsdServiceInfo>()
                every { callbackServiceInfo.serviceName } returns "${NSDConstants.BASE_SERVICE_NAME}_RACE_CODE"
                every { callbackServiceInfo.host } returns mockk()
                every { callbackServiceInfo.port } returns 54321

                // No need to mock host/port if the onServiceRegistered method doesn't use them in this path

                registrationListenerSlot.captured.onServiceRegistered(callbackServiceInfo)

                val registeredState = awaitItem()
                assertTrue(registeredState is NsdRegistrationState.Registered)

                // Finally, ensure no other strange events happened.
                expectNoEvents()
            }
        }

    // Parallel Unregisters
    @Test
    fun `GIVEN Registered state WHEN unregisterService is called concurrently THEN only one succeeds and state becomes UnRegistered`() =
        runTest {
            nsdNetworkRegistration.registrationState.test {
                // --- Arrange ---
                successfullyRegisterTheService()

                // ACT: Launch 10 coroutines that will all try to unregister at once.
                repeat(10) {
                    launch {
                        nsdNetworkRegistration.unregisterService()
                    }
                }

                // This is a key command from kotlinx-coroutines-test. It tells the dispatcher
                // to execute all the coroutines that have been queued up until there's no more
                // immediate work to do. This simulates them all "running at once".
                advanceUntilIdle()

                // --- ASSERT ---
                // 1. One (and only one) of the calls should have successfully changed the state to `UnRegistering`.
                // 2. The other 9 calls should have hit the guard clause after the first one changed the state, and returned.

                // Assert that the state transitioned to UnRegistering
                assertEquals(NsdRegistrationState.UnRegistering, awaitItem())

                // Verify that the underlying NsdManager.registerService was called exactly ONCE.
                // This is the most important assertion for proving the race condition was handled.
                verify(exactly = 1) {
                    nsdManagerMock.unregisterService(
                        any()
                    )
                }

                // Now, we can continue the test as normal by simulating the success callback.
                val callbackServiceInfo = mockk<NsdServiceInfo>()
                every { callbackServiceInfo.serviceName } returns "${NSDConstants.BASE_SERVICE_NAME}_RACE_CODE"
                every { callbackServiceInfo.host } returns mockk()
                every { callbackServiceInfo.port } returns 54321

                // No need to mock host/port if the onServiceRegistered method doesn't use them in this path

                registrationListenerSlot.captured.onServiceUnregistered(callbackServiceInfo)

                val unregisteredState = awaitItem()
                assertTrue(unregisteredState is NsdRegistrationState.UnRegistered)

                // Finally, ensure no other strange events happened.
                expectNoEvents()
            }
        }
    //endregion

    // region --- Helper Functions For Modularity ---
    /**
     * A private helper function to perform the common "arrange" step of getting the
     * SUT into a successfully registered state.
     */
    private suspend fun TurbineTestContext<NsdRegistrationState>.successfullyRegisterTheService(
        checkForIdleState: Boolean = true
    ): NsdServiceInfo {
        // Consume the initial Idle state
        if (checkForIdleState) assertEquals(NsdRegistrationState.Idle, awaitItem())

        // ACT
        val serviceName = "TESTCODE"
        val port = 12345
        nsdNetworkRegistration.registerService(serviceName, port)

        // Consume the 'Registering' state
        assertEquals(NsdRegistrationState.Registering, awaitItem())

        // SIMULATE
        val callbackServiceInfo = mockk<NsdServiceInfo>()
        every { callbackServiceInfo.serviceName } returns "RegisteredService_$serviceName"
        every { callbackServiceInfo.host } returns mockk()
        every { callbackServiceInfo.port } returns port
        registrationListenerSlot.captured.onServiceRegistered(callbackServiceInfo)

        // Consume the 'Registered' state to confirm we are in the desired state.
        assertTrue(awaitItem() is NsdRegistrationState.Registered)

        return callbackServiceInfo
    }

    /**
     * A private helper function to perform the common "arrange" step of getting the
     * SUT into a successfully unregistered state.
     */
    private suspend fun TurbineTestContext<NsdRegistrationState>.successfullyUnregisterTheService(
        checkForIdleState: Boolean = true
    ): String {
        // Arrange
        val callbackServiceInfo = successfullyRegisterTheService(checkForIdleState)

        // Act
        nsdNetworkRegistration.unregisterService()
        registrationListenerSlot.captured.onServiceUnregistered(callbackServiceInfo)

        // Assert
        assertTrue(
            "State after calling unregisterService should be UnRegistering",
            awaitItem() is NsdRegistrationState.UnRegistering
        )

        val unregisteredState = awaitItem()
        assertTrue(
            "Final state should be UnRegistered",
            unregisteredState is NsdRegistrationState.UnRegistered
        )
        assertEquals(
            "Service name in state should match callback",
            callbackServiceInfo.serviceName,
            (unregisteredState as NsdRegistrationState.UnRegistered).serviceName
        )

        return callbackServiceInfo.serviceName
    }

    //endregion

    @After
    fun tearDown() {
        unmockkAll()
    }
}
