package com.example.tipidtrack

import android.content.Context
import android.media.RingtoneManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.tipidtrack.ui.*
import com.example.tipidtrack.ui.theme.TipidTrackTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class Screen {
    LOGIN, REGISTER, MPIN, HOME, EXPENSES, BUDGETS, REPORTS, NOTIFICATIONS, STAFF_DASHBOARD, ADMIN_DASHBOARD
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TipidTrackTheme {
                val context = LocalContext.current
                val sharedPrefs = remember { context.getSharedPreferences("TipidTrackPrefs", Context.MODE_PRIVATE) }
                val gson = remember { Gson() }

                var currentScreen by remember { mutableStateOf(Screen.LOGIN) }
                
                // For this multi-role system, we'll maintain a list of all registered users in sharedPrefs
                val allUsers = remember { mutableStateListOf<User>() }
                
                var currentUser by remember { mutableStateOf<User?>(null) }

                val allExpenses = remember { mutableStateListOf<ExpenseItem>() }
                val allBudgets = remember { mutableStateListOf<BudgetItem>() }
                val goals = remember { mutableStateListOf<Goal>() }
                val notifications = remember { mutableStateListOf<NotificationItem>() }
                
                var totalAllowance by remember { 
                    mutableDoubleStateOf(sharedPrefs.getFloat("total_allowance", 0f).toDouble()) 
                }

                var selectedCycleRange by remember { mutableStateOf<CycleManager.CycleRange?>(null) }

                val saveData = {
                    sharedPrefs.edit().apply {
                        putString("all_users", gson.toJson(allUsers.toList()))
                        putString("expenses", gson.toJson(allExpenses.toList()))
                        putString("budgets", gson.toJson(allBudgets.toList()))
                        putString("goals", gson.toJson(goals.toList()))
                        putString("notifications", gson.toJson(notifications.toList()))
                        putFloat("total_allowance", totalAllowance.toFloat())
                        apply()
                    }
                }

                LaunchedEffect(Unit) {
                    sharedPrefs.getString("all_users", null)?.let {
                        val type = object : TypeToken<List<User>>() {}.type
                        val list: List<User> = gson.fromJson(it, type)
                        allUsers.clear()
                        allUsers.addAll(list)
                    } ?: run {
                        // Migration: if there's an old single user, add it to the list
                        sharedPrefs.getString("user_data", null)?.let {
                            try {
                                val oldUser = gson.fromJson(it, User::class.java)
                                if (allUsers.none { u -> u.email == oldUser.email }) {
                                    allUsers.add(oldUser)
                                }
                            } catch (e: Exception) {}
                        }
                    }

                    sharedPrefs.getString("expenses", null)?.let {
                        val type = object : TypeToken<List<ExpenseItem>>() {}.type
                        val list: List<ExpenseItem> = gson.fromJson(it, type)
                        allExpenses.clear()
                        allExpenses.addAll(list)
                    }
                    
                    sharedPrefs.getString("budgets", null)?.let {
                        val type = object : TypeToken<List<BudgetItem>>() {}.type
                        val list: List<BudgetItem> = gson.fromJson(it, type)
                        allBudgets.clear()
                        allBudgets.addAll(list)
                    }

                    sharedPrefs.getString("goals", null)?.let {
                        val type = object : TypeToken<List<Goal>>() {}.type
                        val list: List<Goal> = gson.fromJson(it, type)
                        goals.clear()
                        goals.addAll(list)
                    }

                    sharedPrefs.getString("notifications", null)?.let {
                        val type = object : TypeToken<List<NotificationItem>>() {}.type
                        val rawList: List<NotificationItem> = gson.fromJson(it, type)
                        
                        val sanitizedList = rawList.filterNotNull().map { item ->
                            NotificationItem(
                                id = item.id ?: UUID.randomUUID().toString(),
                                title = item.title ?: "Notification",
                                message = item.message ?: "",
                                category = item.category ?: "General",
                                type = item.type ?: NotificationType.GENERAL,
                                timestamp = item.timestamp ?: System.currentTimeMillis(),
                                isRead = item.isRead ?: false
                            )
                        }
                        notifications.clear()
                        notifications.addAll(sanitizedList)
                    }
                }

                LaunchedEffect(currentUser) {
                    currentUser?.cycleStartDate?.let { startStr ->
                        if (selectedCycleRange == null) {
                            selectedCycleRange = CycleManager.getCycleRange(startStr)
                        }
                    }
                }

                // Filter data based on current user (Privacy)
                val filteredExpenses by remember(allExpenses, selectedCycleRange, currentUser) {
                    derivedStateOf {
                        val userOwned = allExpenses.filter { it.userId == currentUser?.id || it.userId == null }
                        selectedCycleRange?.let { range ->
                            userOwned.filter { CycleManager.isDateInCycle(it.date ?: "", range) }
                        } ?: userOwned.toList()
                    }
                }

                val filteredBudgets by remember(allBudgets, selectedCycleRange, currentUser) {
                    derivedStateOf {
                        val userOwned = allBudgets.filter { it.userId == currentUser?.id || it.userId == null }
                        selectedCycleRange?.let { range ->
                            userOwned.filter { CycleManager.isDateInCycle(it.date ?: "", range) }
                        } ?: userOwned.toList()
                    }
                }

                fun parseAmount(amountStr: String): Double {
                    return amountStr.replace("₱", "").replace(",", "").toDoubleOrNull() ?: 0.0
                }

                val totalBudgets by remember { derivedStateOf { filteredBudgets.sumOf { parseAmount(it.budget ?: "0") } } }
                val totalExpenses by remember { derivedStateOf { filteredExpenses.sumOf { parseAmount(it.amount ?: "0") } } }
                val balance by remember { derivedStateOf { totalAllowance - totalExpenses } }

                val budgetsWithSpent by remember {
                    derivedStateOf {
                        filteredBudgets.map { budget ->
                            val spent = filteredExpenses
                                .filter { it.category?.equals(budget.category, ignoreCase = true) == true }
                                .sumOf { parseAmount(it.amount ?: "0") }
                            budget.copy(spent = "₱$spent")
                        }
                    }
                }

                fun playNotificationSound() {
                    try {
                        val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        val r = RingtoneManager.getRingtone(context, notificationUri)
                        r.play()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                fun checkBudgetsAndNotify(category: String) {
                    val budgetItem = filteredBudgets.find { it.category?.equals(category, ignoreCase = true) == true } ?: return
                    val budgetLimit = parseAmount(budgetItem.budget ?: "0")
                    val totalSpent = filteredExpenses
                        .filter { it.category?.equals(category, ignoreCase = true) == true }
                        .sumOf { parseAmount(it.amount ?: "0") }

                    var triggered = false
                    if (totalSpent > budgetLimit) {
                        notifications.add(0, NotificationItem(
                            title = "Overspending Alert",
                            message = "You have exceeded your $category budget by ₱${String.format(Locale.US, "%,.2f", totalSpent - budgetLimit)}.",
                            category = category,
                            type = NotificationType.OVERSPENDING
                        ))
                        triggered = true
                    } else if (totalSpent == budgetLimit) {
                        notifications.add(0, NotificationItem(
                            title = "Budget Limit Reached",
                            message = "You have fully used your $category budget.",
                            category = category,
                            type = NotificationType.BUDGET_REACHED
                        ))
                        triggered = true
                    } else if (totalSpent >= budgetLimit * 0.8) {
                        notifications.add(0, NotificationItem(
                            title = "Warning",
                            message = "You are close to reaching your $category budget. Spent: ₱${String.format(Locale.US, "%,.2f", totalSpent)} of ₱${String.format(Locale.US, "%,.2f", budgetLimit)}",
                            category = category,
                            type = NotificationType.WARNING
                        ))
                        triggered = true
                    }
                    
                    if (triggered) {
                        playNotificationSound()
                    }
                    saveData()
                }

                fun checkGoalsAndNotify() {
                    goals.forEach { goal ->
                        if (balance >= (goal.targetAmount ?: 0.0) && (goal.targetAmount ?: 0.0) > 0) {
                            val alreadyNotified = notifications.any { 
                                it.type == NotificationType.SAVINGS && it.title?.contains(goal.title ?: "") == true && it.message?.contains("reached") == true 
                            }
                            if (!alreadyNotified) {
                                notifications.add(0, NotificationItem(
                                    title = "Goal Reached! 🥳",
                                    message = "Congratulations! You have successfully reached your goal: ${goal.title}",
                                    category = "Goal",
                                    type = NotificationType.SAVINGS
                                ))
                                playNotificationSound()
                            }
                        }
                    }
                }

                fun performLogout() {
                    currentScreen = Screen.LOGIN
                    currentUser = null 
                    selectedCycleRange = null
                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (currentScreen) {
                            Screen.LOGIN -> LoginScreen(
                                onLoginClick = { email, password ->
                                    val user = allUsers.find { it.email == email && it.password == password }
                                    if (user != null) {
                                        currentUser = user
                                        currentScreen = Screen.MPIN
                                    } else {
                                        Toast.makeText(context, "Invalid credentials", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onRegisterClick = { currentScreen = Screen.REGISTER }
                            )

                            Screen.REGISTER -> RegisterScreen(
                                onRegisterComplete = { newUser ->
                                    val startDateStr = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                                    val userWithCycle = newUser.copy(cycleStartDate = startDateStr)
                                    allUsers.add(userWithCycle)
                                    saveData()
                                    Toast.makeText(context, "Registration Successful", Toast.LENGTH_SHORT).show()
                                    currentScreen = Screen.LOGIN
                                },
                                onBackToLogin = { currentScreen = Screen.LOGIN }
                            )

                            Screen.MPIN -> MPINScreen(
                                userName = currentUser?.name ?: "ka-Tipid",
                                onMpinComplete = { mpin ->
                                    if (currentUser != null && mpin != currentUser?.mpin) {
                                        Toast.makeText(context, "Incorrect MPIN", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                                        currentScreen = when (currentUser?.role) {
                                            UserRole.STAFF -> Screen.STAFF_DASHBOARD
                                            UserRole.ADMIN -> Screen.ADMIN_DASHBOARD
                                            else -> Screen.HOME
                                        }
                                    }
                                }
                            )

                            Screen.HOME -> {
                                HomeScreen(
                                    balance = balance,
                                    allowance = totalBudgets,
                                    expensesAmount = totalExpenses,
                                    goals = goals,
                                    user = currentUser,
                                    onUpdateBalance = { },
                                    onUpdateAllowance = { amount -> 
                                        totalAllowance += amount
                                        checkGoalsAndNotify()
                                        saveData()
                                    },
                                    onUpdateExpensesAmount = { },
                                    onUpdateProfileImage = { uri -> 
                                        val index = allUsers.indexOfFirst { it.id == currentUser?.id }
                                        if (index != -1) {
                                            val updated = allUsers[index].copy(profileImageUri = uri.toString())
                                            allUsers[index] = updated
                                            currentUser = updated
                                            saveData()
                                        }
                                    },
                                    onLogout = { performLogout() },
                                    onNavigateToExpenses = { currentScreen = Screen.EXPENSES },
                                    onNavigateToBudgets = { currentScreen = Screen.BUDGETS },
                                    onNavigateToReports = { currentScreen = Screen.REPORTS },
                                    onAddExpenseItem = { amount, category, notes ->
                                        val currentDate = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                                        allExpenses.add(0, ExpenseItem(
                                            date = currentDate,
                                            category = category,
                                            amount = "₱$amount",
                                            notes = notes,
                                            userId = currentUser?.id
                                        ))
                                        checkBudgetsAndNotify(category)
                                        saveData()
                                    },
                                    selectedCycle = selectedCycleRange,
                                    availableCycles = CycleManager.getAllCycles(currentUser?.cycleStartDate),
                                    onCycleSelected = { selectedCycleRange = it },
                                    onNotificationClick = { 
                                        currentScreen = Screen.NOTIFICATIONS 
                                        val updatedNotifications = notifications.map { it.copy(isRead = true) }
                                        notifications.clear()
                                        notifications.addAll(updatedNotifications)
                                        saveData()
                                    },
                                    unreadNotificationsCount = notifications.count { it.isRead == false }
                                )
                            }

                            Screen.STAFF_DASHBOARD -> {
                                StaffDashboard(
                                    user = currentUser,
                                    allExpenses = allExpenses,
                                    allBudgets = allBudgets,
                                    onLogout = { performLogout() },
                                    onUpdateProfileImage = { uri ->
                                        val index = allUsers.indexOfFirst { it.id == currentUser?.id }
                                        if (index != -1) {
                                            val updated = allUsers[index].copy(profileImageUri = uri.toString())
                                            allUsers[index] = updated
                                            currentUser = updated
                                            saveData()
                                        }
                                    }
                                )
                            }

                            Screen.ADMIN_DASHBOARD -> {
                                AdminDashboard(
                                    user = currentUser,
                                    allUsers = allUsers,
                                    onDeleteUser = { target ->
                                        allUsers.remove(target)
                                        saveData()
                                    },
                                    onUpdateUser = { target ->
                                        val index = allUsers.indexOfFirst { it.id == target.id }
                                        if (index != -1) {
                                            allUsers[index] = target
                                            saveData()
                                        }
                                    },
                                    onAddUser = { name, email, role ->
                                        allUsers.add(User(name = name, email = email, role = role))
                                        saveData()
                                    },
                                    onLogout = { performLogout() },
                                    onUpdateProfileImage = { uri ->
                                        val index = allUsers.indexOfFirst { it.id == currentUser?.id }
                                        if (index != -1) {
                                            val updated = allUsers[index].copy(profileImageUri = uri.toString())
                                            allUsers[index] = updated
                                            currentUser = updated
                                            saveData()
                                        }
                                    }
                                )
                            }

                            Screen.EXPENSES -> {
                                ExpensesScreen(
                                    expenses = filteredExpenses,
                                    user = currentUser,
                                    onUpdateProfileImage = { uri -> 
                                        val index = allUsers.indexOfFirst { it.id == currentUser?.id }
                                        if (index != -1) {
                                            val updated = allUsers[index].copy(profileImageUri = uri.toString())
                                            allUsers[index] = updated
                                            currentUser = updated
                                            saveData()
                                        }
                                    },
                                    onLogout = { performLogout() },
                                    onHomeClick = { currentScreen = Screen.HOME },
                                    onBudgetsClick = { currentScreen = Screen.BUDGETS },
                                    onReportsClick = { currentScreen = Screen.REPORTS },
                                    onAddExpense = { amount, category, notes ->
                                        val currentDate = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                                        allExpenses.add(0, ExpenseItem(
                                            date = currentDate,
                                            category = category,
                                            amount = "₱$amount",
                                            notes = notes,
                                            userId = currentUser?.id
                                        ))
                                        checkBudgetsAndNotify(category)
                                        saveData()
                                    },
                                    selectedCycle = selectedCycleRange,
                                    availableCycles = CycleManager.getAllCycles(currentUser?.cycleStartDate),
                                    onCycleSelected = { selectedCycleRange = it },
                                    onNotificationClick = { 
                                        currentScreen = Screen.NOTIFICATIONS 
                                        val updatedNotifications = notifications.map { it.copy(isRead = true) }
                                        notifications.clear()
                                        notifications.addAll(updatedNotifications)
                                        saveData()
                                    },
                                    unreadNotificationsCount = notifications.count { it.isRead == false }
                                )
                            }

                            Screen.BUDGETS -> {
                                BudgetsScreen(
                                    budgets = budgetsWithSpent,
                                    hasExpenses = filteredExpenses.isNotEmpty(),
                                    onAddBudget = { budget -> 
                                        allBudgets.add(budget.copy(userId = currentUser?.id))
                                        saveData()
                                    },
                                    user = currentUser,
                                    onUpdateProfileImage = { uri -> 
                                        val index = allUsers.indexOfFirst { it.id == currentUser?.id }
                                        if (index != -1) {
                                            val updated = allUsers[index].copy(profileImageUri = uri.toString())
                                            allUsers[index] = updated
                                            currentUser = updated
                                            saveData()
                                        }
                                    },
                                    onLogout = { performLogout() },
                                    onHomeClick = { currentScreen = Screen.HOME },
                                    onExpensesClick = { currentScreen = Screen.EXPENSES },
                                    onReportsClick = { currentScreen = Screen.REPORTS },
                                    selectedCycle = selectedCycleRange,
                                    availableCycles = CycleManager.getAllCycles(currentUser?.cycleStartDate),
                                    onCycleSelected = { selectedCycleRange = it },
                                    onNotificationClick = { 
                                        currentScreen = Screen.NOTIFICATIONS 
                                        val updatedNotifications = notifications.map { it.copy(isRead = true) }
                                        notifications.clear()
                                        notifications.addAll(updatedNotifications)
                                        saveData()
                                    },
                                    unreadNotificationsCount = notifications.count { it.isRead == false }
                                )
                            }

                            Screen.REPORTS -> {
                                ReportsScreen(
                                    expenses = filteredExpenses,
                                    budgets = filteredBudgets,
                                    onHomeClick = { currentScreen = Screen.HOME },
                                    onExpensesClick = { currentScreen = Screen.EXPENSES },
                                    onBudgetsClick = { currentScreen = Screen.BUDGETS },
                                    onUpdateProfileImage = { uri -> 
                                        val index = allUsers.indexOfFirst { it.id == currentUser?.id }
                                        if (index != -1) {
                                            val updated = allUsers[index].copy(profileImageUri = uri.toString())
                                            allUsers[index] = updated
                                            currentUser = updated
                                            saveData()
                                        }
                                    },
                                    onLogout = { performLogout() },
                                    user = currentUser,
                                    selectedCycle = selectedCycleRange,
                                    availableCycles = CycleManager.getAllCycles(currentUser?.cycleStartDate),
                                    onCycleSelected = { selectedCycleRange = it },
                                    onNotificationClick = { 
                                        currentScreen = Screen.NOTIFICATIONS 
                                        val updatedNotifications = notifications.map { it.copy(isRead = true) }
                                        notifications.clear()
                                        notifications.addAll(updatedNotifications)
                                        saveData()
                                    },
                                    unreadNotificationsCount = notifications.count { it.isRead == false }
                                )
                            }

                            Screen.NOTIFICATIONS -> {
                                NotificationScreen(
                                    notifications = notifications,
                                    onBackClick = { currentScreen = Screen.HOME },
                                    onClearAllClick = {
                                        notifications.clear()
                                        saveData()
                                    },
                                    onNotificationClick = { notification ->
                                        val index = notifications.indexOfFirst { it.id == notification.id }
                                        if (index != -1) {
                                            notifications[index] = notifications[index].copy(isRead = true)
                                            saveData()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun <T> List<T>.toMutableStateList(): androidx.compose.runtime.snapshots.SnapshotStateList<T> {
    val list = androidx.compose.runtime.mutableStateListOf<T>()
    list.addAll(this)
    return list
}
