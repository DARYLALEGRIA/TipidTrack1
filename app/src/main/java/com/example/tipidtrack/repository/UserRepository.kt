package com.example.tipidtrack.repository

import com.example.tipidtrack.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getCurrentUser(): Flow<User?>
    suspend fun saveUser(user: User)
    suspend fun getUser(userId: String): User?
    fun getAllUsers(): Flow<List<User>>
    suspend fun deleteUser(userId: String)
}
