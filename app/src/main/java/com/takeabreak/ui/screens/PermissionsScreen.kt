package com.takeabreak.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.takeabreak.R
import com.takeabreak.domain.model.AppPermission
import com.takeabreak.platform.permissions.openSettingsFor
import com.takeabreak.ui.components.PillButton

// TODO(US-05): permisos de Figma (frames 06 y 07): notificaciones, detectar la vuelta de Ajustes y el caso rechazado.
// Por ahora solo completa el grafo: después del acceso, y se saltea si el acceso a uso ya está dado
@Composable
fun PermissionsScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.permissions_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.permissions_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { context.openSettingsFor(AppPermission.USAGE_ACCESS) }) {
            Text(stringResource(R.string.permissions_grant))
        }
        PillButton(stringResource(R.string.continue_label), onClick = onDone)
    }
}
