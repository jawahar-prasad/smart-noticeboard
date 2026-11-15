package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in before accessing Firestore.")

  fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    val path = "users/$userId"
    emitAll(
      db.collection(USERS_COLLECTION)
        .document(userId)
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              UserProfile::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  suspend fun getUserProfile(userId: String): Result<UserProfile?> {
    val path = "users/$userId"
    return try {
      val snapshot = db.collection(USERS_COLLECTION).document(userId).get().await()
      val profile =
        if (snapshot.exists()) {
          snapshot.toObject(
            UserProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        } else {
          null
        }
      Result.success(profile)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      Result.failure(e)
    }
  }

  suspend fun createUserProfile(
    displayName: String,
    email: String,
    role: UserRole,
    emailVerified: Boolean,
    institution: String = "",
    photoUrl: String? = null,
  ): Result<Unit> {
    val uid = requireUserId()
    val trimmedName = displayName.trim()
    val trimmedEmail = email.trim()
    val trimmedInstitution = institution.trim()
    require(trimmedName.length in 2..80) { "Display name must be between 2 and 80 characters." }
    require(trimmedEmail.length in 3..120) { "Valid email address is required." }

    val docRef = db.collection(USERS_COLLECTION).document(uid)
    val payload = buildMap<String, Any> {
      put("userId", uid)
      put("displayName", trimmedName)
      put("email", trimmedEmail)
      put("role", role.value)
      put("emailVerified", emailVerified)
      put("institution", trimmedInstitution.take(100))
      if (!photoUrl.isNullOrBlank()) {
        put("photoUrl", photoUrl.trim().take(500))
      }
      put("createdAt", FieldValue.serverTimestamp())
      put("updatedAt", FieldValue.serverTimestamp())
    }

    return try {
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateUserProfile(
    displayName: String,
    institution: String,
    emailVerified: Boolean,
    photoUrl: String? = null,
  ): Result<Unit> {
    val uid = requireUserId()
    val trimmedName = displayName.trim()
    require(trimmedName.length in 2..80) { "Display name must be between 2 and 80 characters." }

    val docRef = db.collection(USERS_COLLECTION).document(uid)
    val updates = buildMap<String, Any> {
      put("displayName", trimmedName)
      put("institution", institution.trim().take(100))
      put("emailVerified", emailVerified)
      if (!photoUrl.isNullOrBlank()) {
        put("photoUrl", photoUrl.trim().take(500))
      }
      put("updatedAt", FieldValue.serverTimestamp())
    }

    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteOwnProfile(): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection(USERS_COLLECTION).document(uid)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  private companion object {
    const val USERS_COLLECTION = "users"
  }
}
