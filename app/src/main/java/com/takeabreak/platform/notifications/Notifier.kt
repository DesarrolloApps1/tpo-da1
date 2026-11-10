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
import com.takeabreak.domain.model.Reward
import com.takeabreak.ui.navigation.DeepLinks

interface Notifier {
    // Las pausas que sumaron en una misma lectura van en un solo aviso: después de días sin abrir
    // la app pueden ser muchas. reachedGoal: el premio meta si el saldo ya alcanza, para abrirlo directo
    suspend fun breaksValidated(sessions: List<BreakSession>, reachedGoal: Reward? = null)
}

class AndroidNotifier(private val context: Context) : Notifier {

    init {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    // Siempre el mismo id: un aviso nuevo reemplaza al anterior en vez de apilarse
    override suspend fun breaksValidated(sessions: List<BreakSession>, reachedGoal: Reward?) {
        if (sessions.isEmpty()) return
        val points = sessions.sumOf { it.points }
        val text = if (sessions.size == 1) {
            context.getString(R.string.notification_break_validated, points)
        } else {
            context.getString(R.string.notification_breaks_validated, sessions.size, points)
        }
        show(
            id = BREAKS_NOTIFICATION_ID,
            text = text + bonusText(sessions.sumOf { it.bonusPoints }) + reachedGoal?.let { context.getString(R.string.notification_goal_reached, it.title) }.orEmpty(),
            deepLink = DeepLinks.afterBreak(reachedGoal?.id),
        )
    }

    private fun bonusText(bonus: Int): String =
        if (bonus > 0) " " + context.getString(R.string.notification_declared_bonus, bonus) else ""

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
        const val BREAKS_NOTIFICATION_ID = 1
    }
}
