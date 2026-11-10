package com.takeabreak.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.takeabreak.domain.model.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Token de sesión y datos de la cuenta: del token solo queda el texto cifrado.
// El saldo y los días informados son de la cuenta, por eso se borran junto con la sesión
class SessionStore(private val store: DataStore<Preferences>, private val cipher: TokenCipher) {

    // Si no se puede descifrar (por ejemplo, un backup restaurado en otro teléfono) cuenta como sin sesión
    val token: Flow<String?> = store.data.map { prefs ->
        prefs[SESSION_TOKEN]?.let { runCatching { cipher.decrypt(it) }.getOrNull() }
    }

    val account: Flow<Account?> = store.data.map { prefs ->
        prefs[ACCOUNT_EMAIL]?.let { Account(email = it, name = prefs[ACCOUNT_NAME], photoUrl = prefs[ACCOUNT_PHOTO]) }
    }

    val lastKnownBalance: Flow<Int?> = store.data.map { it[LAST_KNOWN_BALANCE] }

    val reportedDays: Flow<Map<String, Int>> = store.data.map { prefs ->
        prefs[REPORTED_DAYS].orEmpty().associate { entry -> entry.substringBefore('=') to entry.substringAfter('=').toInt() }
    }

    // Se guarda después de que el servidor confirmó: si el proceso muere antes, se reenvía sin duplicar
    suspend fun savePointsReport(days: Map<String, Int>, balance: Int) {
        store.edit { prefs ->
            val known = prefs[REPORTED_DAYS].orEmpty().filterNot { it.substringBefore('=') in days }
            prefs[REPORTED_DAYS] = known.toSet() + days.map { (day, points) -> "$day=$points" }
            prefs[LAST_KNOWN_BALANCE] = balance
        }
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
            prefs.remove(LAST_KNOWN_BALANCE)
            prefs.remove(REPORTED_DAYS)
        }
    }

    private companion object {
        val SESSION_TOKEN = stringPreferencesKey("session_token")
        val ACCOUNT_EMAIL = stringPreferencesKey("account_email")
        val ACCOUNT_NAME = stringPreferencesKey("account_name")
        val ACCOUNT_PHOTO = stringPreferencesKey("account_photo")
        val LAST_KNOWN_BALANCE = intPreferencesKey("last_known_balance")
        val REPORTED_DAYS = stringSetPreferencesKey("reported_days")
    }
}
