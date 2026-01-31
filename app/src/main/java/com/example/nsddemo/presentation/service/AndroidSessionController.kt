package com.example.nsddemo.presentation.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidSessionController @Inject constructor(
    @ApplicationContext private val context: Context
) : SessionController {

    override fun startHost(gameCode: String, playerId: String) {
        ServiceHelper.startHost(context, gameCode, playerId)
    }

    override fun startJoin(gameCode: String, playerId: String) {
        ServiceHelper.startJoin(context, gameCode, playerId)
    }

    override fun stopSession() {
        ServiceHelper.stop(context)
    }
}