package com.takeabreak.platform.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.takeabreak.MainActivity
import com.takeabreak.R
import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.model.Redemption

interface Notifier {
    fun breakValidated(session: BreakSession)
    fun redemptionConfirmed(redemption: Redemption)
}

class AndroidNotifier(private val context: Context) : Notifier {

    init {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun breakValidated(session: BreakSession) = show(
        id = session.start.hashCode(),
        text = context.getString(R.string.notification_break_validated, session.points),
        intent = Intent(context, MainActivity::class.java),
    )

    // Abre el premio canjeado con el deep link
    override fun redemptionConfirmed(redemption: Redemption) = show(
        id = redemption.id.hashCode(),
        text = context.getString(R.string.notification_redemption_confirmed),
        intent = Intent(Intent.ACTION_VIEW, "$DEEP_LINK_REWARD/${redemption.rewardId}".toUri(), context, MainActivity::class.java),
    )

    private fun show(id: Int, text: String, intent: Intent) {
        if (!canNotify()) return
        val pendingIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    // Desde Android 13 hace falta el permiso en tiempo de ejecución
    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val DEEP_LINK_REWARD = "takeabreak://reward"
        private const val CHANNEL_ID = "breaks"
    }
}
