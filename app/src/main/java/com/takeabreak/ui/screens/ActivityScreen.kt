package com.takeabreak.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takeabreak.R
import com.takeabreak.domain.model.ActivityPeriod
import com.takeabreak.domain.model.ActivitySummary
import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.model.BreakStatus
import com.takeabreak.domain.model.DayHistory
import com.takeabreak.domain.model.DayMinutes
import com.takeabreak.domain.rules.BreakRulesConfig
import com.takeabreak.presentation.activity.ActivityViewModel
import com.takeabreak.ui.components.StateContent
import com.takeabreak.ui.theme.Palette
import com.takeabreak.ui.theme.PillShape
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Figma 23 (con pausas) y 24 (vacía)
@Composable
fun ActivityScreen(viewModel: ActivityViewModel = viewModel(factory = ActivityViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    StateContent(state, emptyText = "") { summary ->
        ActivityContent(summary, viewModel.rules, viewModel::selectPeriod)
    }
}

@Composable
private fun ActivityContent(summary: ActivitySummary, rules: BreakRulesConfig, onSelectPeriod: (ActivityPeriod) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { PeriodSelector(summary.period, onSelectPeriod) }
        item { Totals(summary) }
        item { Chart(summary) }
        if (summary.streakDays > 0) item { StreakCard(summary.streakDays, summary.recordStreakDays) }
        if (summary.days.isEmpty()) {
            item { HowYouScore(rules) }
        } else {
            summary.days.forEach { day -> dayItems(day, summary.today, rules) }
        }
    }
}

@Composable
private fun PeriodSelector(selected: ActivityPeriod, onSelect: (ActivityPeriod) -> Unit) {
    val options = listOf(ActivityPeriod.WEEK to R.string.activity_week, ActivityPeriod.MONTH to R.string.activity_month)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (period, label) ->
            SegmentedButton(
                selected = period == selected,
                onClick = { onSelect(period) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size, PillShape),
                icon = {},
            ) { Text(stringResource(label)) }
        }
    }
}

@Composable
private fun Totals(summary: ActivitySummary) {
    val isWeek = summary.period == ActivityPeriod.WEEK
    Column {
        Text(formatMinutes(summary.minutes), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(
            text = when {
                summary.minutes == 0 && isWeek -> stringResource(R.string.activity_empty_week)
                summary.minutes == 0 -> stringResource(R.string.activity_empty_month)
                isWeek -> stringResource(R.string.activity_away_week, summary.points)
                else -> stringResource(R.string.activity_away_month, summary.points)
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Barras de minutos por día con la meta diaria punteada. Hoy va en verde oscuro
@Composable
private fun Chart(summary: ActivitySummary) {
    val goal = summary.dailyGoalMinutes
    val top = maxOf(goal, summary.chart.maxOfOrNull { it.minutes } ?: 0, 1)
    val description = stringResource(R.string.activity_chart_description, formatMinutes(goal))
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Palette.Paper,
        border = BorderStroke(1.dp, Palette.Line),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = description },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.activity_goal, formatMinutes(goal)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End),
            )
            Box(Modifier.fillMaxWidth().height(ChartHeight)) {
                val goalFraction = goal / top.toFloat()
                Canvas(Modifier.matchParentSize()) {
                    val y = size.height * (1 - goalFraction)
                    drawLine(
                        color = Palette.Muted.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
                    )
                }
                Row(
                    modifier = Modifier.matchParentSize(),
                    horizontalArrangement = Arrangement.spacedBy(if (summary.period == ActivityPeriod.WEEK) 12.dp else 2.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    summary.chart.forEach { Bar(it, top, isToday = it.day == summary.today, Modifier.weight(1f)) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (summary.period == ActivityPeriod.WEEK) 12.dp else 2.dp)) {
                summary.chart.forEach { day ->
                    Text(
                        text = dayLabel(day.day, summary.period),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (day.day == summary.today) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Bar(day: DayMinutes, top: Int, isToday: Boolean, modifier: Modifier) {
    val height = if (day.minutes == 0) EmptyBarHeight else (ChartHeight * (day.minutes / top.toFloat())).coerceAtLeast(EmptyBarHeight)
    val color = when {
        isToday -> Palette.Forest
        day.minutes == 0 -> Palette.Line
        else -> Mint
    }
    Box(modifier.height(height).clip(RoundedCornerShape(8.dp)).background(color))
}

@Composable
private fun StreakCard(days: Int, record: Int) {
    Surface(shape = MaterialTheme.shapes.large, color = Palette.AmberBg, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = Palette.Amber)
            Text(
                pluralStringResource(R.plurals.activity_streak, days, days),
                fontWeight = FontWeight.Bold,
                color = Palette.SandInk,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            Text(stringResource(R.string.activity_record, record), color = Palette.SandInk)
        }
    }
}

private fun LazyListScope.dayItems(day: DayHistory, today: LocalDate, rules: BreakRulesConfig) {
    item(key = "day-${day.day}") {
        Text(
            dayTitle(day.day, today).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    items(day.sessions, key = { it.start.toString() }) { SessionRow(it, rules) }
}

@Composable
private fun SessionRow(session: BreakSession, rules: BreakRulesConfig) {
    val summed = session.points > 0
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(if (summed) Mint else Palette.Line),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (summed) Icons.Filled.Check else Icons.Outlined.Info,
                contentDescription = null,
                tint = if (summed) Palette.Green else Palette.Muted,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.activity_session, session.start.format(TimeFormat), session.end.format(TimeFormat)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.activity_session_detail, formatMinutes(Duration.between(session.start, session.end).toMinutes().toInt()), reason(session, rules)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(shape = PillShape, color = if (summed) Mint else Palette.Line) {
            Text(
                if (summed) stringResource(R.string.activity_points_earned, session.points) else stringResource(R.string.points, 0),
                fontWeight = FontWeight.Bold,
                color = if (summed) Palette.Green else Palette.Muted,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

// Por qué sumó o no, en lenguaje simple (principio P4)
@Composable
private fun reason(session: BreakSession, rules: BreakRulesConfig): String = when (session.status) {
    BreakStatus.VALID ->
        if (session.bonusPoints > 0) stringResource(R.string.reason_declared_bonus, session.bonusPoints)
        else stringResource(R.string.reason_valid)
    BreakStatus.PAUSE_CAP -> stringResource(R.string.reason_pause_cap, rules.pauseCapMinutes / 60)
    BreakStatus.TOO_SHORT ->
        if (session.start.dayOfWeek.value >= 6) stringResource(R.string.reason_too_short_weekend, rules.weekendMinMinutes)
        else stringResource(R.string.reason_too_short, rules.weekdayMinMinutes)
    BreakStatus.REST_TIME -> stringResource(R.string.reason_rest_time)
    BreakStatus.DAILY_CAP -> stringResource(R.string.reason_daily_cap)
    BreakStatus.CLOCK_CHANGED -> stringResource(R.string.reason_clock_changed)
}

// Figma 24: con el historial vacío se explica cómo se suman puntos, con los valores de las reglas
@Composable
private fun HowYouScore(rules: BreakRulesConfig) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.activity_how_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Surface(
            shape = MaterialTheme.shapes.large,
            color = Palette.Paper,
            border = BorderStroke(1.dp, Palette.Line),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RuleRow(Icons.Filled.Star, stringResource(R.string.activity_rule_points))
                RuleRow(Icons.Filled.Check, stringResource(R.string.activity_rule_minimum, rules.weekdayMinMinutes, rules.weekendMinMinutes))
                RuleRow(Icons.Outlined.Info, stringResource(R.string.activity_rule_rest, rules.restFrom, rules.restTo))
                RuleRow(Icons.Outlined.Info, stringResource(R.string.activity_rule_pause_cap, rules.pauseCapMinutes / 60))
                RuleRow(Icons.Filled.Check, stringResource(R.string.activity_rule_daily_cap, rules.dailyCap))
            }
        }
    }
}

@Composable
private fun RuleRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Palette.Cream), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Palette.Green)
        }
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> stringResource(R.string.activity_minutes, rest)
        rest == 0 -> stringResource(R.string.activity_hours, hours)
        else -> stringResource(R.string.activity_hours_minutes, hours, rest)
    }
}

@Composable
private fun dayTitle(day: LocalDate, today: LocalDate): String = when (day) {
    today -> stringResource(R.string.activity_today)
    today.minusDays(1) -> stringResource(R.string.activity_yesterday)
    else -> day.format(DayFormat)
}

// Semana: inicial del día. Mes: solo algunos números, para que entren
private fun dayLabel(day: LocalDate, period: ActivityPeriod): String = when (period) {
    ActivityPeriod.WEEK -> WeekInitials[day.dayOfWeek.value - 1]
    ActivityPeriod.MONTH -> if ((day.dayOfMonth - 1) % 7 == 0) day.dayOfMonth.toString() else ""
}

private val ChartHeight = 160.dp
private val EmptyBarHeight = 8.dp
private val Mint = Palette.Green.copy(alpha = 0.25f)
private val WeekInitials = listOf("L", "M", "M", "J", "V", "S", "D")
private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d/M", Locale.forLanguageTag("es"))
