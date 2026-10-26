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
import com.takeabreak.domain.model.Reward
import com.takeabreak.ui.navigation.DeepLinks

interface Notifier {
    // reachedGoal: el premio meta si el saldo ya alcanza, para abrirlo directo
    suspend fun breakValidated(session: BreakSession, reachedGoal: Reward? = null)
    suspend fun redemptionConfirmed(redemption: Redemption)
}

class AndroidNotifier(private val context: Context) : Notifier {

    init {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override suspend fun breakValidated(session: BreakSession, reachedGoal: Reward?) = show(
        id = session.start.hashCode(),
        text = context.getString(R.string.notification_break_validated, session.points) +
            reachedGoal?.let { context.getString(R.string.notification_goal_reached, it.title) }.orEmpty(),
        deepLink = DeepLinks.afterBreak(reachedGoal?.id),
    )

    // El código queda arriba de todo en Mis canjes, que es la pantalla que lo muestra
    override suspend fun redemptionConfirmed(redemption: Redemption) = show(
        id = redemption.id.hashCode(),
        text = context.getString(R.string.notification_redemption_confirmed),
        deepLink = DeepLinks.REDEMPTIONS,
    )

    // Intent explícito a MainActivity (singleTop): con la app abierta llega a onNewIntent y no se crea otra
    private fun show(id: Int, text: String, deepLink: String) {
        if (!canNotify()) return
        val intent = Intent(Intent.ACTION_VIEW, deepLink.toUri(), context, MainActivity::class.java)
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

    private companion object {
        const val CHANNEL_ID = "breaks"
    }
}
