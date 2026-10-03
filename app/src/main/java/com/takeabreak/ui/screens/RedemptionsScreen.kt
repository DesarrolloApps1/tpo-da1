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
import com.takeabreak.presentation.redemptions.RedemptionsViewModel
import com.takeabreak.ui.components.StateContent

// TODO: botón "Mostrar" para volver a ver el código activo (frame 26)
@Composable
fun RedemptionsScreen(viewModel: RedemptionsViewModel = viewModel(factory = RedemptionsViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    StateContent(state, emptyText = stringResource(R.string.redemptions_empty)) { redemptions ->
        LazyColumn(Modifier.fillMaxSize()) {
            items(redemptions, key = { it.id }) { redemption ->
                ListItem(
                    headlineContent = { Text(redemption.code ?: "—") },
                    supportingContent = { Text(redemption.status.name) },
                    trailingContent = { Text(stringResource(R.string.points, redemption.points)) },
                )
            }
        }
    }
}
