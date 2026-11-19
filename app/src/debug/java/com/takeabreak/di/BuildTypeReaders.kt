package com.takeabreak.di

import android.content.Context
import com.takeabreak.debug.DemoPauseReader
import com.takeabreak.debug.PrefsDemoPauseStore
import com.takeabreak.platform.usage.UsageEventsReader

// Debug: el lector real más la pausa simulada de TaB diagnóstico. En release esta función devuelve
// el lector real tal cual (src/release), así que la pausa simulada no existe en el APK final
fun withDemoPauses(context: Context, real: UsageEventsReader): UsageEventsReader =
    DemoPauseReader(real, PrefsDemoPauseStore(context))
