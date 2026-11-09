package com.takeabreak.platform.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.takeabreak.R
import com.takeabreak.di.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant

// "Me tomo un break": solo guarda la hora de la declaración. No crea ninguna pausa: el bonus
// se aplica después, si el sistema detecta un bloqueo dentro de la ventana
class BreakTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        scope.launch { render() }
    }

    // Tocar con la declaración vigente la cancela
    override fun onClick() {
        scope.launch {
            val container = applicationContext.appContainer
            val now = Instant.now()
            if (isActive(now)) container.settings.clearDeclaredBreak() else container.settings.declareBreak(now)
            render()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun isActive(now: Instant): Boolean {
        val container = applicationContext.appContainer
        val declaredAt = container.settings.declaredBreakAt.first() ?: return false
        return container.breakRules.isDeclarationActive(declaredAt, now)
    }

    private suspend fun render() {
        val tile = qsTile ?: return
        val active = isActive(Instant.now())
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (active) getString(R.string.tile_subtitle_active) else null
        }
        tile.updateTile()
    }
}
