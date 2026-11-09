package com.takeabreak.domain.model

import java.time.LocalDateTime

// Pausa ya validada, tal como queda en el historial. La hora de inicio la identifica
data class BreakSession(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val validMinutes: Int,
    val points: Int,
    val status: BreakStatus,
    // Parte de points que viene del bonus por declarar el break desde el tile (US-14)
    val bonusPoints: Int = 0,
)

// Por qué una pausa sumó o no
enum class BreakStatus { VALID, PAUSE_CAP, TOO_SHORT, REST_TIME, DAILY_CAP, CLOCK_CHANGED }
