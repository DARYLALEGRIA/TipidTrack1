package com.example.tipidtrack.viewmodel

import android.util.Log
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
    private val notificationRepository: NotificationRepository,
    private val reportRepository: ReportRepository
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
            userRepository.getCurrentUser()
                .catch { e -> Log.e("MainViewModel", "Error observing current user", e) }
                .collect { user ->
                    currentUser = user
                    if (user != null) {
                        observeUserData(user.id, user.role)
                        if (selectedCycleRange == null && !user.cycleStartDate.isNullOrEmpty()) {
                            selectedCycleRange = CycleManager.getCycleRange(user.cycleStartDate)
                        }
                    }
                }
        }
    }

    private fun observeAllUsers() {
        viewModelScope.launch {
            userRepository.getAllUsers()
                .catch { e -> Log.e("MainViewModel", "Error observing all users", e) }
                .collect { users ->
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
            expensesFlow
                .catch { e -> Log.e("MainViewModel", "Error observing expenses", e) }
                .collect { list ->
                    allExpenses.clear()
                    allExpenses.addAll(list)
                    autoSaveReport()
                }
        }

        viewModelScope.launch {
            val budgetsFlow = if (role == UserRole.STAFF || role == UserRole.ADMIN) {
                budgetRepository.getAllBudgets()
            } else {
                budgetRepository.getBudgets(userId)
            }
            budgetsFlow
                .catch { e -> Log.e("MainViewModel", "Error observing budgets", e) }
                .collect { list ->
                    allBudgets.clear()
                    allBudgets.addAll(list)
                    autoSaveReport()
                }
        }

        viewModelScope.launch {
            goalRepository.getGoals(userId)
                .catch { e -> Log.e("MainViewModel", "Error observing goals", e) }
                .collect { list ->
                    allGoals.clear()
                    allGoals.addAll(list)
                }
        }

        viewModelScope.launch {
            notificationRepository.getNotifications(userId)
                .catch { e -> Log.e("MainViewModel", "Error observing notifications", e) }
                .collect { list ->
                    // Check if anything actually changed to avoid unnecessary triggers in UI
                    if (notifications.size != list.size || notifications != list) {
                        notifications.clear()
                        notifications.addAll(list)
                    }
                }
        }
    }

    fun updateProfileImage(uri: String) {
        val user = currentUser ?: return
        viewModelScope.launch {
            try {
                userRepository.saveUser(user.copy(profileImageUri = uri))
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error updating profile image", e)
            }
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
        val expenseAmount = parseAmount(amount)
        viewModelScope.launch {
            try {
                expenseRepository.saveExpense(newItem)
                // Pass predicted values to avoid race conditions with Firestore listeners
                checkBudgetsAndNotify(category, expenseAmount, newItem.id)
                checkGoalsAndNotify(overrideBalance = balance - expenseAmount)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error adding expense", e)
            }
        }
    }

    fun deleteExpense(id: String) {
        val expense = allExpenses.find { it.id == id }
        val amount = expense?.amount?.let { parseAmount(it) } ?: 0.0
        viewModelScope.launch {
            try {
                expenseRepository.deleteExpense(id)
                // After deleting, balance increases, check if goals are now met
                checkGoalsAndNotify(overrideBalance = balance + amount)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting expense", e)
            }
        }
    }

    fun addBudget(budget: BudgetItem) {
        val user = currentUser ?: return
        val currentDate = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
        viewModelScope.launch {
            try {
                budgetRepository.saveBudget(budget.copy(date = currentDate, userId = user.id))
                // When adding a budget, check if current spending already exceeds it
                checkBudgetsAndNotify(budget.category ?: "", 0.0)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error adding budget", e)
            }
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            try {
                budgetRepository.deleteBudget(id)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting budget", e)
            }
        }
    }

    fun addGoal(goal: Goal) {
        val user = currentUser ?: return
        val currentDate = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
        viewModelScope.launch {
            try {
                goalRepository.saveGoal(goal.copy(userId = user.id, createdAt = currentDate))
                checkGoalsAndNotify()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error adding goal", e)
            }
        }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch {
            try {
                goalRepository.deleteGoal(id)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting goal", e)
            }
        }
    }

    fun addAllowance(amount: Double) {
        val user = currentUser ?: return
        val newAllowance = user.totalAllowance + amount
        viewModelScope.launch {
            try {
                userRepository.saveUser(user.copy(totalAllowance = newAllowance))
                // Predict new balance for goal check
                checkGoalsAndNotify(overrideBalance = balance + amount)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error adding allowance", e)
            }
        }
    }

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            try {
                notificationRepository.markAsRead(id)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error marking notification as read", e)
            }
        }
    }

    fun clearNotifications() {
        val user = currentUser ?: return
        viewModelScope.launch {
            try {
                notificationRepository.clearAll(user.id)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error clearing notifications", e)
            }
        }
    }

    private fun autoSaveReport() {
        val user = currentUser ?: return
        if (user.role != UserRole.STUDENT) return
        
        val cycle = selectedCycleRange ?: return
        val cycleStr = CycleManager.formatCycle(cycle)
        
        val expensesInCycle = filteredExpenses
        if (expensesInCycle.isEmpty()) return

        val categoryData = expensesInCycle.groupBy { (it.category ?: "").uppercase() }
            .mapValues { entry -> entry.value.sumOf { parseAmount(it.amount ?: "") } }
        
        val totalSpentValue = categoryData.values.sum()
        
        // Use a deterministic ID based on user and cycle to overwrite the same report
        val reportId = "${user.id}_${cycleStr.replace("/", "-").replace(" ", "_")}"
        
        val report = ReportItem(
            id = reportId,
            userId = user.id,
            cycleRange = cycleStr,
            totalSpent = totalSpentValue,
            categoryBreakdown = categoryData,
            generatedAt = System.currentTimeMillis(),
            notes = "Automatically updated"
        )

        viewModelScope.launch {
            try {
                reportRepository.saveReport(report)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error auto-saving report", e)
            }
        }
    }

    private suspend fun checkBudgetsAndNotify(category: String, addedAmount: Double, excludeExpenseId: String? = null) {
        val user = currentUser ?: return
        
        // Use filteredBudgets to ensure we check against the budget for the current cycle
        val budgetItem = filteredBudgets.find { 
            it.category?.trim().equals(category.trim(), ignoreCase = true) == true 
        } ?: return
        
        val budgetLimit = parseAmount(budgetItem.budget ?: "0")
        
        // Calculate current spent in cycle, excluding the one we just added if it's already in the list
        val currentSpentInCycle = filteredExpenses
            .filter { 
                it.category?.trim().equals(category.trim(), ignoreCase = true) == true && 
                it.id != excludeExpenseId 
            }
            .sumOf { parseAmount(it.amount ?: "0") }
        
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
            val recentlyNotified = notifications.take(10).any { 
                it.type == newNotif.type && 
                it.category == newNotif.category && 
                it.message == newNotif.message &&
                (System.currentTimeMillis() - (it.timestamp ?: 0)) < 10000 
            }
            if (!recentlyNotified) {
                try {
                    notificationRepository.saveNotification(newNotif)
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error saving notification", e)
                }
            }
        }
    }

    private suspend fun checkGoalsAndNotify(overrideBalance: Double? = null) {
        val user = currentUser ?: return
        val currentBalance = overrideBalance ?: balance
        allGoals.forEach { goal ->
            val target = goal.targetAmount ?: 0.0
            if (currentBalance >= target && target > 0) {
                // Check if already notified for this goal recently or if unread exists
                val alreadyNotified = notifications.any { 
                    it.type == NotificationType.SAVINGS && 
                    it.title?.contains(goal.title ?: "") == true &&
                    (it.isRead == false || (System.currentTimeMillis() - (it.timestamp ?: 0)) < 86400000)
                }
                if (!alreadyNotified) {
                    try {
                        notificationRepository.saveNotification(NotificationItem(
                            title = "Goal Reached! 🥳",
                            message = "Congratulations! You have successfully reached your goal: ${goal.title}",
                            category = "Goal",
                            type = NotificationType.SAVINGS,
                            userId = user.id
                        ))
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Error saving goal notification", e)
                    }
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
