package com.takeabreak.domain.model

// Datos de la cuenta que se muestran en Perfil. Con email solo hay email; Google agrega nombre y foto
data class Account(
    val email: String,
    val name: String? = null,
    val photoUrl: String? = null,
) {
    // Nombre visible: el de Google o la parte del email antes de la arroba
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: email.substringBefore('@')

    val initial: String
        get() = displayName.firstOrNull()?.uppercase() ?: "?"
}
