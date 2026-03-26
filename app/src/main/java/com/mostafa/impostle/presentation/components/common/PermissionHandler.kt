package com.mostafa.impostle.presentation.components.common

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.mostafa.impostle.R
import com.mostafa.impostle.domain.model.AppPermission

// ─────────────────────────────────────────────
// Presentation layer mapping
// ─────────────────────────────────────────────
private val AppPermission.manifestPermission: String
    get() =
        when (this) {
            AppPermission.POST_NOTIFICATIONS -> Manifest.permission.POST_NOTIFICATIONS
        }
// ─────────────────────────────────────────────
// State exposed to the caller
// ─────────────────────────────────────────────

data class PermissionHandler(
    val isGranted: Boolean,
    val shouldShowRationale: Boolean,
    val isPermanentlyDenied: Boolean,
    val request: () -> Unit,
)

// ─────────────────────────────────────────────
// Hook — no auto-request, caller decides when
// ─────────────────────────────────────────────

@Composable
fun rememberPermissionHandler(
    appPermission: AppPermission,
    hasRequested: Boolean,
    markPermissionRequested: (AppPermission) -> Unit,
): PermissionHandler {
    val context = LocalContext.current
    val activity = context as Activity
    val manifestPermission = appPermission.manifestPermission

    var isGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, manifestPermission)
                == PackageManager.PERMISSION_GRANTED,
        )
    }
    var shouldShowRationale by remember {
        mutableStateOf(activity.shouldShowRequestPermissionRationale(manifestPermission))
    }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            isGranted = granted
            shouldShowRationale = activity.shouldShowRequestPermissionRationale(manifestPermission)
            markPermissionRequested(appPermission)
        }

    // Denied and system won't show dialog again
    val isPermanentlyDenied = hasRequested && !isGranted && !shouldShowRationale
    return remember(isGranted, shouldShowRationale, isPermanentlyDenied) {
        PermissionHandler(
            isGranted = isGranted,
            shouldShowRationale = shouldShowRationale,
            isPermanentlyDenied = isPermanentlyDenied,
            request = { launcher.launch(manifestPermission) },
        )
    }
}

// ─────────────────────────────────────────────
// Wrapper — content always shown, dialog overlays
// ─────────────────────────────────────────────

@Composable
fun WithPermission(
    appPermission: AppPermission,
    rationaleText: String,
    hasRequested: Boolean,
    markPermissionRequested: (AppPermission) -> Unit,
    onRationaleDismissed: () -> Unit = {},
    onGranted: () -> Unit = {},
    content: @Composable (
        onActionRequiringPermission: () -> Unit,
        isShowingDialog: Boolean,
    ) -> Unit,
) {
    val handler =
        rememberPermissionHandler(
            appPermission = appPermission,
            hasRequested = hasRequested,
            markPermissionRequested = markPermissionRequested,
        )
    var showRationaleDialog by remember { mutableStateOf(false) }

    // Triggered when user taps a feature that needs the permission
    val onActionRequiringPermission = {
        when {
            handler.isGranted -> onGranted()
            handler.shouldShowRationale -> showRationaleDialog = true
            else -> handler.request() // First time — go straight to system dialog
        }
    }

    // Content always renders — passes the trigger down
    content(onActionRequiringPermission, showRationaleDialog)

    // Rationale dialog — only shown when user initiates an action
    if (showRationaleDialog) {
        PermissionRationaleDialog(
            rationaleText = rationaleText,
            onConfirm = {
                showRationaleDialog = false
                handler.request()
            },
            onDismiss = {
                showRationaleDialog = false
                onRationaleDismissed()
            },
        )
    }

    // Granted callback
    if (handler.isGranted) {
        LaunchedEffect(Unit) { onGranted() }
    }
}

// ─────────────────────────────────────────────
// Rationale dialog with cancel option
// ─────────────────────────────────────────────

@Composable
private fun PermissionRationaleDialog(
    rationaleText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Text(
                text = rationaleText,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.grant_permission))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.not_now))
            }
        },
    )
}
