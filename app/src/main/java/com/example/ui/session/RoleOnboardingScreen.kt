package com.example.ui.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.CoPresent
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.UserRole

@Composable
fun RoleOnboardingScreen(
  onboardingState: UserSessionUiState.NeedsOnboarding,
  actionState: SessionActionState,
  onCompleteRegistration: (displayName: String, institution: String, role: UserRole) -> Unit,
  onSignOutClick: () -> Unit,
) {
  var displayName by rememberSaveable(onboardingState.uid) {
    mutableStateOf(onboardingState.defaultName)
  }
  var institution by rememberSaveable(onboardingState.uid) { mutableStateOf("") }
  var selectedRole by rememberSaveable(onboardingState.uid) { mutableStateOf<UserRole?>(null) }

  val trimmedName = displayName.trim()
  val isNameValid = trimmedName.length >= 2

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .widthIn(max = 600.dp)
            .padding(horizontal = 24.dp, vertical = 24.dp),
      ) {
        Text(
          text = stringResource(R.string.onboarding_title),
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = stringResource(R.string.onboarding_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Verified Google Identity Header Card
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            UserAvatarBadge(
              name = trimmedName.ifBlank { onboardingState.email },
              photoUrl = onboardingState.photoUrl,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = onboardingState.email,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Outlined.VerifiedUser,
                  contentDescription = null,
                  tint =
                    if (onboardingState.isEmailVerified) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.tertiary,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text =
                    if (onboardingState.isEmailVerified) {
                      stringResource(R.string.email_verified_badge)
                    } else {
                      stringResource(R.string.email_unverified_badge)
                    },
                  style = MaterialTheme.typography.labelMedium,
                  color =
                    if (onboardingState.isEmailVerified) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.tertiary,
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
          value = displayName,
          onValueChange = { displayName = it.take(80) },
          label = { Text(stringResource(R.string.full_name_label)) },
          placeholder = { Text(stringResource(R.string.full_name_placeholder)) },
          singleLine = true,
          isError = displayName.isNotEmpty() && !isNameValid,
          supportingText =
            if (displayName.isNotEmpty() && !isNameValid) {
              { Text(stringResource(R.string.full_name_error)) }
            } else {
              null
            },
          modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input"),
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = institution,
          onValueChange = { institution = it.take(100) },
          label = { Text(stringResource(R.string.institution_label)) },
          placeholder = { Text(stringResource(R.string.institution_placeholder)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("onboarding_institution_input"),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
          text = stringResource(R.string.choose_role_heading),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(12.dp))

        RoleSelectionCard(
          title = stringResource(R.string.role_teacher_title),
          description = stringResource(R.string.role_teacher_desc),
          icon = Icons.Outlined.CoPresent,
          selected = selectedRole == UserRole.TEACHER,
          onClick = { selectedRole = UserRole.TEACHER },
          testTag = "role_card_teacher",
        )

        Spacer(modifier = Modifier.height(12.dp))

        RoleSelectionCard(
          title = stringResource(R.string.role_student_title),
          description = stringResource(R.string.role_student_desc),
          icon = Icons.Filled.School,
          selected = selectedRole == UserRole.STUDENT,
          onClick = { selectedRole = UserRole.STUDENT },
          testTag = "role_card_student",
        )

        Spacer(modifier = Modifier.height(20.dp))

        AnimatedVisibility(visible = !actionState.errorMessage.isNullOrBlank()) {
          Card(
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
              ),
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("onboarding_error_banner"),
          ) {
            Text(
              text = actionState.errorMessage.orEmpty(),
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(14.dp),
            )
          }
        }

        Button(
          onClick = {
            val role = selectedRole ?: return@Button
            onCompleteRegistration(trimmedName, institution, role)
          },
          enabled = isNameValid && selectedRole != null && !actionState.isSubmitting,
          shape = RoundedCornerShape(16.dp),
          modifier =
            Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("complete_registration_button"),
        ) {
          if (actionState.isSubmitting) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.5.dp,
              color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(stringResource(R.string.registering_account))
          } else {
            Text(stringResource(R.string.complete_registration))
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
          onClick = onSignOutClick,
          enabled = !actionState.isSubmitting,
          modifier =
            Modifier.align(Alignment.CenterHorizontally)
              .heightIn(min = 48.dp)
              .testTag("onboarding_sign_out_button"),
        ) {
          Text(stringResource(R.string.switch_account))
        }
      }
    }
  }
}

@Composable
private fun RoleSelectionCard(
  title: String,
  description: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  val borderColor =
    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
  val containerColor =
    if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    else MaterialTheme.colorScheme.surface

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = containerColor,
    border = BorderStroke(width = if (selected) 2.dp else 1.dp, color = borderColor),
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = 84.dp)
        .clip(RoundedCornerShape(18.dp))
        .clickable(role = Role.RadioButton, onClick = onClick)
        .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Box(
        modifier =
          Modifier.size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
              if (selected) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint =
            if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(24.dp),
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Icon(
        imageVector =
          if (selected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
        contentDescription = title,
        tint =
          if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        modifier = Modifier.size(24.dp),
      )
    }
  }
}

@Composable
fun UserAvatarBadge(
  name: String,
  photoUrl: String?,
  modifier: Modifier = Modifier,
) {
  val initials =
    name
      .trim()
      .split(Regex("\\s+"))
      .filter { it.isNotEmpty() }
      .take(2)
      .joinToString("") { it.first().uppercase() }
      .ifBlank { "SC" }

  if (!photoUrl.isNullOrBlank()) {
    AsyncImage(
      model = photoUrl,
      contentDescription = name,
      contentScale = ContentScale.Crop,
      modifier = modifier.size(52.dp).clip(CircleShape),
    )
  } else {
    Box(
      modifier =
        modifier.size(52.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = initials,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    }
  }
}
