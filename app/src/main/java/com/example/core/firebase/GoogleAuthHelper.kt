package com.example.core.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "GoogleAuthHelper"

object GoogleAuthHelper {

    suspend fun attemptAutoSignIn(
        context: Context,
        auth: FirebaseAuth = Firebase.auth
    ): Boolean {
        if (auth.currentUser != null) return true
        return try {
            val webClientId = context.getString(R.string.default_web_client_id)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val credentialManager = CredentialManager.create(context)
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
                auth.signInWithCredential(firebaseCred).await()
                true
            } else {
                false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: NoCredentialException) {
            Log.d(TAG, "No authorized credential cached for silent auto sign-in.")
            false
        } catch (e: Exception) {
            Log.w(TAG, "Silent auto sign-in skipped: ${e.localizedMessage}")
            false
        }
    }

    sealed interface SignInResult {
        data object Success : SignInResult
        data object Cancelled : SignInResult
        data class Error(val message: String) : SignInResult
    }

    suspend fun signInWithGoogleInteractive(
        context: Context,
        auth: FirebaseAuth = Firebase.auth
    ): SignInResult {
        return try {
            val webClientId = context.getString(R.string.default_web_client_id)
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()
            val credentialManager = CredentialManager.create(context)
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
                auth.signInWithCredential(firebaseCred).await()
                SignInResult.Success
            } else {
                SignInResult.Error("Unexpected credential type received.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google sign-in cancelled or Play Services rejected fingerprint/config", e)
            SignInResult.Cancelled
        } catch (e: GetCredentialException) {
            Log.e(TAG, "CredentialManager error during Google Sign-In", e)
            SignInResult.Error(e.localizedMessage ?: "Google sign-in failed.")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase sign-in with Google credential failed", e)
            SignInResult.Error(e.localizedMessage ?: "Authentication failed.")
        }
    }

    suspend fun signOut(
        context: Context,
        auth: FirebaseAuth = Firebase.auth
    ) {
        withContext(NonCancellable) {
            runCatching {
                CredentialManager.create(context).clearCredentialState(
                    ClearCredentialStateRequest()
                )
            }.onFailure { err ->
                Log.w(TAG, "Failed to clear CredentialManager state", err)
            }
            auth.signOut()
        }
    }
}
