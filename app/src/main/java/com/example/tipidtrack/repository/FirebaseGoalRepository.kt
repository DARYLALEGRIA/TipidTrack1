package com.example.tipidtrack.repository

import com.example.tipidtrack.model.Goal
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseGoalRepository(
    private val db: FirebaseFirestore
) : GoalRepository {

    override fun getGoals(userId: String): Flow<List<Goal>> = callbackFlow {
        val listener = db.collection("goals")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val goals = snapshot?.documents?.mapNotNull { it.toObject(Goal::class.java)?.copy(id = it.id) } ?: emptyList()
                trySend(goals)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveGoal(goal: Goal) {
        db.collection("goals").document(goal.id).set(goal).await()
    }

    override suspend fun deleteGoal(goalId: String) {
        db.collection("goals").document(goalId).delete().await()
    }
}
