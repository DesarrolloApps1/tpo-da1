package com.takeabreak.platform.permissions

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.takeabreak.domain.model.AppPermission
import com.takeabreak.domain.repository.PermissionChecker

class AndroidPermissionChecker(private val context: Context) : PermissionChecker {

    override fun isGranted(permission: AppPermission): Boolean = when (permission) {
        AppPermission.USAGE_ACCESS -> hasUsageAccess()
        // Cubre el permiso de Android 13 y el caso en que se apagaron desde Ajustes
        AppPermission.NOTIFICATIONS -> NotificationManagerCompat.from(context).areNotificationsEnabled()
        AppPermission.LOCATION ->
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // El acceso a uso no es un permiso de tiempo de ejecución: se consulta en AppOps.
    // Las dos variantes están deprecadas en las APIs nuevas pero siguen siendo la forma de consultarlo
    @Suppress("DEPRECATION")
    private fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }
}

// Abre la pantalla de Ajustes donde se da cada permiso; si el fabricante no la tiene, los ajustes de la app
fun Context.openSettingsFor(permission: AppPermission?) {
    val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
    val intent = when (permission) {
        AppPermission.USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        AppPermission.NOTIFICATIONS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        AppPermission.LOCATION, null -> appDetails
    }
    runCatching { startActivity(intent) }.onFailure { startActivity(appDetails) }
}
