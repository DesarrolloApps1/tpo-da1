package com.takeabreak.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takeabreak.R
import com.takeabreak.domain.model.HomeSummary
import com.takeabreak.domain.model.Reward
import com.takeabreak.presentation.home.HomeViewModel
import com.takeabreak.ui.components.PillButton
import com.takeabreak.ui.components.StateContent

// Frames 08 a 12: saldo, avance del día, meta y lo que ya alcanza. El avatar va en la barra de AppNavHost
@Composable
fun HomeScreen(
    onRewardClick: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    StateContent(state, emptyText = stringResource(R.string.home_empty)) { summary ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text(stringResource(R.string.points, summary.balance), style = MaterialTheme.typography.displayMedium) }
            item { DayProgress(summary) }
            if (summary.streakDays > 0) {
                item { Text(stringResource(R.string.home_streak, summary.streakDays), style = MaterialTheme.typography.titleMedium) }
            }
            summary.goal?.let { goal -> item { GoalCard(goal, summary.balance, onRewardClick) } }
            if (summary.affordable.isNotEmpty()) {
                item { Text(stringResource(R.string.home_affordable), style = MaterialTheme.typography.titleMedium) }
                items(summary.affordable, key = { it.id }) { reward ->
                    ListItem(
                        headlineContent = { Text(reward.title) },
                        supportingContent = { Text(reward.business.name) },
                        trailingContent = { Text(stringResource(R.string.points, reward.points)) },
                        modifier = Modifier.clickable { onRewardClick(reward.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayProgress(summary: HomeSummary) {
    val goal = summary.dailyGoal.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.home_today, summary.pointsToday, summary.dailyGoal))
        LinearProgressIndicator(
            progress = { (summary.pointsToday.toFloat() / goal).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// Cuánto falta para el premio meta, en puntos y en tiempo (1 punto = 1 minuto sin uso)
@Composable
private fun GoalCard(goal: Reward, balance: Int, onRewardClick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.home_goal, goal.title), style = MaterialTheme.typography.titleMedium)
        val missing = goal.points - balance
        if (missing <= 0) {
            Text(stringResource(R.string.home_goal_reached))
            PillButton(stringResource(R.string.redeem), onClick = { onRewardClick(goal.id) })
        } else {
            Text(stringResource(R.string.home_goal_missing, missing, durationLabel(missing)))
        }
    }
}

// Duración legible a partir de minutos: "45 min", "2 h" o "2 h 15 min"
@Composable
fun durationLabel(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> stringResource(R.string.minutes, rest)
        rest == 0 -> stringResource(R.string.hours, hours)
        else -> stringResource(R.string.hours_minutes, hours, rest)
    }
}
