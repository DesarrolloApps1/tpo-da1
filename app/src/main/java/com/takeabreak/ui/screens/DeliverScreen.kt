package com.takeabreak.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takeabreak.R
import com.takeabreak.presentation.deliver.DeliverViewModel
import com.takeabreak.presentation.deliver.DeliverState
import com.takeabreak.ui.components.PillButton
import com.takeabreak.ui.theme.Palette

// Confirmar entrega: el cajero escribe el PIN del local en el teléfono del cliente.
// Sin conexión no se puede confirmar: el servidor es quien marca el canje como usado
@Composable
fun DeliverScreen(
    onDone: () -> Unit,
    viewModel: DeliverViewModel = viewModel(factory = DeliverViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        when (state.phase) {
            DeliverViewModel.Phase.Delivered -> Result(stringResource(R.string.deliver_done), onDone)
            DeliverViewModel.Phase.Rejected -> Result(stringResource(R.string.deliver_rejected), onDone)
            DeliverViewModel.Phase.AlreadyUsed -> Result(stringResource(R.string.deliver_already_used), onDone)
            DeliverViewModel.Phase.Expired -> Result(stringResource(R.string.deliver_expired), onDone)
            DeliverViewModel.Phase.NoConnection -> Result(stringResource(R.string.deliver_no_connection), onDone)
            DeliverViewModel.Phase.Failed -> Result(stringResource(R.string.deliver_failed), onDone)
            DeliverViewModel.Phase.Sending -> Text(stringResource(R.string.deliver_sending))
            DeliverViewModel.Phase.Entering -> Entering(state, viewModel::digit, viewModel::delete)
        }
    }
}

@Composable
private fun Entering(state: DeliverState, onDigit: (Char) -> Unit, onDelete: () -> Unit) {
    Text(stringResource(R.string.deliver_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    Text(stringResource(R.string.deliver_subtitle), color = Palette.Muted, textAlign = TextAlign.Center)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(DeliverViewModel.PIN_LENGTH) { index ->
            val filled = index < state.pin.length
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (filled) Palette.Forest else Palette.Line),
            )
        }
    }
    state.attemptsLeft?.let { left ->
        Text(stringResource(R.string.deliver_wrong_pin, left), color = Palette.Red, textAlign = TextAlign.Center)
    }
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
    keys.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            row.forEach { key ->
                when (key) {
                    "" -> Box(Modifier.size(72.dp))
                    "⌫" -> Keypad(key, stringResource(R.string.deliver_delete)) { onDelete() }
                    else -> Keypad(key, key) { onDigit(key.first()) }
                }
            }
        }
    }
}

@Composable
private fun Keypad(label: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Palette.Paper)
            .semantics { contentDescription = description }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.headlineSmall, color = Palette.Ink)
    }
}

@Composable
private fun Result(message: String, onDone: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(message, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        PillButton(stringResource(R.string.done), onClick = onDone)
    }
}
