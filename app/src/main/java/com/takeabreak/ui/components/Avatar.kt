package com.takeabreak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.takeabreak.domain.model.Account
import com.takeabreak.ui.theme.Palette

// Inicial sobre rosa (frames 08 y 25). La foto de Google queda guardada, pero mostrarla pediría una librería de imágenes
@Composable
fun InitialAvatar(account: Account?, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(Palette.Pink), contentAlignment = Alignment.Center) {
        if (account == null) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = Palette.PinkInk)
        } else {
            Text(account.initial, color = Palette.PinkInk, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.4f).sp)
        }
    }
}
