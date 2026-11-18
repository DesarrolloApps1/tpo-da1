package com.takeabreak.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.takeabreak.presentation.UiState
import com.takeabreak.presentation.redeem.RedeemViewModel
import com.takeabreak.ui.components.PillButton
import com.takeabreak.ui.components.StateContent
import com.takeabreak.ui.theme.Palette
import com.takeabreak.ui.theme.PillShape

// Frame 19: confirmar con un botón de 64 dp que se mantiene presionado. Canjear no se puede deshacer,
// así que no hay diálogo extra. El botón también responde a TalkBack con la acción de mantener presionado
@Composable
fun RedeemScreen(
    onDone: () -> Unit,
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
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(redemption.code.orEmpty(), style = MaterialTheme.typography.displayMedium)
            PillButton(stringResource(R.string.done), onClick = onDone)
        }
    }
}

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
