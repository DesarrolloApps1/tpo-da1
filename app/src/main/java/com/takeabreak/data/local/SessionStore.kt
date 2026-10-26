package com.takeabreak.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Token de sesión: en DataStore solo queda el texto cifrado
class SessionStore(private val store: DataStore<Preferences>, private val cipher: TokenCipher) {

    // Si no se puede descifrar (por ejemplo, un backup restaurado en otro teléfono) cuenta como sin sesión
    val token: Flow<String?> = store.data.map { prefs ->
        prefs[SESSION_TOKEN]?.let { runCatching { cipher.decrypt(it) }.getOrNull() }
    }

    suspend fun save(token: String) {
        val encrypted = cipher.encrypt(token)
        store.edit { it[SESSION_TOKEN] = encrypted }
    }

    suspend fun clear() {
        store.edit { it.remove(SESSION_TOKEN) }
    }

    private companion object {
        val SESSION_TOKEN = stringPreferencesKey("session_token")
    }
}
