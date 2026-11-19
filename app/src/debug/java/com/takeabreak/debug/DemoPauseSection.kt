package com.takeabreak.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Solo en debug: botones de la pausa simulada para la demo de la defensa. 200 min muestra el tope de 3 h
@Composable
fun DemoPauseSection(status: String, onSimulate: (minutes: Long, afternoon: Boolean, declared: Boolean) -> Unit) {
    var afternoon by rememberSaveable { mutableStateOf(false) }
    var declared by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Pausa simulada (minutos)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(30L, 60L, 90L, 200L).forEach { minutes ->
                OutlinedButton(onClick = { onSimulate(minutes, afternoon, declared) }) { Text("$minutes") }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = afternoon, onCheckedChange = { afternoon = it })
            Text("Desde las 14 h")
            Checkbox(checked = declared, onCheckedChange = { declared = it })
            Text("Con el tile")
        }
        if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.bodySmall)
    }
}
