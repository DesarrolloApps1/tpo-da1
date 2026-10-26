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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.takeabreak.R
import com.takeabreak.platform.permissions.openSettingsFor
import com.takeabreak.presentation.ErrorCause
import com.takeabreak.presentation.ScreenState
import com.takeabreak.presentation.UiState
import com.takeabreak.ui.theme.Palette
import com.takeabreak.ui.theme.PillShape

// Dibuja carga, vacío y error. El contenido lo pone cada pantalla; el aviso offline, el scaffold
@Composable
fun <T> StateContent(
    state: ScreenState<T>,
    emptyText: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onContinueOffline: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val ui = state.content) {
            UiState.Loading -> CircularProgressIndicator()
            UiState.Empty -> Text(emptyText, Modifier.padding(24.dp))
            is UiState.Error -> ErrorMessage(ui.cause, onRetry, onContinueOffline)
            is UiState.Content -> content(ui.data)
        }
    }
}

// Qué pasó y qué puede hacer el usuario. Las acciones que la pantalla no pasa no se muestran
@Composable
fun ErrorMessage(
    cause: ErrorCause,
    onRetry: (() -> Unit)? = null,
    onContinueOffline: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val (title, body) = when (cause) {
        ErrorCause.NO_CONNECTION -> R.string.error_offline_title to R.string.error_offline_body
        ErrorCause.MISSING_PERMISSION -> R.string.error_permission_title to R.string.error_permission_body
        ErrorCause.UNKNOWN -> R.string.error_generic to R.string.error_generic_body
    }
    Column(
        modifier.padding(24.dp).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(stringResource(body), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (cause == ErrorCause.MISSING_PERMISSION) {
            PillButton(stringResource(R.string.open_settings), onClick = { context.openSettingsFor(null) })
        } else if (onRetry != null) {
            PillButton(stringResource(R.string.retry), onRetry)
        }
        if (cause == ErrorCause.NO_CONNECTION && onContinueOffline != null) {
            TextButton(onClick = onContinueOffline) { Text(stringResource(R.string.continue_offline)) }
        }
    }
}

// Aviso global de offline (frame 12), arriba del contenido de cada pantalla
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.offline_banner),
        style = MaterialTheme.typography.labelLarge,
        color = Palette.SandInk,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Palette.AmberBg, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
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
