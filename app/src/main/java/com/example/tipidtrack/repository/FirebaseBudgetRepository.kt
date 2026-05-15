package com.example.tipidtrack.repository

import com.example.tipidtrack.model.BudgetItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseBudgetRepository(
    private val db: FirebaseFirestore
) : BudgetRepository {

    override fun getBudgets(userId: String): Flow<List<BudgetItem>> = callbackFlow {
        val listener = db.collection("budgets")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val budgets = snapshot?.documents?.mapNotNull { it.toObject(BudgetItem::class.java)?.copy(id = it.id) } ?: emptyList()
                trySend(budgets)
            }
        awaitClose { listener.remove() }
    }

    override fun getAllBudgets(): Flow<List<BudgetItem>> = callbackFlow {
        val listener = db.collection("budgets")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val budgets = snapshot?.documents?.mapNotNull { it.toObject(BudgetItem::class.java)?.copy(id = it.id) } ?: emptyList()
                trySend(budgets)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveBudget(budget: BudgetItem) {
        db.collection("budgets").document(budget.id).set(budget).await()
    }

    override suspend fun deleteBudget(budgetId: String) {
        db.collection("budgets").document(budgetId).delete().await()
    }
}
