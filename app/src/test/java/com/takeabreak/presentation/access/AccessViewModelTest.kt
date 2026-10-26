package com.takeabreak.presentation.access

import app.cash.turbine.test
import com.takeabreak.domain.model.AuthError
import com.takeabreak.domain.model.AuthResult
import com.takeabreak.domain.repository.GoogleIdTokenProvider
import com.takeabreak.domain.repository.GoogleIdTokenResult
import com.takeabreak.domain.usecase.SignInWithEmail
import com.takeabreak.domain.usecase.SignInWithGoogle
import com.takeabreak.testing.FakeAuthRepository
import com.takeabreak.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccessViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeAuthRepository()

    private fun viewModel(googleEnabled: Boolean = true) =
        AccessViewModel(SignInWithGoogle(repository), SignInWithEmail(repository), googleEnabled)

    private fun AccessViewModel.fillForm(email: String = "ana@mail.com", password: String = "secreta123") {
        showEmailForm()
        onEmailChange(email)
        onPasswordChange(password)
    }

    @Test
    fun `registro con email muestra carga y despues limpia la contrasena`() = runTest {
        val gate = CompletableDeferred<Unit>().also { repository.gate = it }
        val vm = viewModel().apply { fillForm() }

        vm.state.test {
            assertFalse(awaitItem().isLoading)
            vm.submitEmail(register = true)
            assertTrue(awaitItem().isLoading)
            gate.complete(Unit)
            val done = awaitItem()
            assertFalse(done.isLoading)
            assertNull(done.error)
            assertEquals("", done.password)
        }
        assertEquals(listOf(Triple("ana@mail.com", "secreta123", true)), repository.emailCalls)
    }

    @Test
    fun `email repetido muestra el error`() = runTest {
        repository.result = AuthResult.Failure(AuthError.EMAIL_TAKEN)
        val vm = viewModel().apply { fillForm() }

        vm.submitEmail(register = true)

        assertEquals(AuthError.EMAIL_TAKEN, vm.state.value.error)
        assertEquals("secreta123", vm.state.value.password)
    }

    @Test
    fun `contrasena incorrecta muestra el error`() = runTest {
        repository.result = AuthResult.Failure(AuthError.WRONG_CREDENTIALS)
        val vm = viewModel().apply { fillForm() }

        vm.submitEmail(register = false)

        assertEquals(AuthError.WRONG_CREDENTIALS, vm.state.value.error)
        assertEquals(false, repository.emailCalls.single().third)
    }

    @Test
    fun `email invalido muestra el error sin llamar al repositorio`() = runTest {
        val vm = viewModel().apply { fillForm(email = "ana") }

        vm.submitEmail(register = true)

        assertEquals(AuthError.INVALID_EMAIL, vm.state.value.error)
        assertTrue(repository.emailCalls.isEmpty())
    }

    @Test
    fun `escribir borra el error anterior`() = runTest {
        val vm = viewModel().apply { fillForm(email = "ana") }
        vm.submitEmail(register = true)

        vm.onEmailChange("ana@mail.com")

        assertNull(vm.state.value.error)
    }

    @Test
    fun `un segundo toque mientras carga se ignora`() = runTest {
        val gate = CompletableDeferred<Unit>().also { repository.gate = it }
        val vm = viewModel().apply { fillForm() }

        vm.submitEmail(register = true)
        vm.submitEmail(register = true)
        gate.complete(Unit)

        assertEquals(1, repository.emailCalls.size)
    }

    @Test
    fun `google con token entra con ese token`() = runTest {
        val vm = viewModel()

        vm.continueWithGoogle { GoogleIdTokenResult.Token("google-id-token") }

        assertEquals(listOf("google-id-token"), repository.googleTokens)
        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `google cancelado no muestra error`() = runTest {
        val vm = viewModel()

        vm.continueWithGoogle { GoogleIdTokenResult.Cancelled }

        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
        assertTrue(repository.googleTokens.isEmpty())
    }

    @Test
    fun `google fallido muestra el error`() = runTest {
        val vm = viewModel()

        vm.continueWithGoogle { GoogleIdTokenResult.Failed }

        assertEquals(AuthError.GOOGLE_FAILED, vm.state.value.error)
    }

    @Test
    fun `token rechazado por el backend muestra el error`() = runTest {
        repository.result = AuthResult.Failure(AuthError.GOOGLE_FAILED)
        val vm = viewModel()

        vm.continueWithGoogle { GoogleIdTokenResult.Token("vencido") }

        assertEquals(AuthError.GOOGLE_FAILED, vm.state.value.error)
    }

    @Test
    fun `sin client ID no se pide el token de Google`() = runTest {
        var asked = false
        val vm = viewModel(googleEnabled = false)

        vm.continueWithGoogle(GoogleIdTokenProvider { asked = true; GoogleIdTokenResult.Cancelled })

        assertFalse(vm.state.value.googleEnabled)
        assertFalse(asked)
    }

    @Test
    fun `volver del formulario lo oculta y borra el error`() = runTest {
        val vm = viewModel().apply { fillForm(email = "ana") }
        vm.submitEmail(register = true)

        vm.hideEmailForm()

        assertFalse(vm.state.value.showEmailForm)
        assertNull(vm.state.value.error)
    }
}
