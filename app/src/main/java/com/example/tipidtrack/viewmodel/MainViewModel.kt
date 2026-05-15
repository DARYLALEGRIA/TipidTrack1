package com.example.tipidtrack.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tipidtrack.model.*
import com.example.tipidtrack.repository.*
import com.example.tipidtrack.ui.CycleManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(
    private val userRepository: UserRepository,
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    var currentUser by mutableStateOf<User?>(null)
        private set

    var allUsers = mutableStateListOf<User>()
        private set

    var allExpenses = mutableStateListOf<ExpenseItem>()
        private set

    var allBudgets = mutableStateListOf<BudgetItem>()
        private set

    var allGoals = mutableStateListOf<Goal>()
        private set

    var notifications = mutableStateListOf<NotificationItem>()
        private set

    var selectedCycleRange by mutableStateOf<CycleManager.CycleRange?>(null)

    init {
        observeCurrentUser()
        observeAllUsers()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { user ->
                currentUser = user
                if (user != null) {
                    observeUserData(user.id, user.role)
                    if (selectedCycleRange == null && user.cycleStartDate != null) {
                        selectedCycleRange = CycleManager.getCycleRange(user.cycleStartDate)
                    }
                }
            }
        }
    }

    private fun observeAllUsers() {
        viewModelScope.launch {
            userRepository.getAllUsers().collect { users ->
                allUsers.clear()
                allUsers.addAll(users)
            }
        }
    }

    private fun observeUserData(userId: String, role: UserRole) {
        viewModelScope.launch {
            val expensesFlow = if (role == UserRole.STAFF || role == UserRole.ADMIN) {
                expenseRepository.getAllExpenses()
            } else {
                expenseRepository.getExpenses(userId)
            }
            expensesFlow.collect { list ->
                allExpenses.clear()
                allExpenses.addAll(list)
            }
        }

        viewModelScope.launch {
            val budgetsFlow = if (role == UserRole.STAFF || role == UserRole.ADMIN) {
                budgetRepository.getAllBudgets()
            } else {
                budgetRepository.getBudgets(userId)
            }
            budgetsFlow.collect { list ->
                allBudgets.clear()
                allBudgets.addAll(list)
            }
        }

        viewModelScope.launch {
            goalRepository.getGoals(userId).collect { list ->
                allGoals.clear()
                allGoals.addAll(list)
            }
        }

        viewModelScope.launch {
            notificationRepository.getNotifications(userId).collect { list ->
                notifications.clear()
                notifications.addAll(list)
            }
        }
    }

    fun updateProfileImage(uri: String) {
        val user = currentUser ?: return
        viewModelScope.launch {
            userRepository.saveUser(user.copy(profileImageUri = uri))
        }
    }

    fun addExpense(amount: String, category: String, notes: String) {
        val user = currentUser ?: return
        val currentDate = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
        val newItem = ExpenseItem(
            date = currentDate,
            category = category,
            amount = "₱$amount",
            notes = notes,
            userId = user.id
        )
        viewModelScope.launch {
            expenseRepository.saveExpense(newItem)
            checkBudgetsAndNotify(category, parseAmount(amount))
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(id)
        }
    }

    fun addBudget(budget: BudgetItem) {
        val user = currentUser ?: return
        val currentDate = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
        viewModelScope.launch {
            budgetRepository.saveBudget(budget.copy(date = currentDate, userId = user.id))
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(id)
        }
    }

    fun addGoal(goal: Goal) {
        val user = currentUser ?: return
        val currentDate = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
        viewModelScope.launch {
            goalRepository.saveGoal(goal.copy(userId = user.id, createdAt = currentDate))
            checkGoalsAndNotify()
        }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch {
            goalRepository.deleteGoal(id)
        }
    }

    fun addAllowance(amount: Double) {
        val user = currentUser ?: return
        val newAllowance = user.totalAllowance + amount
        viewModelScope.launch {
            userRepository.saveUser(user.copy(totalAllowance = newAllowance))
            checkGoalsAndNotify()
        }
    }

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(id)
        }
    }

    fun clearNotifications() {
        val user = currentUser ?: return
        viewModelScope.launch {
            notificationRepository.clearAll(user.id)
        }
    }

    private suspend fun checkBudgetsAndNotify(category: String, addedAmount: Double) {
        val user = currentUser ?: return
        val budgetItem = allBudgets.find { 
            it.userId == user.id && it.category?.trim().equals(category.trim(), ignoreCase = true) == true 
        } ?: return
        
        val budgetLimit = parseAmount(budgetItem.budget ?: "0")
        val currentSpentInCycle = filteredExpenses
            .filter { it.category?.trim().equals(category.trim(), ignoreCase = true) == true }
            .sumOf { parseAmount(it.amount ?: "0") }
        
        val totalSpent = currentSpentInCycle // Already includes the newly added expense if the flow updated fast enough, 
        // but to be safe we might want to pass the addedAmount separately if we want immediate check.
        // Actually, since we call this *after* saveExpense, the flow might not have emitted yet.
        
        val checkAmount = currentSpentInCycle + addedAmount

        val newNotif = when {
            checkAmount > budgetLimit -> NotificationItem(
                title = "Overspending Alert",
                message = "You have exceeded your $category budget by ₱${String.format(Locale.US, "%,.2f", checkAmount - budgetLimit)}.",
                category = category,
                type = NotificationType.OVERSPENDING,
                userId = user.id
            )
            checkAmount == budgetLimit -> NotificationItem(
                title = "Budget Limit Reached",
                message = "You have fully used your $category budget.",
                category = category,
                type = NotificationType.BUDGET_REACHED,
                userId = user.id
            )
            checkAmount >= budgetLimit * 0.8 -> NotificationItem(
                title = "Warning",
                message = "You are close to reaching your $category budget. Spent: ₱${String.format(Locale.US, "%,.2f", checkAmount)} of ₱${String.format(Locale.US, "%,.2f", budgetLimit)}",
                category = category,
                type = NotificationType.WARNING,
                userId = user.id
            )
            else -> null
        }

        if (newNotif != null) {
            val recentlyNotified = notifications.take(5).any { 
                it.type == newNotif.type && it.category == newNotif.category && (System.currentTimeMillis() - (it.timestamp ?: 0)) < 10000 
            }
            if (!recentlyNotified) {
                notificationRepository.saveNotification(newNotif)
            }
        }
    }

    private suspend fun checkGoalsAndNotify() {
        val user = currentUser ?: return
        val currentBalance = balance
        allGoals.forEach { goal ->
            val target = goal.targetAmount ?: 0.0
            if (currentBalance >= target && target > 0) {
                val alreadyNotified = notifications.any { 
                    it.type == NotificationType.SAVINGS && it.title?.contains(goal.title ?: "") == true
                }
                if (!alreadyNotified) {
                    notificationRepository.saveNotification(NotificationItem(
                        title = "Goal Reached! 🥳",
                        message = "Congratulations! You have successfully reached your goal: ${goal.title}",
                        category = "Goal",
                        type = NotificationType.SAVINGS,
                        userId = user.id
                    ))
                }
            }
        }
    }

    // Calculated properties
    val filteredExpenses: List<ExpenseItem>
        get() {
            val userOwned = allExpenses.filter { it.userId == currentUser?.id }
            return selectedCycleRange?.let { range ->
                userOwned.filter { CycleManager.isDateInCycle(it.date ?: "", range) }
            } ?: userOwned
        }

    val filteredBudgets: List<BudgetItem>
        get() {
            val userOwned = allBudgets.filter { it.userId == currentUser?.id }
            return selectedCycleRange?.let { range ->
                userOwned.filter { CycleManager.isDateInCycle(it.date ?: "", range) }
            } ?: userOwned
        }

    val totalBudgetsInCycle: Double
        get() = filteredBudgets.sumOf { parseAmount(it.budget ?: "0") }

    val totalExpenses: Double
        get() = allExpenses.filter { it.userId == currentUser?.id }.sumOf { parseAmount(it.amount ?: "0") }

    val balance: Double
        get() = ((currentUser?.totalAllowance ?: 0.0) - totalExpenses).coerceAtLeast(-999999.0)

    val budgetsWithSpent: List<BudgetItem>
        get() = filteredBudgets.map { budget ->
            val spent = filteredExpenses
                .filter { it.category?.trim().equals(budget.category?.trim() ?: "", ignoreCase = true) == true }
                .sumOf { parseAmount(it.amount ?: "0") }
            budget.copy(spent = "₱$spent")
        }

    private fun parseAmount(amountStr: String): Double {
        return amountStr.replace("₱", "").replace(",", "").toDoubleOrNull() ?: 0.0
    }
}
