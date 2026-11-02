package com.niru.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Foreground service: VOICE MODE ON hone par ongoing notification dikhati hai,
 * taaki Android app ko background me band na kare. Pause aur Stop buttons notification me hain.
 * Ye service mic NAHI kholti. Mic sirf app khule hone par user ke tap se chalta hai.
 */
class VoiceService : Service() {

    companion object {
        private const val CHANNEL_ID = "niru_voice"
        private const val NOTIF_ID = 42

        const val ACTION_PAUSE_TOGGLE = "com.niru.ai.PAUSE_TOGGLE"
        const val ACTION_STOP = "com.niru.ai.STOP"
        const val ACTION_REFRESH = "com.niru.ai.REFRESH"

        @Volatile
        var paused = false

        var onStopRequested: (() -> Unit)? = null
        var onPauseChanged: ((Boolean) -> Unit)? = null

        fun start(ctx: Context) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, VoiceService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, VoiceService::class.java))
        }

        fun setPaused(ctx: Context, p: Boolean) {
            paused = p
            ContextCompat.startForegroundService(
                ctx,
                Intent(ctx, VoiceService::class.java).setAction(ACTION_REFRESH)
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_PAUSE_TOGGLE) {
            paused = !paused
            onPauseChanged?.invoke(paused)
        }

        // Android ki shart: startForegroundService ke baad turant startForeground zaroori hai
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )

        if (action == ACTION_STOP) {
            onStopRequested?.invoke()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        paused = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "NIRU वॉइस मोड", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val pauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, VoiceService::class.java).setAction(ACTION_PAUSE_TOGGLE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, VoiceService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NIRU वॉइस मोड चालू है")
            .setContentText(if (paused) "रुका हुआ है" else "बैकग्राउंड में बोल सकती है")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openApp)
            .setOngoing(true)
            .addAction(0, if (paused) "फिर चालू" else "रोकें", pauseIntent)
            .addAction(0, "बंद करें", stopIntent)
            .build()
    }
}
