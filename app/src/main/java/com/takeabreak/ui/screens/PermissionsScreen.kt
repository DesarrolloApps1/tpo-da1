package com.takeabreak.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.takeabreak.R
import com.takeabreak.di.appContainer
import com.takeabreak.domain.model.AppPermission
import com.takeabreak.domain.repository.PermissionChecker
import com.takeabreak.platform.permissions.openSettingsFor
import com.takeabreak.ui.components.PillButton
import com.takeabreak.ui.theme.Palette

// El rechazo del acceso a uso lo muestra PermissionDeniedScreen (frame 07)
// Frame 06: el acceso a uso es obligatorio; los avisos y la exclusión de batería, recomendados.
// Se saltea si el acceso a uso ya está dado
@Composable
fun PermissionsScreen(
    onDone: () -> Unit,
    checker: PermissionChecker = LocalContext.current.appContainer.permissionChecker,
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(PERMISSION_ITEMS.associate { it.permission to checker.isGranted(it.permission) }) }

    // Al volver de Ajustes o del diálogo del sistema el estado puede haber cambiado
    LifecycleResumeEffect(Unit) {
        granted = PERMISSION_ITEMS.associate { it.permission to checker.isGranted(it.permission) }
        onPauseOrDispose { }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom),
    ) {
        Text(stringResource(R.string.permissions_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        PERMISSION_ITEMS.forEach { item ->
            PermissionItem(item, granted[item.permission] == true, onAllow = { context.openSettingsFor(item.permission) })
        }
        PillButton(stringResource(R.string.continue_label), onClick = onDone)
    }
}

private data class PermissionSpec(val permission: AppPermission, val title: Int, val body: Int, val required: Boolean)

private val PERMISSION_ITEMS = listOf(
    PermissionSpec(AppPermission.USAGE_ACCESS, R.string.permissions_usage_title, R.string.permissions_body, required = true),
    PermissionSpec(AppPermission.NOTIFICATIONS, R.string.permissions_notifications_title, R.string.permissions_notifications_body, required = false),
    PermissionSpec(AppPermission.BATTERY_OPTIMIZATION, R.string.permissions_battery_title, R.string.permissions_battery_body, required = false),
)

@Composable
private fun PermissionItem(item: PermissionSpec, granted: Boolean, onAllow: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = Palette.Paper,
        border = BorderStroke(1.dp, Palette.Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(item.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    stringResource(if (item.required) R.string.permissions_required else R.string.permissions_recommended),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(stringResource(item.body), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (granted) {
                Text(stringResource(R.string.permissions_active), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            } else {
                TextButton(onClick = onAllow, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(if (item.required) R.string.permissions_grant else R.string.permissions_allow))
                }
            }
        }
    }
}
