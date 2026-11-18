package com.takeabreak.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takeabreak.R
import com.takeabreak.domain.model.Redemption
import com.takeabreak.domain.model.RedemptionStatus
import com.takeabreak.presentation.UiState
import com.takeabreak.presentation.redeem.RedeemViewModel
import com.takeabreak.ui.components.PillButton
import com.takeabreak.ui.components.StateContent
import com.takeabreak.ui.theme.Palette
import com.takeabreak.ui.theme.PillShape
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

// Frame 19: confirmar con un botón de 64 dp que se mantiene presionado. Canjear no se puede deshacer,
// así que no hay diálogo extra. El botón también responde a TalkBack con la acción de mantener presionado
@Composable
fun RedeemScreen(
    onDone: () -> Unit,
    onDeliver: (String) -> Unit,
    viewModel: RedeemViewModel = viewModel(factory = RedeemViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.content == UiState.Empty) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HoldToConfirm(stringResource(R.string.redeem_confirm), onConfirm = viewModel::confirm)
        }
        return
    }

    StateContent(state, emptyText = "", onRetry = viewModel::confirm, onContinueOffline = onDone) { redemption ->
        CodeView(redemption, onDeliver = { onDeliver(redemption.id) }, onDone = onDone)
    }
}

// Frame 20: código con cuenta regresiva, la hora actual con segundos y un punto que late,
// para que el cajero distinga la pantalla real de una captura
@Composable
private fun CodeView(redemption: Redemption, onDeliver: () -> Unit, onDone: () -> Unit) {
    val now by produceState(Instant.now()) {
        while (true) {
            delay(1_000)
            value = Instant.now()
        }
    }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "alpha",
    )
    val active = redemption.statusAt(now) == RedemptionStatus.ACTIVE
    val left = Duration.between(now, redemption.expiresAt ?: now).seconds.coerceAtLeast(0)

    Column(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(redemption.code.orEmpty(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        if (active) {
            Text(stringResource(R.string.redemptions_expires_in, (left / 60).toInt(), (left % 60).toInt()))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(Palette.Green.copy(alpha = alpha)))
                Text(stringResource(R.string.redeem_now, now.atZone(ZoneId.systemDefault()).format(CLOCK)))
            }
            PillButton(stringResource(R.string.redemptions_deliver), onClick = onDeliver)
        } else {
            Text(stringResource(R.string.redemptions_expired))
        }
        TextButton(onClick = onDone) { Text(stringResource(R.string.done)) }
    }
}

private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

@Composable
private fun HoldToConfirm(label: String, onConfirm: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(PillShape)
                .background(Palette.Lime)
                .semantics {
                    role = Role.Button
                    contentDescription = label
                    onLongClick(label = label) {
                        onConfirm()
                        true
                    }
                }
                .pointerInput(onConfirm) {
                    detectTapGestures(onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm()
                    })
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(label, color = Palette.Forest, fontWeight = FontWeight.Bold)
        }
        Text(stringResource(R.string.redeem_hold), color = Palette.Muted)
    }
}
