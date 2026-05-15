package com.example.tipidtrack.repository

import com.example.tipidtrack.model.User
import com.google.firebase.auth.AuthResult

interface AuthRepository {
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(email: String, password: String): AuthResult
    fun signOut()
    fun isUserLoggedIn(): Boolean
    fun getCurrentUserId(): String?
}
