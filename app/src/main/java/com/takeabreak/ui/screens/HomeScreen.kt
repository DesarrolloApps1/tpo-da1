package com.takeabreak.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takeabreak.R
import com.takeabreak.presentation.home.HomeViewModel
import com.takeabreak.ui.components.StateContent

// TODO: tarjeta de saldo con anillo, tarjeta de meta y filas de premio (frames 08 a 12)
@Composable
fun HomeScreen(
    onRewardClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TextButton(onClick = onProfileClick) { Text(stringResource(R.string.profile)) }
        StateContent(state, emptyText = stringResource(R.string.home_empty)) { summary ->
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.points, summary.balance), style = MaterialTheme.typography.displayMedium)
                Text(stringResource(R.string.home_today, summary.pointsToday, summary.dailyGoal))
                summary.affordable.forEach { reward ->
                    Text(reward.title, Modifier.clickable { onRewardClick(reward.id) }.padding(vertical = 12.dp))
                }
            }
        }
    }
}
