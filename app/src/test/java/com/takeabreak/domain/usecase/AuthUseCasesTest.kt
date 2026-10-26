package com.takeabreak.domain.usecase

import app.cash.turbine.test
import com.takeabreak.domain.model.AuthError
import com.takeabreak.domain.model.AuthResult
import com.takeabreak.testing.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUseCasesTest {

    private val repository = FakeAuthRepository()

    @Test
    fun `email invalido falla sin ir a la red`() = runTest {
        val result = SignInWithEmail(repository)("no-es-un-email", "secreta123", register = true)

        assertEquals(AuthResult.Failure(AuthError.INVALID_EMAIL), result)
        assertTrue(repository.emailCalls.isEmpty())
    }

    @Test
    fun `contrasena corta falla sin ir a la red`() = runTest {
        val result = SignInWithEmail(repository)("ana@mail.com", "corta", register = false)

        assertEquals(AuthResult.Failure(AuthError.SHORT_PASSWORD), result)
        assertTrue(repository.emailCalls.isEmpty())
    }

    @Test
    fun `email valido se normaliza y se envia con el modo elegido`() = runTest {
        val result = SignInWithEmail(repository)("  Ana@Mail.com ", "secreta123", register = true)

        assertEquals(AuthResult.Success, result)
        assertEquals(listOf(Triple("ana@mail.com", "secreta123", true)), repository.emailCalls)
    }

    @Test
    fun `el error del repositorio llega tal cual`() = runTest {
        repository.result = AuthResult.Failure(AuthError.EMAIL_TAKEN)

        assertEquals(AuthResult.Failure(AuthError.EMAIL_TAKEN), SignInWithEmail(repository)("ana@mail.com", "secreta123", true))
    }

    @Test
    fun `google envia el id token al repositorio`() = runTest {
        val result = SignInWithGoogle(repository)("google-id-token")

        assertEquals(AuthResult.Success, result)
        assertEquals(listOf("google-id-token"), repository.googleTokens)
    }

    @Test
    fun `la sesion se observa al entrar y al salir`() = runTest {
        ObserveSession(repository)().test {
            assertEquals(false, awaitItem())
            SignInWithGoogle(repository)("google-id-token")
            assertEquals(true, awaitItem())
            SignOut(repository)()
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `la sesion no repite valores iguales`() = runTest {
        ObserveSession(repository)().test {
            assertEquals(false, awaitItem())
            repository.token.value = "uno"
            assertEquals(true, awaitItem())
            repository.token.value = "dos"
            expectNoEvents()
        }
    }
}
