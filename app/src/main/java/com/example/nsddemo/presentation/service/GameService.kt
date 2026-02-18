package com.example.nsddemo.presentation.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.nsddemo.R
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GameService : LifecycleService() {

    @Inject
    lateinit var gameSession: GameSession

    private var sessionJob: Job? = null
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "GameService: CRITICAL SERVER CRASH: ${throwable.message}", throwable)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START_HOST -> {
                val gameCode = intent.getStringExtra(EXTRA_GAME_CODE) ?: return START_NOT_STICKY
                val playerId = intent.getStringExtra(EXTRA_PLAYER_ID) ?: return START_NOT_STICKY
                startForegroundSession(isHost = true, gameCode = gameCode, playerId)
            }

            ACTION_START_JOIN -> {
                val gameCode = intent.getStringExtra(EXTRA_GAME_CODE) ?: return START_NOT_STICKY
                val playerId = intent.getStringExtra(EXTRA_PLAYER_ID) ?: return START_NOT_STICKY
                startForegroundSession(isHost = false, gameCode = gameCode, playerId)
            }

            ACTION_STOP -> stopSession()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundSession(isHost: Boolean, gameCode: String, playerId: String) {
        startForeground(NOTIFICATION_ID, buildNotification(gameCode))

        sessionJob?.cancel()

        sessionJob = lifecycleScope.launch(Dispatchers.IO + exceptionHandler) {
            launch {
                gameSession.sessionState.collect {
                    Log.d(TAG, "GameService: SessionState = $it")
                }
            }
            if (isHost) {
                gameSession.startHostSession(gameCode, playerId)
            } else {
                gameSession.startJoinSession(gameCode, playerId)
            }
        }
    }

    private fun stopSession() {
        lifecycleScope.launch(Dispatchers.IO) {
            gameSession.reset()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    // --- Notification Helpers ---
    private fun buildNotification(gameCode: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Add a "Stop" action directly to notification
        val stopIntent = Intent(this, GameService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("Impostle Game Active")
            .setContentText("Game Code: $gameCode").setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW) // For Android 7.1 and below
            .setContentIntent(pendingIntent).addAction(
                android.R.drawable.ic_menu_close_clear_cancel, "Exit Game", stopPendingIntent
            ).setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE).build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Active Game Session", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for your current game progress"
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "GameService: Being destroyed.")
    }

    companion object {
        const val CHANNEL_ID = "impostle_game_service_channel"
        const val NOTIFICATION_ID = 1337

        const val ACTION_START_HOST = "ACTION_START_HOST"
        const val ACTION_START_JOIN = "ACTION_START_JOIN"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_GAME_CODE = "EXTRA_GAME_CODE"
        const val EXTRA_PLAYER_ID = "EXTRA_PLAYER_ID"
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSession()
    }
}