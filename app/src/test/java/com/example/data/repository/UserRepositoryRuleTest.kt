package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserRole
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class UserRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createAndGetUserProfile_authenticatedTeacher_succeeds() = runBlocking {
    val uid = signInTestUser("teacher_create_${System.nanoTime()}@school.edu")
    val repo = UserRepository(firestore)

    val createResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.createUserProfile(
          displayName = "Prof. Ada Lovelace",
          email = "ada@school.edu",
          role = UserRole.TEACHER,
          emailVerified = true,
          institution = "Computer Science",
        )
      }
    assertTrue("Expected profile creation to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)

    val fetched = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getUserProfile(uid).getOrThrow() }
    assertNotNull(fetched)
    assertEquals(uid, fetched?.id)
    assertEquals(uid, fetched?.userId)
    assertEquals("Prof. Ada Lovelace", fetched?.displayName)
    assertEquals(UserRole.TEACHER, fetched?.parsedRole())
    assertEquals("Computer Science", fetched?.institution)
    assertNotNull(fetched?.createdAt)
  }

  @Test
  fun observeUserProfile_authenticatedStudent_emitsRealtimeProfileAndUpdates() = runBlocking {
    val uid = signInTestUser("student_observe_${System.nanoTime()}@school.edu")
    val repo = UserRepository(firestore)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createUserProfile(
        displayName = "Grace Hopper",
        email = "grace@school.edu",
        role = UserRole.STUDENT,
        emailVerified = true,
        institution = "Mathematics",
      ).getOrThrow()
    }

    val initial =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeUserProfile(uid).first { it != null }
      }
    assertEquals(UserRole.STUDENT, initial?.parsedRole())
    assertEquals("Grace Hopper", initial?.displayName)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateUserProfile(
        displayName = "Grace M. Hopper",
        institution = "Applied Math",
        emailVerified = true,
      ).getOrThrow()
    }

    val updated =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeUserProfile(uid).first { it?.displayName == "Grace M. Hopper" }
      }
    assertEquals("Applied Math", updated?.institution)
    assertEquals(UserRole.STUDENT, updated?.parsedRole())
  }

  @Test
  fun getUserProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser("alice_cross_${System.nanoTime()}@school.edu")
    val aliceRepo = UserRepository(firestore)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createUserProfile(
        displayName = "Alice Owner",
        email = "alice@school.edu",
        role = UserRole.STUDENT,
        emailVerified = true,
      ).getOrThrow()
    }

    signInTestUser("bob_cross_${System.nanoTime()}@school.edu")
    val bobRepo = UserRepository(firestore)
    val result = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getUserProfile(aliceUid) }
    assertTrue("Expected cross-user read to fail", result.isFailure)
    val firestoreException = result.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
  }

  @Test
  fun observeUserProfile_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = UserRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeUserProfile("some_user_123").first() }
      fail("Expected PERMISSION_DENIED for unauthenticated profile observation")
    } catch (e: Exception) {
      val firestoreException = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }
  }

  @Test
  fun updateRoleDirectMutation_blockedBySecurityRules() = runBlocking {
    val uid = signInTestUser("student_escalate_${System.nanoTime()}@school.edu")
    val repo = UserRepository(firestore)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createUserProfile(
        displayName = "Student User",
        email = "student@school.edu",
        role = UserRole.STUDENT,
        emailVerified = true,
      ).getOrThrow()
    }

    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(uid).update("role", "teacher").await()
      }
      fail("Expected role escalation to be rejected by Firestore rules")
    } catch (e: Exception) {
      val firestoreException = e.unwrapFirestoreException()
      assertNotNull(firestoreException)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }
  }

  @Test
  fun deleteOwnProfile_removesDocument() = runBlocking {
    val uid = signInTestUser("delete_user_${System.nanoTime()}@school.edu")
    val repo = UserRepository(firestore)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createUserProfile(
        displayName = "Delete Me",
        email = "delete@school.edu",
        role = UserRole.TEACHER,
        emailVerified = true,
      ).getOrThrow()
    }

    val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.deleteOwnProfile() }
    assertTrue(deleteResult.isSuccess)
    val afterDelete = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getUserProfile(uid).getOrThrow() }
    assertNull(afterDelete)
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
