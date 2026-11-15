package com.example.data.model

enum class UserRole(val value: String, val label: String) {
  TEACHER("teacher", "Teacher"),
  STUDENT("student", "Student");

  companion object {
    fun fromValue(value: String?): UserRole? =
      entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
  }
}
