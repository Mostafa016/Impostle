package com.mostafa.impostle.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.mostafa.impostle.di.GamePermissionsDataStore
import com.mostafa.impostle.domain.model.AppPermission
import com.mostafa.impostle.domain.repository.PermissionsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStorePermissionsRepository
    @Inject
    constructor(
        @GamePermissionsDataStore private val dataStore: DataStore<Preferences>,
    ) : PermissionsRepository {
        override val permissionRequestState: Flow<Map<AppPermission, Boolean>> =
            dataStore.data.map { prefs ->
                AppPermission.entries.associateWith { permission ->
                    prefs[booleanPreferencesKey(permission.prefsKey)] ?: false
                }
            }

        override suspend fun hasRequested(permission: AppPermission): Boolean =
            dataStore.data
                .map { prefs -> prefs[booleanPreferencesKey(permission.prefsKey)] ?: false }
                .first()

        override suspend fun markRequested(permission: AppPermission) {
            dataStore.edit { prefs ->
                prefs[booleanPreferencesKey(permission.prefsKey)] = true
            }
        }
    }

private val AppPermission.prefsKey: String
    get() =
        when (this) {
            AppPermission.POST_NOTIFICATIONS -> "perm_requested_notifications"
        }
