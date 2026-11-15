package com.example.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.InteractiveSignInResult
import com.example.auth.attemptAutoSignIn
import com.example.auth.signInWithGoogleInteractive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val hasAttemptedAutoSignIn: Boolean = false,
)

class AuthViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(AuthUiState())
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

  fun tryAutoSignIn(context: Context) {
    if (_uiState.value.hasAttemptedAutoSignIn) return
    _uiState.update { it.copy(hasAttemptedAutoSignIn = true) }
    val credentialManager = CredentialManager.create(context)
    viewModelScope.launch { attemptAutoSignIn(context, credentialManager) }
  }

  fun onSignInClicked(context: Context) {
    if (_uiState.value.isLoading) return
    val credentialManager = CredentialManager.create(context)
    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
    viewModelScope.launch {
      when (val result = signInWithGoogleInteractive(context, credentialManager)) {
        InteractiveSignInResult.Success -> {
          _uiState.update { it.copy(isLoading = false, errorMessage = null) }
        }
        InteractiveSignInResult.Cancelled -> {
          _uiState.update { it.copy(isLoading = false) }
        }
        is InteractiveSignInResult.Error -> {
          _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
        }
      }
    }
  }

  fun clearError() {
    _uiState.update { it.copy(errorMessage = null) }
  }
}
