package com.takeabreak.domain.rules

import com.takeabreak.domain.model.Pause
import java.time.Duration
import java.time.Instant

// Reglas de validación de la tabla de reglas del documento, con los valores de BreakRulesConfig
class BreakRules(val config: BreakRulesConfig = BreakRulesConfig()) {

    fun isRecorded(pause: Pause): Boolean = Duration.between(pause.start, pause.end).toMinutes() >= config.minRecordedMinutes

    fun minMinutes(pause: Pause): Int = if (pause.isWeekend) config.weekendMinMinutes else config.weekdayMinMinutes

    fun validMinutes(pause: Pause): Int = pause.minutesOutside(restFrom = config.restFrom, restTo = config.restTo)

    // Si la pausa supera el tope por pausa
    fun isOverPauseCap(validMinutes: Int): Boolean = validMinutes > config.pauseCapMinutes

    // La declaración del tile sigue vigente mientras no pasó la ventana para bloquear el teléfono
    fun isDeclarationActive(declaredAt: Instant, now: Instant): Boolean =
        !now.isBefore(declaredAt) && Duration.between(declaredAt, now).toMinutes() < config.tileWindowMinutes

    fun points(pause: Pause, pointsToday: Int): Int {
        if (pause.clockChanged) return 0
        val countable = validMinutes(pause)
        if (countable < minMinutes(pause)) return 0
        return countable
            .coerceAtMost(config.pauseCapMinutes)
            .coerceAtMost(config.dailyCap - pointsToday)
            .coerceAtLeast(0)
    }
}
