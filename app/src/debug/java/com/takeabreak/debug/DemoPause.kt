package com.takeabreak.debug

import android.content.Context
import com.takeabreak.domain.model.Pause
import com.takeabreak.platform.usage.UsageEventsReader
import java.time.Instant
import java.time.LocalDateTime

// Solo en debug: pausa pedida desde TaB diagnóstico para la demo (US-01). Se guarda hasta la próxima
// lectura y después sigue el mismo camino que una real: reglas, Room, aviso, Inicio y Actividad
interface DemoPauseStore {
    fun put(pause: Pause)

    // Devuelve la pausa pendiente y la borra, para que no se cuente dos veces
    fun take(): Pause?
}

class PrefsDemoPauseStore(context: Context) : DemoPauseStore {

    private val prefs = context.getSharedPreferences("demo_pause", Context.MODE_PRIVATE)

    override fun put(pause: Pause) {
        prefs.edit().putString(START, pause.start.toString()).putString(END, pause.end.toString()).apply()
    }

    override fun take(): Pause? {
        val start = prefs.getString(START, null) ?: return null
        val end = prefs.getString(END, null) ?: return null
        prefs.edit().clear().apply()
        return Pause(LocalDateTime.parse(start), LocalDateTime.parse(end))
    }

    private companion object {
        const val START = "start"
        const val END = "end"
    }
}

// Suma la pausa simulada, si hay una, a las que leyó el lector real
class DemoPauseReader(
    private val real: UsageEventsReader,
    private val store: DemoPauseStore,
) : UsageEventsReader {
    override suspend fun closedPausesSince(checkpoint: Instant): List<Pause> =
        real.closedPausesSince(checkpoint) + listOfNotNull(store.take())
}
