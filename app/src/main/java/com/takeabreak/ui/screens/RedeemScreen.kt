package com.takeabreak.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takeabreak.R
import com.takeabreak.domain.model.RedemptionStatus
import com.takeabreak.presentation.UiState
import com.takeabreak.presentation.redeem.RedeemViewModel
import com.takeabreak.ui.components.PillButton
import com.takeabreak.ui.components.StateContent

// TODO: botón de 64 dp que se mantiene presionado para confirmar (frame 19)
@Composable
fun RedeemScreen(
    onDone: () -> Unit,
    viewModel: RedeemViewModel = viewModel(factory = RedeemViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.content == UiState.Empty) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
            PillButton(stringResource(R.string.redeem_confirm), onClick = viewModel::confirm)
        }
        return
    }

    StateContent(state, emptyText = "", onRetry = viewModel::confirm, onContinueOffline = onDone) { redemption ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (redemption.status == RedemptionStatus.PENDING) {
                Text(stringResource(R.string.redeem_pending))
            } else {
                Text(redemption.code.orEmpty(), style = MaterialTheme.typography.displayMedium)
            }
            PillButton(stringResource(R.string.done), onClick = onDone)
        }
    }
}
