package com.takeabreak.di

import android.content.Context
import com.takeabreak.platform.usage.UsageEventsReader

// Release: sin pausa simulada (la de la demo vive solo en src/debug)
@Suppress("UNUSED_PARAMETER")
fun withDemoPauses(context: Context, real: UsageEventsReader): UsageEventsReader = real
