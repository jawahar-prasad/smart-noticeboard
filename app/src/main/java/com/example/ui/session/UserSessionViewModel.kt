package com.example.ui.session

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.repository.UserRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface UserSessionUiState {
  data object Loading : UserSessionUiState

  data class NeedsOnboarding(
    val uid: String,
    val email: String,
    val defaultName: String,
    val photoUrl: String?,
    val isEmailVerified: Boolean,
  ) : UserSessionUiState

  data class AuthenticatedTeacher(
    val profile: UserProfile,
    val isEmailVerified: Boolean,
  ) : UserSessionUiState

  data class AuthenticatedStudent(
    val profile: UserProfile,
    val isEmailVerified: Boolean,
  ) : UserSessionUiState

  data class Error(val message: String) : UserSessionUiState
}

data class SessionActionState(
  val isSubmitting: Boolean = false,
  val statusMessage: String? = null,
  val errorMessage: String? = null,
)

class UserSessionViewModel(
  private val repository: UserRepository,
  private val currentUserId: String,
) : ViewModel() {

  private val _actionState = MutableStateFlow(SessionActionState())
  val actionState: StateFlow<SessionActionState> = _actionState.asStateFlow()

  private val _emailVerifiedOverride = MutableStateFlow<Boolean?>(null)

  val sessionState: StateFlow<UserSessionUiState> =
    repository
      .observeUserProfile(currentUserId)
      .map { profile ->
        val authUser = Firebase.auth.currentUser
        val verified = _emailVerifiedOverride.value ?: (authUser?.isEmailVerified ?: false)
        if (profile == null) {
          UserSessionUiState.NeedsOnboarding(
            uid = currentUserId,
            email = authUser?.email.orEmpty().ifBlank { "user@smartclassroom.edu" },
            defaultName = authUser?.displayName.orEmpty(),
            photoUrl = authUser?.photoUrl?.toString(),
            isEmailVerified = verified,
          )
        } else {
          when (profile.parsedRole()) {
            UserRole.TEACHER ->
              UserSessionUiState.AuthenticatedTeacher(
                profile = profile,
                isEmailVerified = verified || profile.emailVerified,
              )
            UserRole.STUDENT ->
              UserSessionUiState.AuthenticatedStudent(
                profile = profile,
                isEmailVerified = verified || profile.emailVerified,
              )
            null -> UserSessionUiState.Error("Unsupported role value in profile: ${profile.role}")
          }
        }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing user profile", rootError)
        emit(UserSessionUiState.Error(rootError.message ?: "Failed to load user profile"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UserSessionUiState.Loading,
      )

  fun completeRegistration(
    displayName: String,
    institution: String,
    role: UserRole,
  ) {
    val trimmedName = displayName.trim()
    if (trimmedName.length < 2) {
      _actionState.update { it.copy(errorMessage = "Please enter at least 2 characters for your name.") }
      return
    }
    val authUser = Firebase.auth.currentUser
    val email = authUser?.email?.takeIf { it.isNotBlank() } ?: "member@smartclassroom.edu"
    val verified = authUser?.isEmailVerified ?: false
    val photoUrl = authUser?.photoUrl?.toString()

    _actionState.update { it.copy(isSubmitting = true, errorMessage = null, statusMessage = null) }
    viewModelScope.launch {
      val result =
        repository.createUserProfile(
          displayName = trimmedName,
          email = email,
          role = role,
          emailVerified = verified,
          institution = institution,
          photoUrl = photoUrl,
        )
      result
        .onSuccess {
          _actionState.update { it.copy(isSubmitting = false, errorMessage = null) }
        }
        .onFailure { err ->
          _actionState.update {
            it.copy(
              isSubmitting = false,
              errorMessage = err.localizedMessage ?: "Failed to complete registration.",
            )
          }
        }
    }
  }

  fun updateProfile(displayName: String, institution: String, onSuccess: () -> Unit = {}) {
    val trimmedName = displayName.trim()
    if (trimmedName.length < 2) {
      _actionState.update { it.copy(errorMessage = "Display name must be at least 2 characters.") }
      return
    }
    val authUser = Firebase.auth.currentUser
    val verified = authUser?.isEmailVerified ?: false
    val photoUrl = authUser?.photoUrl?.toString()

    _actionState.update { it.copy(isSubmitting = true, errorMessage = null, statusMessage = null) }
    viewModelScope.launch {
      val result =
        repository.updateUserProfile(
          displayName = trimmedName,
          institution = institution,
          emailVerified = verified,
          photoUrl = photoUrl,
        )
      result
        .onSuccess {
          _actionState.update {
            it.copy(isSubmitting = false, statusMessage = "Profile updated.", errorMessage = null)
          }
          onSuccess()
        }
        .onFailure { err ->
          _actionState.update {
            it.copy(
              isSubmitting = false,
              errorMessage = err.localizedMessage ?: "Could not update profile.",
            )
          }
        }
    }
  }

  fun sendVerificationEmail() {
    val user = Firebase.auth.currentUser ?: return
    _actionState.update { it.copy(isSubmitting = true, errorMessage = null, statusMessage = null) }
    viewModelScope.launch {
      try {
        user.sendEmailVerification().await()
        _actionState.update {
          it.copy(
            isSubmitting = false,
            statusMessage = "Verification email sent to ${user.email ?: "your address"}.",
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _actionState.update {
          it.copy(
            isSubmitting = false,
            errorMessage = e.localizedMessage ?: "Unable to send verification email.",
          )
        }
      }
    }
  }

  fun refreshEmailVerificationStatus(profile: UserProfile) {
    val user = Firebase.auth.currentUser ?: return
    _actionState.update { it.copy(isSubmitting = true, errorMessage = null, statusMessage = null) }
    viewModelScope.launch {
      try {
        user.reload().await()
        val verified = user.isEmailVerified
        _emailVerifiedOverride.value = verified
        if (verified && !profile.emailVerified) {
          repository.updateUserProfile(
            displayName = profile.displayName,
            institution = profile.institution,
            emailVerified = true,
            photoUrl = profile.photoUrl.ifBlank { null },
          )
        }
        _actionState.update {
          it.copy(
            isSubmitting = false,
            statusMessage =
              if (verified) "Email verification confirmed." else "Email is still unverified.",
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _actionState.update {
          it.copy(
            isSubmitting = false,
            errorMessage = e.localizedMessage ?: "Failed to refresh verification status.",
          )
        }
      }
    }
  }

  fun clearMessages() {
    _actionState.update { it.copy(statusMessage = null, errorMessage = null) }
  }

  private companion object {
    const val TAG = "UserSessionVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
