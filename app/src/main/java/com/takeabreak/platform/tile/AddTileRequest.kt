package com.takeabreak.platform.tile

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import com.takeabreak.R

// Pide al sistema agregar el tile a ajustes rápidos; el usuario confirma en un diálogo del sistema
val canRequestAddTile: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

fun Context.requestAddBreakTile() {
    if (!canRequestAddTile) return
    val statusBar = getSystemService(StatusBarManager::class.java)
    statusBar.requestAddTileService(
        ComponentName(this, BreakTileService::class.java),
        getString(R.string.tile_label),
        Icon.createWithResource(this, R.drawable.ic_tile_break),
        mainExecutor,
    ) { }
}
