package com.takeabreak.domain.repository

import kotlinx.coroutines.flow.Flow

// Avisa si hay conexión; lo implementa la capa de plataforma
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
