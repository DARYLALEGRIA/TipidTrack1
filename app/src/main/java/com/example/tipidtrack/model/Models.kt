package com.example.tipidtrack.model

import java.util.UUID

enum class UserRole {
    STUDENT, STAFF, ADMIN
}

data class User(
    val name: String? = "",
    val email: String? = "",
    val phone: String? = "",
    val password: String? = "",
    val mpin: String? = "",
    val profileImageUri: String? = null,
    val cycleStartDate: String? = null,
    val role: UserRole = UserRole.STUDENT,
    val totalAllowance: Double = 0.0,
    val id: String = "" 
)

data class Goal(
    val id: String = UUID.randomUUID().toString(),
    val title: String? = "",
    val subtitle: String? = "",
    val targetAmount: Double? = 0.0,
    val currentAmount: Double? = 0.0,
    val icon: String? = "🎯",
    val targetDate: String? = null,
    val createdAt: String? = "",
    val userId: String? = null
)

data class ExpenseItem(
    val id: String = UUID.randomUUID().toString(),
    val date: String? = null,
    val category: String? = "",
    val amount: String? = "₱0.0",
    val notes: String? = "",
    val userId: String? = null
)

data class BudgetItem(
    val id: String = UUID.randomUUID().toString(),
    val category: String? = "",
    val budget: String? = "₱0.0",
    val spent: String? = "₱0.0",
    val date: String? = "",
    val userId: String? = null
)

enum class NotificationType {
    OVERSPENDING, WARNING, BUDGET_REACHED, SAVINGS, GENERAL
}

data class NotificationItem(
    val id: String? = UUID.randomUUID().toString(),
    val title: String? = "Notification",
    val message: String? = "",
    val category: String? = "General",
    val type: NotificationType? = NotificationType.GENERAL,
    val timestamp: Long? = System.currentTimeMillis(),
    val isRead: Boolean? = false,
    val userId: String? = null
)

data class ReportItem(
    val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val cycleRange: String? = "",
    val totalSpent: Double = 0.0,
    val categoryBreakdown: Map<String, Double> = emptyMap(),
    val generatedAt: Long = System.currentTimeMillis(),
    val notes: String? = ""
)
