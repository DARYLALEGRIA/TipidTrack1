package com.example.tipidtrack.repository

import com.example.tipidtrack.model.ExpenseItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseExpenseRepository(
    private val db: FirebaseFirestore
) : ExpenseRepository {

    override fun getExpenses(userId: String): Flow<List<ExpenseItem>> = callbackFlow {
        val listener = db.collection("expenses")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val expenses = snapshot?.documents?.mapNotNull { it.toObject(ExpenseItem::class.java)?.copy(id = it.id) } ?: emptyList()
                trySend(expenses)
            }
        awaitClose { listener.remove() }
    }

    override fun getAllExpenses(): Flow<List<ExpenseItem>> = callbackFlow {
        val listener = db.collection("expenses")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val expenses = snapshot?.documents?.mapNotNull { it.toObject(ExpenseItem::class.java)?.copy(id = it.id) } ?: emptyList()
                trySend(expenses)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveExpense(expense: ExpenseItem) {
        db.collection("expenses").document(expense.id).set(expense).await()
    }

    override suspend fun deleteExpense(expenseId: String) {
        db.collection("expenses").document(expenseId).delete().await()
    }
}
