package com.takeabreak.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takeabreak.R
import com.takeabreak.domain.model.Redemption
import com.takeabreak.domain.model.RedemptionStatus
import com.takeabreak.presentation.redemptions.RedemptionRow
import com.takeabreak.presentation.redemptions.RedemptionsViewModel
import com.takeabreak.ui.components.StateContent
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

@Composable
fun RedemptionsScreen(viewModel: RedemptionsViewModel = viewModel(factory = RedemptionsViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Una sola fuente de hora para las cuentas regresivas de toda la lista
    val now by produceState(Instant.now()) {
        while (true) {
            delay(1_000)
            value = Instant.now()
        }
    }

    StateContent(state, emptyText = stringResource(R.string.redemptions_empty)) { rows ->
        LazyColumn(Modifier.fillMaxSize()) {
            items(rows, key = { it.redemption.id }) { row -> RedemptionItem(row, now) }
        }
    }
}

@Composable
private fun RedemptionItem(row: RedemptionRow, now: Instant) {
    val status = row.redemption.statusAt(now)
    var codeVisible by rememberSaveable(row.redemption.id) { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(row.title ?: stringResource(R.string.redemptions_unknown_reward)) },
        supportingContent = {
            if (status == RedemptionStatus.ACTIVE && codeVisible) {
                Text(row.redemption.code.orEmpty(), style = MaterialTheme.typography.headlineMedium)
            } else {
                Text(statusText(row.redemption, status, now))
            }
        },
        trailingContent = {
            if (status == RedemptionStatus.ACTIVE) {
                TextButton(onClick = { codeVisible = !codeVisible }) {
                    Text(stringResource(if (codeVisible) R.string.redemptions_hide else R.string.redemptions_show))
                }
            } else {
                Text(stringResource(R.string.points, row.redemption.points))
            }
        },
    )
}

@Composable
private fun statusText(redemption: Redemption, status: RedemptionStatus, now: Instant): String = when (status) {
    RedemptionStatus.ACTIVE -> {
        val left = Duration.between(now, redemption.expiresAt ?: now).seconds.coerceAtLeast(0)
        stringResource(R.string.redemptions_expires_in, (left / 60).toInt(), (left % 60).toInt())
    }
    RedemptionStatus.USED -> stringResource(R.string.redemptions_used)
    RedemptionStatus.EXPIRED -> stringResource(R.string.redemptions_expired)
    RedemptionStatus.REFUNDED -> stringResource(R.string.redemptions_refunded)
}
