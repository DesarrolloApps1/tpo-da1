package com.takeabreak.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takeabreak.R
import com.takeabreak.di.appContainer
import com.takeabreak.domain.model.AuthError
import com.takeabreak.domain.usecase.SignInWithEmail
import com.takeabreak.presentation.access.AccessState
import com.takeabreak.presentation.access.AccessViewModel
import com.takeabreak.ui.theme.Palette
import com.takeabreak.ui.theme.PillShape
import com.takeabreak.ui.theme.TakeABreakTheme

// Acceso con Google o email (frame 05). Al entrar, AppNavHost navega solo porque observa la sesión
@Composable
fun AccessScreen(viewModel: AccessViewModel = viewModel(factory = AccessViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val google = remember(context) { context.appContainer.googleIdTokenProvider(context) }

    BackHandler(enabled = state.showEmailForm, onBack = viewModel::hideEmailForm)

    AccessContent(
        state = state,
        onGoogle = { viewModel.continueWithGoogle(google) },
        onShowEmailForm = viewModel::showEmailForm,
        onHideEmailForm = viewModel::hideEmailForm,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submitEmail,
    )
}

@Composable
private fun AccessContent(
    state: AccessState,
    onGoogle: () -> Unit,
    onShowEmailForm: () -> Unit,
    onHideEmailForm: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: (register: Boolean) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Palette.Forest).imePadding()) {
        Hero(showHeadline = !state.showEmailForm, modifier = Modifier.weight(1f))
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(width = 40.dp, height = 4.dp).background(Palette.Line, PillShape))
            Text(
                stringResource(R.string.access_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (state.isLoading) {
                val loading = stringResource(R.string.access_loading)
                LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = loading })
            }
            if (state.showEmailForm) {
                EmailForm(state, onEmailChange, onPasswordChange, onSubmit, onHideEmailForm)
            } else {
                Options(state, onGoogle, onShowEmailForm)
            }
            state.error?.let { error ->
                Text(
                    error.message(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

@Composable
private fun Hero(showHeadline: Boolean, modifier: Modifier = Modifier) {
    val headline = stringResource(R.string.access_headline)
    val accent = stringResource(R.string.access_headline_accent)
    Box(modifier.fillMaxWidth().clipToBounds()) {
        // Círculo decorativo del frame 05
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 140.dp)
                .size(320.dp)
                .background(Palette.Green.copy(alpha = 0.35f), CircleShape),
        )
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Logo()
            if (showHeadline) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        buildAnnotatedString {
                            append("$headline ")
                            withStyle(SpanStyle(color = Palette.Lime)) { append(accent) }
                        },
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Palette.Paper,
                    )
                    Text(
                        stringResource(R.string.access_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Palette.Paper.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Logo() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Ícono de pausa decorativo: el nombre ya se lee al lado
        Row(
            Modifier.size(48.dp).background(Palette.Lime, CircleShape).clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(2) { Box(Modifier.size(width = 6.dp, height = 18.dp).background(Palette.Forest, PillShape)) }
        }
        Text(
            stringResource(R.string.access_logo),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Palette.Paper,
        )
    }
}

@Composable
private fun Options(state: AccessState, onGoogle: () -> Unit, onShowEmailForm: () -> Unit) {
    AccessButton(
        text = stringResource(R.string.access_google),
        onClick = onGoogle,
        filled = true,
        enabled = state.googleEnabled && !state.isLoading,
        icon = { GoogleMark() },
    )
    AccessButton(
        text = stringResource(R.string.access_email),
        onClick = onShowEmailForm,
        filled = false,
        enabled = !state.isLoading,
        icon = { Icon(Icons.Outlined.Email, contentDescription = stringResource(R.string.access_email_icon)) },
    )
    if (!state.googleEnabled) {
        Text(
            stringResource(R.string.access_google_disabled),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Text(
        stringResource(R.string.access_legal),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EmailForm(
    state: AccessState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: (register: Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = state.email,
        onValueChange = onEmailChange,
        label = { Text(stringResource(R.string.access_email_label)) },
        singleLine = true,
        enabled = !state.isLoading,
        isError = state.error == AuthError.INVALID_EMAIL,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.password,
        onValueChange = onPasswordChange,
        label = { Text(stringResource(R.string.access_password_label)) },
        supportingText = { Text(stringResource(R.string.access_password_hint, SignInWithEmail.MIN_PASSWORD_LENGTH)) },
        singleLine = true,
        enabled = !state.isLoading,
        isError = state.error == AuthError.SHORT_PASSWORD || state.error == AuthError.WRONG_CREDENTIALS,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = Modifier.fillMaxWidth(),
    )
    AccessButton(stringResource(R.string.access_register), onClick = { onSubmit(true) }, filled = true, enabled = !state.isLoading)
    AccessButton(stringResource(R.string.access_sign_in), onClick = { onSubmit(false) }, filled = false, enabled = !state.isLoading)
    TextButton(
        onClick = onBack,
        enabled = !state.isLoading,
        colors = ButtonDefaults.textButtonColors(contentColor = Palette.Forest),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Text(stringResource(R.string.access_back))
    }
}

// Pastilla de 56 dp: rellena para la acción principal, con borde para la secundaria
@Composable
private fun AccessButton(
    text: String,
    onClick: () -> Unit,
    filled: Boolean,
    enabled: Boolean,
    icon: (@Composable () -> Unit)? = null,
) {
    val modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = PillShape,
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Forest, contentColor = Palette.Paper),
            content = content,
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = PillShape,
            border = BorderStroke(2.dp, if (enabled) Palette.Forest else Palette.Line),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Palette.Forest),
            content = content,
        )
    }
}

@Composable
private fun GoogleMark() {
    val description = stringResource(R.string.access_google_icon)
    Box(
        Modifier.size(28.dp).background(Palette.Paper, CircleShape).clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text("G", color = GoogleBlue, fontWeight = FontWeight.Bold)
    }
}

private val GoogleBlue = Color(0xFF4285F4)

@Composable
private fun AuthError.message(): String = when (this) {
    AuthError.INVALID_EMAIL -> stringResource(R.string.access_error_invalid_email)
    AuthError.SHORT_PASSWORD -> stringResource(R.string.access_error_short_password, SignInWithEmail.MIN_PASSWORD_LENGTH)
    AuthError.EMAIL_TAKEN -> stringResource(R.string.access_error_email_taken)
    AuthError.WRONG_CREDENTIALS -> stringResource(R.string.access_error_wrong_credentials)
    AuthError.GOOGLE_FAILED -> stringResource(R.string.access_error_google)
    AuthError.NO_CONNECTION -> stringResource(R.string.access_error_no_connection)
    AuthError.UNKNOWN -> stringResource(R.string.error_generic)
}

@Preview
@Composable
private fun AccessPreview() {
    TakeABreakTheme {
        AccessContent(AccessState(googleEnabled = true), {}, {}, {}, {}, {}, {})
    }
}
