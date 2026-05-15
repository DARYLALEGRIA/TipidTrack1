package com.example.tipidtrack.repository

import com.example.tipidtrack.model.ExpenseItem
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getExpenses(userId: String): Flow<List<ExpenseItem>>
    fun getAllExpenses(): Flow<List<ExpenseItem>>
    suspend fun saveExpense(expense: ExpenseItem)
    suspend fun deleteExpense(expenseId: String)
}
