package com.example.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.auth.AuthScreenContent
import com.example.ui.auth.AuthUiState
import com.example.ui.session.RoleOnboardingScreen
import com.example.ui.session.SessionActionState
import com.example.ui.session.StudentSessionScreen
import com.example.ui.session.TeacherSessionScreen
import com.example.ui.session.UserSessionUiState
import com.example.ui.theme.SmartClassroomTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Module1AuthUiTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun authScreen_rendersGoogleSignInAndHandlesTap() {
    var clicked = false
    composeTestRule.setContent {
      SmartClassroomTheme {
        AuthScreenContent(
          uiState = AuthUiState(),
          onSignInClick = { clicked = true },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed().performClick()
    assertTrue(clicked)
  }

  @Test
  fun onboardingScreen_requiresNameAndRoleBeforeSubmission() {
    var submittedName = ""
    var submittedInstitution = ""
    var submittedRole: UserRole? = null

    composeTestRule.setContent {
      SmartClassroomTheme {
        RoleOnboardingScreen(
          onboardingState =
            UserSessionUiState.NeedsOnboarding(
              uid = "uid_123",
              email = "ada@school.edu",
              defaultName = "",
              photoUrl = null,
              isEmailVerified = true,
            ),
          actionState = SessionActionState(),
          onCompleteRegistration = { name, inst, role ->
            submittedName = name
            submittedInstitution = inst
            submittedRole = role
          },
          onSignOutClick = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("complete_registration_button").performScrollTo().assertIsNotEnabled()

    composeTestRule.onNodeWithTag("onboarding_name_input").performScrollTo().performTextInput("Ada Lovelace")
    composeTestRule.onNodeWithTag("onboarding_institution_input").performScrollTo().performTextInput("CS Dept")
    composeTestRule.onNodeWithTag("complete_registration_button").performScrollTo().assertIsNotEnabled()

    composeTestRule.onNodeWithTag("role_card_teacher").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("complete_registration_button").performScrollTo().assertIsEnabled().performClick()

    assertEquals("Ada Lovelace", submittedName)
    assertEquals("CS Dept", submittedInstitution)
    assertEquals(UserRole.TEACHER, submittedRole)
  }

  @Test
  fun teacherAndStudentSessionScreens_renderIsolatedRoleWorkspaces() {
    val teacherProfile =
      UserProfile(
        id = "t1",
        userId = "t1",
        displayName = "Dr. Alan Turing",
        email = "alan@school.edu",
        role = UserRole.TEACHER.value,
        emailVerified = true,
        institution = "Mathematics",
      )

    composeTestRule.setContent {
      SmartClassroomTheme {
        TeacherSessionScreen(
          profile = teacherProfile,
          isEmailVerified = true,
          actionState = SessionActionState(),
          onUpdateProfile = { _, _, _ -> },
          onSendVerificationEmail = {},
          onRefreshVerificationStatus = {},
          onSignOutClick = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Teacher Workspace").assertIsDisplayed()
    composeTestRule.onNodeWithTag("role_badge_teacher").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sign_out_button").assertIsDisplayed()
  }

  @Test
  fun studentSessionScreen_rendersStudentRoleAndUnverifiedActions() {
    val studentProfile =
      UserProfile(
        id = "s1",
        userId = "s1",
        displayName = "Katherine Johnson",
        email = "katherine@school.edu",
        role = UserRole.STUDENT.value,
        emailVerified = false,
      )

    composeTestRule.setContent {
      SmartClassroomTheme {
        StudentSessionScreen(
          profile = studentProfile,
          isEmailVerified = false,
          actionState = SessionActionState(),
          onUpdateProfile = { _, _, _ -> },
          onSendVerificationEmail = {},
          onRefreshVerificationStatus = {},
          onSignOutClick = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Student Workspace").assertIsDisplayed()
    composeTestRule.onNodeWithTag("role_badge_student").assertIsDisplayed()
    composeTestRule.onNodeWithTag("send_verification_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("refresh_verification_button").assertIsDisplayed()
  }
}
