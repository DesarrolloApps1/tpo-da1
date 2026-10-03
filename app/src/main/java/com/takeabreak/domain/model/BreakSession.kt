package com.takeabreak.domain.model

import java.time.LocalDateTime

// Pausa ya validada, tal como queda en el historial. La hora de inicio la identifica
data class BreakSession(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val validMinutes: Int,
    val points: Int,
    val status: BreakStatus,
)

// Por qué una pausa sumó o no
enum class BreakStatus { VALID, TOO_SHORT, REST_TIME, DAILY_CAP }
