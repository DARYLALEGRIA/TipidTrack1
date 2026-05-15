package com.example.tipidtrack.repository

import com.example.tipidtrack.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseUserRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : UserRepository {

    override fun getCurrentUser(): Flow<User?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)?.copy(id = uid)
                trySend(user)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveUser(user: User) {
        if (user.id.isNotEmpty()) {
            db.collection("users").document(user.id).set(user).await()
        }
    }

    override suspend fun getUser(userId: String): User? {
        return db.collection("users").document(userId).get().await().toObject(User::class.java)?.copy(id = userId)
    }

    override fun getAllUsers(): Flow<List<User>> = callbackFlow {
        val listener = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { it.toObject(User::class.java)?.copy(id = it.id) } ?: emptyList()
                trySend(users)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun deleteUser(userId: String) {
        db.collection("users").document(userId).delete().await()
    }
}
