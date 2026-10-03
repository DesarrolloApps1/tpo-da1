package com.takeabreak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.takeabreak.R
import com.takeabreak.presentation.ScreenState
import com.takeabreak.presentation.UiState
import com.takeabreak.ui.theme.PillShape

// Dibuja carga, vacío, error y el aviso offline. El contenido lo pone cada pantalla
@Composable
fun <T> StateContent(
    state: ScreenState<T>,
    emptyText: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        if (state.isOffline) OfflineBanner()
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (val ui = state.content) {
                UiState.Loading -> CircularProgressIndicator()
                UiState.Empty -> Text(emptyText, Modifier.padding(24.dp))
                is UiState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(ui.message ?: stringResource(R.string.error_generic))
                    if (onRetry != null) PillButton(stringResource(R.string.retry), onRetry)
                }
                is UiState.Content -> content(ui.data)
            }
        }
    }
}

@Composable
private fun OfflineBanner() {
    Text(
        text = stringResource(R.string.offline_banner),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(12.dp),
    )
}

// Acción principal: 56 dp de alto y abajo, al alcance del pulgar
@Composable
fun PillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = PillShape,
        modifier = modifier.widthIn(max = 320.dp).fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Text(text)
    }
}
