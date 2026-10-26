package com.takeabreak.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SessionStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("settings.preferences_pb").also { it.delete() } }
    }

    // Cifrado de mentira: el Keystore no existe en la JVM
    private val cipher = object : TokenCipher {
        override fun encrypt(plain: String) = "enc:" + plain.reversed()
        override fun decrypt(encoded: String): String {
            require(encoded.startsWith("enc:"))
            return encoded.removePrefix("enc:").reversed()
        }
    }

    private val store by lazy { SessionStore(dataStore, cipher) }

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun `guarda solo el texto cifrado y devuelve el token descifrado`() = runBlocking {
        store.save("jwt-secreto")

        val raw = dataStore.data.first()[stringPreferencesKey("session_token")]
        assertFalse(raw!!.contains("jwt-secreto"))
        assertEquals("jwt-secreto", store.token.first())
    }

    @Test
    fun `cerrar sesion borra el token`() = runBlocking {
        store.save("jwt-secreto")

        store.clear()

        assertNull(store.token.first())
    }

    @Test
    fun `un token que no se puede descifrar cuenta como sin sesion`() = runBlocking {
        dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply { this[stringPreferencesKey("session_token")] = "basura" }
        }

        assertNull(store.token.first())
    }
}
