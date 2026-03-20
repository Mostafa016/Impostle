package com.mostafa.impostle.presentation.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object ServiceHelper {
    fun startHost(
        context: Context,
        gameCode: String,
        playerId: String,
    ) {
        val intent =
            Intent(context, GameService::class.java).apply {
                action = GameService.ACTION_START_HOST
                putExtra(GameService.EXTRA_GAME_CODE, gameCode)
                putExtra(GameService.EXTRA_PLAYER_ID, playerId)
            }
        ContextCompat.startForegroundService(context, intent)
    }

    fun startJoin(
        context: Context,
        gameCode: String,
        playerId: String,
    ) {
        val intent =
            Intent(context, GameService::class.java).apply {
                action = GameService.ACTION_START_JOIN
                putExtra(GameService.EXTRA_GAME_CODE, gameCode)
                putExtra(GameService.EXTRA_PLAYER_ID, playerId)
            }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        val intent =
            Intent(context, GameService::class.java).apply {
                action = GameService.ACTION_STOP
            }
        context.startService(intent)
    }
}
