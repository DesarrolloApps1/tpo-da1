package com.takeabreak.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.takeabreak.domain.model.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Token de sesión y datos de la cuenta: del token solo queda el texto cifrado
class SessionStore(private val store: DataStore<Preferences>, private val cipher: TokenCipher) {

    // Si no se puede descifrar (por ejemplo, un backup restaurado en otro teléfono) cuenta como sin sesión
    val token: Flow<String?> = store.data.map { prefs ->
        prefs[SESSION_TOKEN]?.let { runCatching { cipher.decrypt(it) }.getOrNull() }
    }

    val account: Flow<Account?> = store.data.map { prefs ->
        prefs[ACCOUNT_EMAIL]?.let { Account(email = it, name = prefs[ACCOUNT_NAME], photoUrl = prefs[ACCOUNT_PHOTO]) }
    }

    // Token y cuenta en la misma escritura, así Perfil nunca ve una sesión sin cuenta
    suspend fun save(token: String, account: Account? = null) {
        val encrypted = cipher.encrypt(token)
        store.edit { prefs ->
            prefs[SESSION_TOKEN] = encrypted
            prefs.remove(ACCOUNT_EMAIL)
            prefs.remove(ACCOUNT_NAME)
            prefs.remove(ACCOUNT_PHOTO)
            account?.let {
                prefs[ACCOUNT_EMAIL] = it.email
                it.name?.let { name -> prefs[ACCOUNT_NAME] = name }
                it.photoUrl?.let { url -> prefs[ACCOUNT_PHOTO] = url }
            }
        }
    }

    suspend fun clear() {
        store.edit { prefs ->
            prefs.remove(SESSION_TOKEN)
            prefs.remove(ACCOUNT_EMAIL)
            prefs.remove(ACCOUNT_NAME)
            prefs.remove(ACCOUNT_PHOTO)
        }
    }

    private companion object {
        val SESSION_TOKEN = stringPreferencesKey("session_token")
        val ACCOUNT_EMAIL = stringPreferencesKey("account_email")
        val ACCOUNT_NAME = stringPreferencesKey("account_name")
        val ACCOUNT_PHOTO = stringPreferencesKey("account_photo")
    }
}
