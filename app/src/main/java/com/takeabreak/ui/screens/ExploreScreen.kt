package com.takeabreak.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.takeabreak.presentation.explore.ExploreViewModel
import com.takeabreak.ui.components.StateContent
import java.time.Duration
import java.time.Instant

@Composable
private fun updatedAgo(synced: Instant): String {
    val minutes = Duration.between(synced, Instant.now()).toMinutes()
    return when {
        minutes < 1 -> stringResource(R.string.explore_updated_now)
        minutes < 60 -> stringResource(R.string.explore_updated_ago, stringResource(R.string.minutes, minutes.toInt()))
        else -> stringResource(R.string.explore_updated_ago, stringResource(R.string.hours, (minutes / 60).toInt()))
    }
}

// TODO: mapa con Maps Compose arriba y la lista en una hoja inferior (frames 13 a 16)
@Composable
fun ExploreScreen(
    onRewardClick: (String) -> Unit,
    viewModel: ExploreViewModel = viewModel(factory = ExploreViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lastSync by viewModel.lastSync.collectAsStateWithLifecycle()

    StateContent(state, emptyText = stringResource(R.string.explore_empty), onRetry = viewModel::refresh) { rewards ->
        LazyColumn(Modifier.fillMaxSize()) {
            lastSync?.let { synced ->
                item { Text(updatedAgo(synced), Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge) }
            }
            items(rewards, key = { it.id }) { reward ->
                ListItem(
                    headlineContent = { Text(reward.title) },
                    supportingContent = { Text(reward.business.name) },
                    trailingContent = { Text(stringResource(R.string.points, reward.points)) },
                    modifier = Modifier.fillMaxWidth().clickable { onRewardClick(reward.id) },
                )
            }
        }
    }
}
