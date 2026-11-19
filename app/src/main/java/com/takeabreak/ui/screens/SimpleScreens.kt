package com.takeabreak.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takeabreak.R
import com.takeabreak.ui.components.PillButton
import com.takeabreak.ui.theme.Palette
import com.takeabreak.ui.theme.PillShape
import kotlinx.coroutines.launch

// Pantallas del primer uso, sin datos propios: por eso no tienen ViewModel

// Frame 01: mientras se lee la sesión guardada
@Composable
fun SplashScreen() {
    Column(
        Modifier.fillMaxSize().background(Palette.Forest),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(88.dp).clip(CircleShape).background(Palette.Lime), contentAlignment = Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) { Box(Modifier.width(10.dp).height(36.dp).clip(PillShape).background(Palette.Forest)) }
            }
        }
        Text(stringResource(R.string.splash_name), color = Palette.Paper, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.splash_tagline), color = Palette.Lime, style = MaterialTheme.typography.titleMedium)
    }
}

// Frames 02 a 04: tres pasos con la ecuación "1 hora = 60 pts". Se muestra solo la primera vez
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { ONBOARDING_PAGES }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == ONBOARDING_PAGES - 1
    Column(Modifier.fillMaxSize().background(Palette.Cream).padding(24.dp)) {
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterEnd) {
            if (!last) TextButton(onClick = onDone) { Text(stringResource(R.string.onboarding_skip), color = Palette.Muted) }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    when (page) {
                        0 -> EquationIllustration()
                        1 -> RewardsIllustration()
                        else -> RingIllustration()
                    }
                }
                OnboardingText(page)
            }
        }
        Row(Modifier.padding(vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(ONBOARDING_PAGES) { index ->
                val current = index == pager.currentPage
                Box(Modifier.height(8.dp).width(if (current) 24.dp else 8.dp).clip(PillShape).background(if (current) Palette.Forest else Palette.Line))
            }
        }
        PillButton(
            stringResource(if (last) R.string.onboarding_start else R.string.onboarding_next),
            onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun OnboardingText(page: Int) {
    val (first, second, body) = when (page) {
        0 -> Triple(R.string.onboarding_1_title, R.string.onboarding_1_accent, R.string.onboarding_1_body)
        1 -> Triple(R.string.onboarding_2_title, R.string.onboarding_2_accent, R.string.onboarding_2_body)
        else -> Triple(R.string.onboarding_3_title, R.string.onboarding_3_accent, R.string.onboarding_3_body)
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            buildAnnotatedString {
                append(stringResource(first))
                append("\n")
                withStyle(SpanStyle(color = Palette.Green)) { append(stringResource(second)) }
            },
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Palette.Ink,
        )
        Text(stringResource(body), style = MaterialTheme.typography.bodyLarge, color = Palette.Muted)
    }
}

// "1 hora = 60 pts" sobre un círculo lima claro
@Composable
private fun EquationIllustration() {
    Box(Modifier.size(300.dp).clip(CircleShape).background(Palette.Lime.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = PillShape, color = Palette.Paper, border = BorderStroke(2.dp, Palette.Forest)) {
                Text(stringResource(R.string.onboarding_hour), Modifier.padding(horizontal = 40.dp, vertical = 16.dp), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Palette.Forest)
            }
            Box(Modifier.size(48.dp).clip(CircleShape).background(Palette.Forest), contentAlignment = Alignment.Center) {
                Text("=", color = Palette.Lime, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Surface(shape = PillShape, color = Palette.Lime) {
                Text(stringResource(R.string.onboarding_points), Modifier.padding(horizontal = 40.dp, vertical = 16.dp), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Palette.Forest)
            }
        }
    }
}

// Tres premios de ejemplo de comercios del barrio
@Composable
private fun RewardsIllustration() {
    val examples = listOf(
        Triple(R.string.onboarding_reward_1, R.string.onboarding_reward_1_place, "120 pts"),
        Triple(R.string.onboarding_reward_2, R.string.onboarding_reward_2_place, "300 pts"),
        Triple(R.string.onboarding_reward_3, R.string.onboarding_reward_3_place, "400 pts"),
    )
    Box(Modifier.size(300.dp).clip(CircleShape).background(Palette.Pink.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            examples.forEach { (title, place, points) ->
                Surface(shape = RoundedCornerShape(20.dp), color = Palette.Paper, border = BorderStroke(1.dp, Palette.Line), modifier = Modifier.width(280.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(title), fontWeight = FontWeight.Bold, color = Palette.Ink)
                            Text(stringResource(place), style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                        }
                        Surface(shape = PillShape, color = Palette.Lime) {
                            Text(points, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, color = Palette.Forest)
                        }
                    }
                }
            }
        }
    }
}

// El anillo de una pausa en curso: no hace falta tocar nada
@Composable
private fun RingIllustration() {
    Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(progress = { 0.78f }, modifier = Modifier.size(260.dp), color = Palette.Green, trackColor = Palette.Line, strokeWidth = 18.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("47:12", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = Palette.Forest)
            Text(stringResource(R.string.onboarding_ring_label), color = Palette.Muted)
        }
    }
}

// Frame 07: sin acceso a uso no se cuentan pausas, pero no se bloquea el resto de la app
@Composable
fun PermissionDeniedScreen(onOpenSettings: () -> Unit, onSkip: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Palette.Cream).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(104.dp).clip(CircleShape).background(Palette.Red.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Text("✕", color = Palette.Red, fontSize = 40.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clearAndSetSemantics { })
        }
        Text(stringResource(R.string.denied_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Palette.Ink)
        Text(stringResource(R.string.denied_body), color = Palette.Muted)
        Surface(shape = RoundedCornerShape(24.dp), color = Palette.Paper, border = BorderStroke(1.dp, Palette.Line), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf(R.string.denied_step_1, R.string.denied_step_2, R.string.denied_step_3).forEachIndexed { index, step ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(Palette.Lime), contentAlignment = Alignment.Center) {
                            Text("${index + 1}", fontWeight = FontWeight.Bold, color = Palette.Forest)
                        }
                        Text(stringResource(step), color = Palette.Ink)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        PillButton(stringResource(R.string.denied_open_settings), onClick = onOpenSettings)
        TextButton(onClick = onSkip) { Text(stringResource(R.string.denied_skip), color = Palette.Forest, fontWeight = FontWeight.Bold) }
    }
}

private const val ONBOARDING_PAGES = 3
