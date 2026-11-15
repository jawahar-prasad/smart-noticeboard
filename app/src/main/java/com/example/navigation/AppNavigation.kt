package com.example.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.authStateFlow
import com.example.auth.signOutWithCredentialClear
import com.example.data.repository.UserRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.session.RoleOnboardingScreen
import com.example.ui.session.StudentSessionScreen
import com.example.ui.session.TeacherSessionScreen
import com.example.ui.session.UserSessionUiState
import com.example.ui.session.UserSessionViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

object Routes {
  const val AUTH = "auth"
  const val SESSION_LOADING = "session_loading"
  const val ONBOARDING = "onboarding"
  const val TEACHER_HOME = "teacher_home"
  const val STUDENT_HOME = "student_home"
  const val SESSION_ERROR = "session_error"
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    AuthScreen()
  } else {
    val uid = user.uid
    key(uid) { AuthenticatedSessionHost(currentUserId = uid) }
  }
}

@Composable
private fun AuthenticatedSessionHost(currentUserId: String) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = CredentialManager.create(context)

  val sessionViewModel: UserSessionViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            UserSessionViewModel(
              repository = UserRepository(db),
              currentUserId = currentUserId,
            )
          }
        },
    )

  val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()
  val actionState by sessionViewModel.actionState.collectAsStateWithLifecycle()
  val navController = rememberNavController()

  val targetRoute =
    when (sessionState) {
      UserSessionUiState.Loading -> Routes.SESSION_LOADING
      is UserSessionUiState.NeedsOnboarding -> Routes.ONBOARDING
      is UserSessionUiState.AuthenticatedTeacher -> Routes.TEACHER_HOME
      is UserSessionUiState.AuthenticatedStudent -> Routes.STUDENT_HOME
      is UserSessionUiState.Error -> Routes.SESSION_ERROR
    }

  LaunchedEffect(targetRoute) {
    val currentRoute = navController.currentDestination?.route
    if (currentRoute != targetRoute) {
      navController.navigate(targetRoute) {
        popUpTo(navController.graph.startDestinationId) { inclusive = true }
        launchSingleTop = true
      }
    }
  }

  val performSignOut: () -> Unit = {
    scope.launch { signOutWithCredentialClear(credentialManager) }
  }

  NavHost(
    navController = navController,
    startDestination = Routes.SESSION_LOADING,
  ) {
    composable(Routes.SESSION_LOADING) { SessionLoadingScreen() }

    composable(Routes.ONBOARDING) {
      val state = sessionState as? UserSessionUiState.NeedsOnboarding
      if (state != null) {
        RoleOnboardingScreen(
          onboardingState = state,
          actionState = actionState,
          onCompleteRegistration = { name, institution, role ->
            sessionViewModel.completeRegistration(name, institution, role)
          },
          onSignOutClick = performSignOut,
        )
      } else {
        SessionLoadingScreen()
      }
    }

    composable(Routes.TEACHER_HOME) {
      // Role gate: strictly prevent non-teacher states from rendering Teacher UI
      val state = sessionState as? UserSessionUiState.AuthenticatedTeacher
      if (state != null) {
        TeacherSessionScreen(
          profile = state.profile,
          isEmailVerified = state.isEmailVerified,
          actionState = actionState,
          onUpdateProfile = { name, institution, onDone ->
            sessionViewModel.updateProfile(name, institution, onDone)
          },
          onSendVerificationEmail = { sessionViewModel.sendVerificationEmail() },
          onRefreshVerificationStatus = {
            sessionViewModel.refreshEmailVerificationStatus(state.profile)
          },
          onSignOutClick = performSignOut,
        )
      } else {
        SessionLoadingScreen()
      }
    }

    composable(Routes.STUDENT_HOME) {
      // Role gate: strictly prevent non-student states from rendering Student UI
      val state = sessionState as? UserSessionUiState.AuthenticatedStudent
      if (state != null) {
        StudentSessionScreen(
          profile = state.profile,
          isEmailVerified = state.isEmailVerified,
          actionState = actionState,
          onUpdateProfile = { name, institution, onDone ->
            sessionViewModel.updateProfile(name, institution, onDone)
          },
          onSendVerificationEmail = { sessionViewModel.sendVerificationEmail() },
          onRefreshVerificationStatus = {
            sessionViewModel.refreshEmailVerificationStatus(state.profile)
          },
          onSignOutClick = performSignOut,
        )
      } else {
        SessionLoadingScreen()
      }
    }

    composable(Routes.SESSION_ERROR) {
      val state = sessionState as? UserSessionUiState.Error
      SessionErrorScreen(
        message = state?.message ?: "Unable to verify user session.",
        onSignOutClick = performSignOut,
      )
    }
  }
}

@Composable
private fun SessionLoadingScreen() {
  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("session_loading_screen"),
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = stringResource(R.string.loading_session),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun SessionErrorScreen(
  message: String,
  onSignOutClick: () -> Unit,
) {
  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .padding(24.dp)
          .testTag("session_error_screen"),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Button(onClick = onSignOutClick, modifier = Modifier.testTag("error_sign_out_button")) {
        Text(stringResource(R.string.sign_out))
      }
      Spacer(modifier = Modifier.height(8.dp))
      TextButton(onClick = onSignOutClick) { Text(stringResource(R.string.switch_account)) }
    }
  }
}
