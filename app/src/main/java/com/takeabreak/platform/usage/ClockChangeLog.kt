package com.takeabreak.platform.usage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.Duration
import java.time.Instant

// Guarda cuándo cambió la hora del sistema. Android no lo deja en los eventos de uso,
// así que se anota al recibir el aviso, aunque la app esté cerrada
class ClockChangeLog(context: Context) {

    private val prefs = context.getSharedPreferences("clock_changes", Context.MODE_PRIVATE)

    fun record(at: Instant) {
        // Lo más viejo que la retención de los eventos ya no sirve
        val limit = at.minus(RETENTION).toEpochMilli()
        val kept = prefs.getStringSet(KEY, emptySet()).orEmpty().filter { it.toLong() >= limit }
        prefs.edit().putStringSet(KEY, (kept + at.toEpochMilli().toString()).toSet()).apply()
    }

    fun changesBetween(from: Instant, to: Instant): List<Instant> =
        prefs.getStringSet(KEY, emptySet()).orEmpty()
            .map { Instant.ofEpochMilli(it.toLong()) }
            .filter { it in from..to }

    private companion object {
        const val KEY = "at"
        val RETENTION: Duration = Duration.ofDays(10)
    }
}

// ACTION_TIME_CHANGED está exceptuado de las restricciones de broadcasts implícitos, así que llega
// aunque la app no corra
class ClockChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_TIME_CHANGED) ClockChangeLog(context).record(Instant.now())
    }
}
