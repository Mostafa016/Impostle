package com.example.nsddemo.data.util

import android.net.nsd.NsdManager

object NSDConstants {
    const val BASE_SERVICE_NAME = "Impostle"
    const val SERVICE_TYPE = "_impostle._tcp."

    fun nsdErrorCodeToString(errorCode: Int): String =
        when (errorCode) {
            NsdManager.FAILURE_ALREADY_ACTIVE -> "Operation already active."
            NsdManager.FAILURE_INTERNAL_ERROR -> "Internal error occurred."
            NsdManager.FAILURE_MAX_LIMIT -> "Maximum outstanding requests from the applications have reached."
            NsdManager.FAILURE_BAD_PARAMETERS -> "Service has failed to resolve because of bad parameters."
            NsdManager.FAILURE_OPERATION_NOT_RUNNING -> "The stop operation failed because it is not running."
            else -> "An unknown error occurred."
        }
}
