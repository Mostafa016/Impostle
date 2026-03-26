package com.mostafa.impostle.presentation.fakes

import com.mostafa.impostle.domain.model.AppPermission
import com.mostafa.impostle.domain.repository.PermissionsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakePermissionsRepository : PermissionsRepository {
    override val permissionRequestState: Flow<Map<AppPermission, Boolean>>
        get() = emptyFlow()

    override suspend fun hasRequested(permission: AppPermission): Boolean = true

    override suspend fun markRequested(permission: AppPermission) {
        return
    }
}
