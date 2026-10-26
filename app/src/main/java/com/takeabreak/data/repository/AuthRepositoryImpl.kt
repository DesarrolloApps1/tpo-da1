package com.takeabreak.data.repository

import com.takeabreak.data.local.SessionStore
import com.takeabreak.data.remote.EmailAuthRequest
import com.takeabreak.data.remote.GoogleAuthRequest
import com.takeabreak.data.remote.SessionDto
import com.takeabreak.data.remote.TakeABreakApi
import com.takeabreak.domain.model.Account
import com.takeabreak.domain.model.AuthError
import com.takeabreak.domain.model.AuthResult
import com.takeabreak.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException

class AuthRepositoryImpl(
    private val api: TakeABreakApi,
    private val session: SessionStore,
) : AuthRepository {

    override val isSignedIn: Flow<Boolean> = session.token.map { it != null }

    override val account: Flow<Account?> = session.account

    override suspend fun signInWithGoogle(idToken: String, account: Account?): AuthResult =
        signIn(AuthError.GOOGLE_FAILED, account) { api.signInWithGoogle(GoogleAuthRequest(idToken)) }

    // Con email el backend no devuelve más datos: la cuenta es solo el email
    override suspend fun signInWithEmail(email: String, password: String, register: Boolean): AuthResult =
        signIn(AuthError.WRONG_CREDENTIALS, Account(email)) { api.signInWithEmail(EmailAuthRequest(email, password, register)) }

    override suspend fun signOut() = session.clear()

    // Traduce los códigos HTTP del backend a errores del dominio
    private suspend fun signIn(onUnauthorized: AuthError, account: Account?, call: suspend () -> SessionDto): AuthResult = try {
        session.save(call().token, account)
        AuthResult.Success
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        AuthResult.Failure(
            when (e.code()) {
                401 -> onUnauthorized
                409 -> AuthError.EMAIL_TAKEN
                else -> AuthError.UNKNOWN
            },
        )
    } catch (_: IOException) {
        AuthResult.Failure(AuthError.NO_CONNECTION)
    } catch (_: Exception) {
        AuthResult.Failure(AuthError.UNKNOWN)
    }
}
