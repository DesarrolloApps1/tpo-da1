package com.takeabreak.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takeabreak.R
import com.takeabreak.presentation.activity.ActivityViewModel
import com.takeabreak.ui.components.StateContent

// TODO: gráfico semanal y motivo legible de cada pausa que no sumó (frames 23 y 24)
@Composable
fun ActivityScreen(viewModel: ActivityViewModel = viewModel(factory = ActivityViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    StateContent(state, emptyText = stringResource(R.string.activity_empty)) { sessions ->
        LazyColumn(Modifier.fillMaxSize()) {
            items(sessions, key = { it.start.toString() }) { session ->
                ListItem(
                    headlineContent = { Text(session.start.toString()) },
                    supportingContent = { Text(session.status.name) },
                    trailingContent = { Text(stringResource(R.string.points, session.points)) },
                )
            }
        }
    }
}
