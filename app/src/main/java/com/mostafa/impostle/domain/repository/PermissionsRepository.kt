package com.mostafa.impostle.domain.repository

import com.mostafa.impostle.domain.model.AppPermission
import kotlinx.coroutines.flow.Flow

interface PermissionsRepository {
    val permissionRequestState: Flow<Map<AppPermission, Boolean>>

    suspend fun hasRequested(permission: AppPermission): Boolean

    suspend fun markRequested(permission: AppPermission)
}
