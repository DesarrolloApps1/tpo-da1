package com.takeabreak.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takeabreak.R
import com.takeabreak.domain.model.AppPermission
import com.takeabreak.domain.model.Profile
import com.takeabreak.platform.permissions.openSettingsFor
import com.takeabreak.presentation.profile.ProfileViewModel
import com.takeabreak.ui.components.InitialAvatar
import com.takeabreak.ui.components.StateContent
import com.takeabreak.ui.theme.Palette

// Diálogos abiertos; se guardan para que sobrevivan a la rotación
private enum class ProfileDialog { GOAL, DAILY_GOAL, SIGN_OUT }

// Perfil y ajustes (frame 25)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onRedemptionsClick: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var dialog by rememberSaveable { mutableStateOf<ProfileDialog?>(null) }

    // Al volver de Ajustes el estado de los permisos puede haber cambiado
    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissions()
        onPauseOrDispose { }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.profile_back))
            }
            Text(stringResource(R.string.profile), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        StateContent(state, emptyText = "") { profile ->
            ProfileContent(
                profile = profile,
                permissions = permissions,
                onRedemptionsClick = onRedemptionsClick,
                onNotificationsChange = viewModel::setNotifications,
                onPermissionClick = { context.openSettingsFor(it) },
                onOpenDialog = { dialog = it },
            )
            when (dialog) {
                ProfileDialog.GOAL -> GoalDialog(profile, onPick = viewModel::chooseGoal, onDismiss = { dialog = null })
                ProfileDialog.DAILY_GOAL -> DailyGoalDialog(profile.dailyGoal, onPick = viewModel::chooseDailyGoal, onDismiss = { dialog = null })
                ProfileDialog.SIGN_OUT -> SignOutDialog(onConfirm = viewModel::signOut, onDismiss = { dialog = null })
                null -> Unit
            }
        }
    }
}

@Composable
private fun ProfileContent(
    profile: Profile,
    permissions: Map<AppPermission, Boolean>,
    onRedemptionsClick: () -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onPermissionClick: (AppPermission) -> Unit,
    onOpenDialog: (ProfileDialog) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InitialAvatar(profile.account, size = 72.dp)
            Column(Modifier.weight(1f)) {
                profile.account?.let {
                    Text(
                        it.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(it.email, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(R.string.profile_total, stringResource(R.string.points, profile.totalEarned), Modifier.weight(1f))
            StatCard(R.string.profile_redemptions_count, profile.redemptionCount.toString(), Modifier.weight(1f))
        }

        SettingsCard {
            SettingRow(
                icon = Icons.Filled.ShoppingCart,
                label = stringResource(R.string.my_redemptions),
                value = profile.activeRedemptions.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.profile_active, it, it) },
                valueColor = MaterialTheme.colorScheme.primary,
                onClick = onRedemptionsClick,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            SettingRow(
                icon = Icons.Filled.Star,
                label = stringResource(R.string.profile_goal),
                value = profile.goal?.title ?: stringResource(R.string.profile_goal_none),
                onClick = { onOpenDialog(ProfileDialog.GOAL) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            SettingRow(
                icon = Icons.Filled.DateRange,
                label = stringResource(R.string.profile_daily_goal),
                value = formatMinutes(profile.dailyGoal),
                onClick = { onOpenDialog(ProfileDialog.DAILY_GOAL) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            NotificationsRow(profile.notificationsEnabled, onNotificationsChange)
        }

        Text(stringResource(R.string.profile_permissions), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        SettingsCard {
            PERMISSION_ROWS.forEachIndexed { index, (permission, label, icon) ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                val granted = permissions[permission] == true
                SettingRow(
                    icon = icon,
                    label = stringResource(label),
                    value = stringResource(if (granted) R.string.permission_granted else R.string.permission_denied),
                    valueColor = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    onClick = { onPermissionClick(permission) },
                )
            }
        }

        SettingRow(
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            label = stringResource(R.string.profile_sign_out),
            tint = MaterialTheme.colorScheme.error,
            showChevron = false,
            onClick = { onOpenDialog(ProfileDialog.SIGN_OUT) },
        )
        Spacer(Modifier.size(8.dp))
    }
}

private val PERMISSION_ROWS = listOf(
    Triple(AppPermission.USAGE_ACCESS, R.string.permission_usage, Icons.Filled.Lock),
    Triple(AppPermission.NOTIFICATIONS, R.string.permission_notifications, Icons.Filled.Notifications),
    Triple(AppPermission.LOCATION, R.string.permission_location, Icons.Filled.LocationOn),
)

// Toda la fila cambia el interruptor, no solo el Switch
@Composable
private fun NotificationsRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Switch) { onChange(!enabled) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.profile_notifications), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.profile_notifications_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = null)
    }
}

@Composable
private fun StatCard(@StringRes label: Int, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column { content() }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    value: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    tint: Color = MaterialTheme.colorScheme.primary,
    showChevron: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = if (showChevron) Color.Unspecified else tint)
        value?.let {
            Text(it, Modifier.weight(1f, fill = false), color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (showChevron) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Palette.Muted)
    }
}

@Composable
private fun GoalDialog(profile: Profile, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    OptionsDialog(
        title = stringResource(R.string.profile_goal_pick),
        options = profile.catalog.map { it.id to "${it.title} · ${stringResource(R.string.points, it.points)}" },
        selected = profile.goal?.id,
        emptyText = stringResource(R.string.profile_goal_empty),
        onPick = onPick,
        onDismiss = onDismiss,
    )
}

@Composable
private fun DailyGoalDialog(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    OptionsDialog(
        title = stringResource(R.string.profile_daily_goal),
        options = ProfileViewModel.DAILY_GOAL_OPTIONS.map { it to formatMinutes(it) },
        selected = current,
        emptyText = "",
        onPick = onPick,
        onDismiss = onDismiss,
    )
}

// Elegir una opción la aplica y cierra el diálogo
@Composable
private fun <T> OptionsDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T?,
    emptyText: String,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (options.isEmpty()) {
                Text(emptyText)
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    options.forEach { (value, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(selected = value == selected, role = Role.RadioButton) {
                                    onPick(value)
                                    onDismiss()
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = value == selected, onClick = null)
                            Text(label, Modifier.padding(start = 12.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun SignOutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_sign_out_title)) },
        text = { Text(stringResource(R.string.profile_sign_out_body)) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm()
            }) {
                Text(stringResource(R.string.profile_sign_out), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// La moneda es el tiempo: 90 se muestra como "1 h 30 min"
@Composable
private fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> stringResource(R.string.minutes, rest)
        rest == 0 -> stringResource(R.string.hours, hours)
        else -> stringResource(R.string.hours_minutes, hours, rest)
    }
}
