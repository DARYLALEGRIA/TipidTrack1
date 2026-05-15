package com.example.tipidtrack.repository

import com.example.tipidtrack.model.BudgetItem
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgets(userId: String): Flow<List<BudgetItem>>
    fun getAllBudgets(): Flow<List<BudgetItem>>
    suspend fun saveBudget(budget: BudgetItem)
    suspend fun deleteBudget(budgetId: String)
}
