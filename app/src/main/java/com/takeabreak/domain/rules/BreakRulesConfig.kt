package com.takeabreak.domain.rules

// Parámetros de la tabla de reglas del documento. Se ajustan con los datos de la semana de
// registro manual (US-13) sin tocar el código que los aplica
data class BreakRulesConfig(
    // Horario de descanso: de restFrom a restTo del día siguiente no se suman minutos
    val restFrom: Int = 23,
    val restTo: Int = 9,
    // Minutos válidos mínimos para que una pausa sume
    val weekdayMinMinutes: Int = 30,
    val weekendMinMinutes: Int = 45,
    // Topes de puntos (1 punto por minuto válido)
    val dailyCap: Int = 240,
    val pauseCapMinutes: Int = 180,
    // Debajo de esto es mirar la hora y volver a bloquear: no se guarda en el historial
    val minRecordedMinutes: Int = 1,
    // Bonus por declarar el break desde el tile (US-14), en porcentaje de los puntos de la pausa
    val tileBonusPercent: Int = 10,
    // Tiempo desde que se toca el tile en el que tiene que empezar la pausa; después se descarta
    val tileWindowMinutes: Int = 2,
)
