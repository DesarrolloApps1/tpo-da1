package com.takeabreak.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takeabreak.R
import com.takeabreak.presentation.reward.RewardDetailViewModel
import com.takeabreak.ui.components.PillButton
import com.takeabreak.ui.components.StateContent

// TODO: estado "no alcanza" con lo que falta en puntos y horas (frame 18)
@Composable
fun RewardDetailScreen(
    onRedeem: (String) -> Unit,
    viewModel: RewardDetailViewModel = viewModel(factory = RewardDetailViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    StateContent(state, emptyText = stringResource(R.string.reward_not_found)) { reward ->
        if (reward == null) return@StateContent
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(reward.title, style = MaterialTheme.typography.headlineMedium)
            Text("${reward.business.name} · ${reward.business.address}")
            Text(stringResource(R.string.points, reward.points))
            TextButton(onClick = viewModel::setAsGoal) { Text(stringResource(R.string.set_goal)) }
            PillButton(stringResource(R.string.redeem), onClick = { onRedeem(reward.id) })
        }
    }
}
