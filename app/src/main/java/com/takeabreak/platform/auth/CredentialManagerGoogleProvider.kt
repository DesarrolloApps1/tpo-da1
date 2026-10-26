package com.takeabreak.platform.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.takeabreak.domain.model.Account
import com.takeabreak.domain.repository.GoogleIdTokenProvider
import com.takeabreak.domain.repository.GoogleIdTokenResult

// Sign in with Google con Credential Manager. Necesita el contexto de la Activity para mostrar la hoja de cuentas
class CredentialManagerGoogleProvider(
    private val activityContext: Context,
    private val webClientId: String,
) : GoogleIdTokenProvider {

    override suspend fun requestIdToken(): GoogleIdTokenResult {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()
        return try {
            val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                val account = google.email?.let { Account(email = it, name = google.displayName, photoUrl = google.profilePictureUri?.toString()) }
                GoogleIdTokenResult.Token(google.idToken, account)
            } else {
                GoogleIdTokenResult.Failed
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleIdTokenResult.Cancelled
        } catch (_: GetCredentialException) {
            GoogleIdTokenResult.Failed
        } catch (_: GoogleIdTokenParsingException) {
            GoogleIdTokenResult.Failed
        }
    }
}
