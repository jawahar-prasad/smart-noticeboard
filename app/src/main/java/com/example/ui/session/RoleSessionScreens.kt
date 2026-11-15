package com.example.ui.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CoPresent
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import java.text.DateFormat
import java.util.Date

@Composable
fun TeacherSessionScreen(
  profile: UserProfile,
  isEmailVerified: Boolean,
  actionState: SessionActionState,
  onUpdateProfile: (displayName: String, institution: String, onSuccess: () -> Unit) -> Unit,
  onSendVerificationEmail: () -> Unit,
  onRefreshVerificationStatus: () -> Unit,
  onSignOutClick: () -> Unit,
) {
  RoleWorkspaceScaffold(
    title = stringResource(R.string.teacher_workspace_title),
    role = UserRole.TEACHER,
    profile = profile,
    isEmailVerified = isEmailVerified,
    actionState = actionState,
    grantedCapabilities = stringResource(R.string.teacher_permissions_active),
    restrictedCapabilities = stringResource(R.string.teacher_permissions_restricted),
    onUpdateProfile = onUpdateProfile,
    onSendVerificationEmail = onSendVerificationEmail,
    onRefreshVerificationStatus = onRefreshVerificationStatus,
    onSignOutClick = onSignOutClick,
  )
}

@Composable
fun StudentSessionScreen(
  profile: UserProfile,
  isEmailVerified: Boolean,
  actionState: SessionActionState,
  onUpdateProfile: (displayName: String, institution: String, onSuccess: () -> Unit) -> Unit,
  onSendVerificationEmail: () -> Unit,
  onRefreshVerificationStatus: () -> Unit,
  onSignOutClick: () -> Unit,
) {
  RoleWorkspaceScaffold(
    title = stringResource(R.string.student_workspace_title),
    role = UserRole.STUDENT,
    profile = profile,
    isEmailVerified = isEmailVerified,
    actionState = actionState,
    grantedCapabilities = stringResource(R.string.student_permissions_active),
    restrictedCapabilities = stringResource(R.string.student_permissions_restricted),
    onUpdateProfile = onUpdateProfile,
    onSendVerificationEmail = onSendVerificationEmail,
    onRefreshVerificationStatus = onRefreshVerificationStatus,
    onSignOutClick = onSignOutClick,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleWorkspaceScaffold(
  title: String,
  role: UserRole,
  profile: UserProfile,
  isEmailVerified: Boolean,
  actionState: SessionActionState,
  grantedCapabilities: String,
  restrictedCapabilities: String,
  onUpdateProfile: (displayName: String, institution: String, onSuccess: () -> Unit) -> Unit,
  onSendVerificationEmail: () -> Unit,
  onRefreshVerificationStatus: () -> Unit,
  onSignOutClick: () -> Unit,
) {
  var showEditDialog by remember { mutableStateOf(false) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.width(10.dp))
            RoleBadge(role = role)
          }
        },
        actions = {
          IconButton(
            onClick = onSignOutClick,
            modifier = Modifier.size(48.dp).testTag("sign_out_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = stringResource(R.string.sign_out_cd),
            )
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
          ),
      )
    },
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
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        AnimatedVisibility(visible = !actionState.statusMessage.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.fillMaxWidth().testTag("session_status_banner"),
          ) {
            Text(
              text = actionState.statusMessage.orEmpty(),
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(14.dp),
            )
          }
        }

        AnimatedVisibility(visible = !actionState.errorMessage.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.fillMaxWidth().testTag("session_error_banner"),
          ) {
            Text(
              text = actionState.errorMessage.orEmpty(),
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(14.dp),
            )
          }
        }

        // Profile & Session Identity Card
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth().testTag("profile_summary_card"),
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              UserAvatarBadge(
                name = profile.displayName,
                photoUrl = profile.photoUrl.ifBlank { null },
              )
              Spacer(modifier = Modifier.width(14.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = profile.displayName,
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = profile.email,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (profile.institution.isNotBlank()) {
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = profile.institution,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
              IconButton(
                onClick = { showEditDialog = true },
                modifier = Modifier.size(48.dp).testTag("edit_profile_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Edit,
                  contentDescription = stringResource(R.string.edit_profile),
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              AssistChip(
                onClick = {},
                enabled = false,
                label = {
                  Text(
                    text =
                      if (isEmailVerified) stringResource(R.string.email_verified_badge)
                      else stringResource(R.string.email_unverified_badge)
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector =
                      if (isEmailVerified) Icons.Outlined.VerifiedUser
                      else Icons.Outlined.MarkEmailUnread,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                },
                colors =
                  AssistChipDefaults.assistChipColors(
                    disabledContainerColor =
                      if (isEmailVerified) MaterialTheme.colorScheme.secondaryContainer
                      else MaterialTheme.colorScheme.tertiaryContainer,
                    disabledLabelColor =
                      if (isEmailVerified) MaterialTheme.colorScheme.onSecondaryContainer
                      else MaterialTheme.colorScheme.onTertiaryContainer,
                    disabledLeadingIconContentColor =
                      if (isEmailVerified) MaterialTheme.colorScheme.onSecondaryContainer
                      else MaterialTheme.colorScheme.onTertiaryContainer,
                  ),
                modifier = Modifier.testTag("email_verification_chip"),
              )

              val memberSinceText =
                profile.createdAt?.toDate()?.let { date ->
                  DateFormat.getDateInstance(DateFormat.MEDIUM).format(date)
                } ?: DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date())
              Text(
                text = "Joined $memberSinceText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            if (!isEmailVerified) {
              Spacer(modifier = Modifier.height(12.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                OutlinedButton(
                  onClick = onSendVerificationEmail,
                  enabled = !actionState.isSubmitting,
                  modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("send_verification_button"),
                ) {
                  Text(stringResource(R.string.send_verification_email))
                }
                OutlinedButton(
                  onClick = onRefreshVerificationStatus,
                  enabled = !actionState.isSubmitting,
                  modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("refresh_verification_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(stringResource(R.string.refresh_verification_status))
                }
              }
            }
          }
        }

        // Role-Based Access Control Card
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth().testTag("role_permissions_card"),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = stringResource(R.string.role_permissions_heading),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }

            Row(verticalAlignment = Alignment.Top) {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp).padding(top = 2.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = grantedCapabilities,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }

            Row(verticalAlignment = Alignment.Top) {
              Icon(
                imageVector = Icons.Outlined.Block,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp).padding(top = 2.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = restrictedCapabilities,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        // Module 1 Verified Status Banner
        Surface(
          shape = RoundedCornerShape(18.dp),
          color =
            if (role == UserRole.TEACHER) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
          modifier = Modifier.fillMaxWidth().testTag("module_status_card"),
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector =
                if (role == UserRole.TEACHER) Icons.Outlined.CoPresent else Icons.Filled.School,
              contentDescription = null,
              tint =
                if (role == UserRole.TEACHER) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(
                text = stringResource(R.string.module_one_verified_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = stringResource(R.string.module_one_verified_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        OutlinedButton(
          onClick = onSignOutClick,
          shape = RoundedCornerShape(16.dp),
          modifier =
            Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("sign_out_primary_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.sign_out))
        }
      }
    }
  }

  if (showEditDialog) {
    EditProfileDialog(
      initialName = profile.displayName,
      initialInstitution = profile.institution,
      isSubmitting = actionState.isSubmitting,
      onDismiss = { showEditDialog = false },
      onSave = { newName, newInst ->
        onUpdateProfile(newName, newInst) { showEditDialog = false }
      },
    )
  }
}

@Composable
private fun RoleBadge(role: UserRole) {
  val containerColor =
    if (role == UserRole.TEACHER) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.secondaryContainer
  val contentColor =
    if (role == UserRole.TEACHER) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSecondaryContainer

  Surface(
    shape = RoundedCornerShape(999.dp),
    color = containerColor,
    contentColor = contentColor,
    modifier = Modifier.testTag("role_badge_${role.value}"),
  ) {
    Text(
      text = role.label.uppercase(),
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
private fun EditProfileDialog(
  initialName: String,
  initialInstitution: String,
  isSubmitting: Boolean,
  onDismiss: () -> Unit,
  onSave: (String, String) -> Unit,
) {
  var name by remember { mutableStateOf(initialName) }
  var institution by remember { mutableStateOf(initialInstitution) }
  val isNameValid = name.trim().length >= 2

  AlertDialog(
    onDismissRequest = { if (!isSubmitting) onDismiss() },
    title = { Text(stringResource(R.string.edit_profile)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it.take(80) },
          label = { Text(stringResource(R.string.full_name_label)) },
          isError = !isNameValid,
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("edit_name_input"),
        )
        OutlinedTextField(
          value = institution,
          onValueChange = { institution = it.take(100) },
          label = { Text(stringResource(R.string.institution_label)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("edit_institution_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(name.trim(), institution.trim()) },
        enabled = isNameValid && !isSubmitting,
        modifier = Modifier.testTag("save_profile_button"),
      ) {
        Text(stringResource(R.string.save_changes))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        enabled = !isSubmitting,
      ) {
        Text(stringResource(R.string.cancel))
      }
    },
  )
}
