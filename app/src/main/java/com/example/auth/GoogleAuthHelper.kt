package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "Auth"

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

suspend fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
): Boolean {
  if (Firebase.auth.currentUser != null) return true
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      Log.w(AUTH_TAG, "default_web_client_id not configured: ${e.message}")
      return false
    }

  val googleIdOption =
    GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

  return try {
    val result = credentialManager.getCredential(context, request)
    val credential = result.credential
    if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
      val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
      Firebase.auth.signInWithCredential(authCredential).await()
      true
    } else {
      false
    }
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    Log.d(AUTH_TAG, "Silent auto sign-in unavailable: ${e.message}")
    false
  }
}

sealed interface InteractiveSignInResult {
  data object Success : InteractiveSignInResult

  data object Cancelled : InteractiveSignInResult

  data class Error(val message: String) : InteractiveSignInResult
}

suspend fun signInWithGoogleInteractive(
  context: Context,
  credentialManager: CredentialManager,
): InteractiveSignInResult {
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      return InteractiveSignInResult.Error(
        "Google Sign-In configuration missing: default_web_client_id not found"
      )
    }

  val activity = context as? Activity
    ?: return InteractiveSignInResult.Error("An active Activity is required for Google Sign-In")

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  return try {
    val result = credentialManager.getCredential(activity, request)
    val credential = result.credential
    if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
      val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
      Firebase.auth.signInWithCredential(authCredential).await()
      InteractiveSignInResult.Success
    } else {
      InteractiveSignInResult.Error("Unexpected credential type received")
    }
  } catch (e: GetCredentialCancellationException) {
    Log.w(AUTH_TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
    InteractiveSignInResult.Cancelled
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    Log.e(AUTH_TAG, "Google Sign-In failed", e)
    InteractiveSignInResult.Error(e.localizedMessage ?: "Sign in failed. Please try again.")
  }
}

suspend fun signOutWithCredentialClear(credentialManager: CredentialManager) {
  withContext(NonCancellable) {
    try {
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    } catch (e: Exception) {
      Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
    } finally {
      Firebase.auth.signOut()
    }
  }
}
