package com.example.tipidtrack.ui

import java.text.SimpleDateFormat
import java.util.*

/**
 * Interface representing the contract for student tracking analytics.
 * Follows Interface Segregation Principle (ISP).
 */
interface IStudentTracker {
    fun getStudentExpenses(expenses: List<ExpenseItem>, users: List<User>): List<ExpenseItem>
    fun calculateTotalSpending(expenses: List<ExpenseItem>): Double
    fun calculateAverageSpending(totalSpending: Double, studentCount: Int): Double
    fun getCategoryDistribution(expenses: List<ExpenseItem>): List<Pair<String, Double>>
    fun getMonthlyTrends(expenses: List<ExpenseItem>): List<Pair<String, Double>>
    fun getAnonymousSpendingHabits(expenses: List<ExpenseItem>, users: List<User>): List<Pair<String, Double>>
}

/**
 * Implementation of student analytics logic.
 * Follows Single Responsibility Principle (SRP) by isolating logic from UI.
 */
class StaffAnalyticsProcessor : IStudentTracker {

    private fun parseAmount(amountStr: String?): Double {
        if (amountStr == null) return 0.0
        return amountStr.replace("₱", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
    }

    override fun getStudentExpenses(expenses: List<ExpenseItem>, users: List<User>): List<ExpenseItem> {
        val studentIds = users.filter { it.role == UserRole.STUDENT }.map { it.id }.toSet()
        return if (studentIds.isNotEmpty()) {
            expenses.filter { it.userId in studentIds }
        } else {
            expenses // Fallback if no user list provided (handled in UI)
        }
    }

    override fun calculateTotalSpending(expenses: List<ExpenseItem>): Double {
        return expenses.sumOf { parseAmount(it.amount) }
    }

    override fun calculateAverageSpending(totalSpending: Double, studentCount: Int): Double {
        return if (studentCount > 0) totalSpending / studentCount else 0.0
    }

    override fun getCategoryDistribution(expenses: List<ExpenseItem>): List<Pair<String, Double>> {
        return expenses.groupBy { it.category?.trim() ?: "Other" }
            .mapValues { entry -> entry.value.sumOf { parseAmount(it.amount) } }
            .toList()
            .sortedByDescending { it.second }
    }

    override fun getMonthlyTrends(expenses: List<ExpenseItem>): List<Pair<String, Double>> {
        val dateFormat = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        
        return expenses.groupBy { 
            try {
                val dateStr = it.date ?: ""
                if (dateStr.isEmpty()) "Unknown"
                else {
                    val date = dateFormat.parse(dateStr)
                    monthFormat.format(date!!)
                }
            } catch (e: Exception) {
                "Unknown"
            }
        }.mapValues { entry -> entry.value.sumOf { parseAmount(it.amount) } }
        .toList()
        .sortedByDescending { it.first }
    }

    override fun getAnonymousSpendingHabits(expenses: List<ExpenseItem>, users: List<User>): List<Pair<String, Double>> {
        val students = users.filter { it.role == UserRole.STUDENT }
        
        val spentList = if (students.isNotEmpty()) {
            students.map { s ->
                expenses.filter { it.userId == s.id }.sumOf { parseAmount(it.amount) }
            }
        } else {
            val activeIds = expenses.mapNotNull { it.userId }.distinct()
            activeIds.map { id ->
                expenses.filter { it.userId == id }.sumOf { parseAmount(it.amount) }
            }
        }

        return spentList.sortedByDescending { it }
            .mapIndexed { index, spent -> "Student ${index + 1}" to spent }
    }
}
