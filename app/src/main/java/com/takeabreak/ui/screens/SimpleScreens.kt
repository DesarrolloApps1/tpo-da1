package com.takeabreak.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.takeabreak.R
import com.takeabreak.ui.components.PillButton

// Pantallas sin datos propios todavía, por eso no tienen ViewModel

// TODO: onboarding, acceso con Google o email y permisos (frames 01 a 07)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineLarge)
        PillButton(stringResource(R.string.continue_label), onClick = onDone)
    }
}

// TODO: datos de la cuenta, meta diaria y notificaciones (frame 25)
@Composable
fun ProfileScreen(onRedemptionsClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PillButton(stringResource(R.string.my_redemptions), onClick = onRedemptionsClick)
    }
}
