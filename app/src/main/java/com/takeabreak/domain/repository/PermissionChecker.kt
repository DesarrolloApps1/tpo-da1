package com.takeabreak.domain.repository

import com.takeabreak.domain.model.AppPermission

// Consulta el estado actual de cada permiso; lo implementa la capa de plataforma
fun interface PermissionChecker {
    fun isGranted(permission: AppPermission): Boolean
}
