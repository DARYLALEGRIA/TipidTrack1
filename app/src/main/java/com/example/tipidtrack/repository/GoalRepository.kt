package com.example.tipidtrack.repository

import com.example.tipidtrack.model.Goal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getGoals(userId: String): Flow<List<Goal>>
    suspend fun saveGoal(goal: Goal)
    suspend fun deleteGoal(goalId: String)
}
