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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
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

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = Firebase.auth
        db = Firebase.firestore

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
                val allGoals = remember { mutableStateListOf<Goal>() }
                val notifications = remember { mutableStateListOf<NotificationItem>() }
                val allReports = remember { mutableStateListOf<ReportItem>() }
                
                // Drive totalAllowance directly from currentUser to ensure persistence
                val totalAllowance by remember(currentUser) {
                    derivedStateOf { currentUser?.totalAllowance ?: 0.0 }
                }

                var selectedCycleRange by remember { mutableStateOf<CycleManager.CycleRange?>(null) }

                val saveData = {
                    currentUser?.let { user ->
                        val userId = user.id
                        if (userId.isNotEmpty()) {
                            db.collection("users").document(userId).set(user)
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Error saving data: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }

                fun saveExpense(item: ExpenseItem) {
                    db.collection("expenses").add(item)
                }

                fun saveBudget(item: BudgetItem) {
                    db.collection("budgets").add(item)
                }

                fun saveGoal(item: Goal) {
                    db.collection("goals").add(item)
                }

                fun saveNotification(item: NotificationItem) {
                    val userId = currentUser?.id ?: return
                    db.collection("notifications").add(item.copy(userId = userId))
                }

                fun saveReport(item: ReportItem) {
                    val userId = currentUser?.id ?: return
                    db.collection("reports").add(item.copy(userId = userId))
                        .addOnSuccessListener {
                            Toast.makeText(context, "Report saved to cloud", Toast.LENGTH_SHORT).show()
                        }
                }

                fun updateNotification(item: NotificationItem) {
                    // This assumes notification has an 'id' that matches the document ID
                    // For simplicity in this migration, we might need to store the doc ID
                }

                LaunchedEffect(Unit) {
                    // Check if a user is already logged in
                    auth.currentUser?.let { firebaseUser ->
                        db.collection("users").document(firebaseUser.uid).get()
                            .addOnSuccessListener { document ->
                                val user = document.toObject(User::class.java)?.copy(id = firebaseUser.uid)
                                if (user != null) {
                                    currentUser = user
                                    currentScreen = when (user.role) {
                                        UserRole.STAFF -> Screen.STAFF_DASHBOARD
                                        UserRole.ADMIN -> Screen.ADMIN_DASHBOARD
                                        else -> Screen.HOME
                                    }
                                }
                            }
                    }
                }

                DisposableEffect(currentUser?.id) {
                    val userId = currentUser?.id
                    if (userId.isNullOrEmpty()) return@DisposableEffect onDispose {}
                    
                    // Real-time listener for the User profile to keep allowance/balance in sync
                    val userListener = db.collection("users").document(userId)
                        .addSnapshotListener { snapshot, _ ->
                            snapshot?.toObject(User::class.java)?.let { updatedUser ->
                                // Only update if there's a meaningful change to prevent loops
                                // We also ensure the ID is locked to the current UID
                                val userWithId = updatedUser.copy(id = userId)
                                if (userWithId != currentUser) {
                                    currentUser = userWithId
                                }
                            }
                        }

                    val expensesListener = db.collection("expenses")
                        .whereEqualTo("userId", userId)
                        .addSnapshotListener { snapshot, _ ->
                            snapshot?.let {
                                val list = it.toObjects(ExpenseItem::class.java)
                                allExpenses.clear()
                                allExpenses.addAll(list.sortedByDescending { e -> e.date })
                            }
                        }

                    val budgetsListener = db.collection("budgets")
                        .whereEqualTo("userId", userId)
                        .addSnapshotListener { snapshot, _ ->
                            snapshot?.let {
                                val list = it.toObjects(BudgetItem::class.java)
                                allBudgets.clear()
                                allBudgets.addAll(list)
                            }
                        }

                    val goalsListener = db.collection("goals")
                        .whereEqualTo("userId", userId)
                        .addSnapshotListener { snapshot, _ ->
                            snapshot?.let {
                                val list = it.toObjects(Goal::class.java)
                                allGoals.clear()
                                allGoals.addAll(list)
                            }
                        }
                    
                    val notificationsListener = db.collection("notifications")
                        .whereEqualTo("userId", userId)
                        .addSnapshotListener { snapshot, _ ->
                            snapshot?.let {
                                val list = it.toObjects(NotificationItem::class.java)
                                notifications.clear()
                                notifications.addAll(list.sortedByDescending { n -> n.timestamp })
                            }
                        }

                    val reportsListener = db.collection("reports")
                        .whereEqualTo("userId", userId)
                        .addSnapshotListener { snapshot, _ ->
                            snapshot?.let {
                                val list = it.toObjects(ReportItem::class.java)
                                allReports.clear()
                                allReports.addAll(list.sortedByDescending { r -> r.generatedAt })
                            }
                        }

                    onDispose {
                        userListener.remove()
                        expensesListener.remove()
                        budgetsListener.remove()
                        goalsListener.remove()
                        notificationsListener.remove()
                        reportsListener.remove()
                    }
                }

                DisposableEffect(currentUser?.role) {
                    val role = currentUser?.role ?: return@DisposableEffect onDispose {}
                    if (role == UserRole.ADMIN || role == UserRole.STAFF) {
                        val usersListener = db.collection("users").addSnapshotListener { snapshot, _ ->
                            snapshot?.let {
                                val list = it.toObjects(User::class.java)
                                allUsers.clear()
                                allUsers.addAll(list)
                            }
                        }
                        onDispose { usersListener.remove() }
                    } else {
                        onDispose {}
                    }
                }

                // Auto-select current cycle when login happens
                LaunchedEffect(currentUser?.cycleStartDate) {
                    if (selectedCycleRange == null && currentUser?.cycleStartDate != null) {
                        selectedCycleRange = CycleManager.getCycleRange(currentUser?.cycleStartDate)
                    }
                }

                // Filtered Lists based on selected cycle
                val filteredExpensesState = remember(allExpenses, selectedCycleRange, currentUser) {
                    derivedStateOf {
                        val userOwned = allExpenses.filter { it.userId == currentUser?.id }
                        selectedCycleRange?.let { range ->
                            userOwned.filter { CycleManager.isDateInCycle(it.date ?: "", range) }
                        } ?: userOwned.toList()
                    }
                }
                val filteredExpenses by filteredExpensesState

                val filteredBudgetsState = remember(allBudgets, selectedCycleRange, currentUser) {
                    derivedStateOf {
                        val userOwned = allBudgets.filter { it.userId == currentUser?.id }
                        selectedCycleRange?.let { range ->
                            userOwned.filter { CycleManager.isDateInCycle(it.date ?: "", range) }
                        } ?: userOwned.toList()
                    }
                }
                val filteredBudgets by filteredBudgetsState

                fun parseAmount(amountStr: String): Double {
                    return amountStr.replace("₱", "").replace(",", "").toDoubleOrNull() ?: 0.0
                }

                val totalBudgetsInCycle by remember(filteredBudgets) { 
                    derivedStateOf { 
                        filteredBudgets.sumOf { parseAmount(it.budget ?: "0") } 
                    } 
                }
                val totalExpenses by remember(allExpenses, currentUser) { 
                    derivedStateOf { 
                        allExpenses.filter { it.userId == currentUser?.id }.sumOf { parseAmount(it.amount ?: "0") } 
                    } 
                }
                
                // Balance is persistent allowance minus all-time expenses
                val balance by remember(totalAllowance, totalExpenses) { 
                    derivedStateOf { (totalAllowance - totalExpenses).coerceAtLeast(-999999.0) } 
                }

                val budgetsWithSpentState = remember(filteredBudgets, filteredExpenses) {
                    derivedStateOf {
                        filteredBudgets.map { budget ->
                            val spent = filteredExpenses
                                .filter { it.category?.trim().equals(budget.category?.trim() ?: "", ignoreCase = true) == true }
                                .sumOf { parseAmount(it.amount ?: "0") }
                            budget.copy(spent = "₱$spent")
                        }
                    }
                }
                val budgetsWithSpent by budgetsWithSpentState

                fun playNotificationSound() {
                    try {
                        val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        val r = RingtoneManager.getRingtone(context, notificationUri)
                        r.play()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                fun checkBudgetsAndNotify(category: String, addedAmount: Double = 0.0) {
                    val userId = currentUser?.id ?: return
                    val budgetItem = allBudgets.find { 
                        it.userId == userId && it.category?.trim().equals(category.trim(), ignoreCase = true) == true 
                    } ?: return
                    
                    val budgetLimit = parseAmount(budgetItem.budget ?: "0")
                    
                    // Use filtered expenses (cycle-aware) for notification logic
                    val currentSpentInCycle = filteredExpenses
                        .filter { it.category?.trim().equals(category.trim(), ignoreCase = true) == true }
                        .sumOf { parseAmount(it.amount ?: "0") }
                    
                    // If we just added an expense, it might not be in filteredExpenses yet due to async Firestore
                    // So we add it manually to the check
                    val totalSpent = currentSpentInCycle + addedAmount

                    var triggered = false
                    val newNotif = if (totalSpent > budgetLimit) {
                        triggered = true
                        NotificationItem(
                            title = "Overspending Alert",
                            message = "You have exceeded your $category budget by ₱${String.format(Locale.US, "%,.2f", totalSpent - budgetLimit)}.",
                            category = category,
                            type = NotificationType.OVERSPENDING,
                            userId = currentUser?.id
                        )
                    } else if (totalSpent == budgetLimit) {
                        triggered = true
                        NotificationItem(
                            title = "Budget Limit Reached",
                            message = "You have fully used your $category budget.",
                            category = category,
                            type = NotificationType.BUDGET_REACHED,
                            userId = currentUser?.id
                        )
                    } else if (totalSpent >= budgetLimit * 0.8) {
                        triggered = true
                        NotificationItem(
                            title = "Warning",
                            message = "You are close to reaching your $category budget. Spent: ₱${String.format(Locale.US, "%,.2f", totalSpent)} of ₱${String.format(Locale.US, "%,.2f", budgetLimit)}",
                            category = category,
                            type = NotificationType.WARNING,
                            userId = currentUser?.id
                        )
                    } else null
                    
                    if (triggered && newNotif != null) {
                        // Check if a similar notification was recently sent to avoid spam
                        // Shortened cooldown to 10 seconds for better responsiveness during testing
                        val recentlyNotified = notifications.take(5).any { 
                            it.type == newNotif.type && it.category == newNotif.category && (System.currentTimeMillis() - (it.timestamp ?: 0)) < 10000 
                        }
                        
                        if (!recentlyNotified) {
                            playNotificationSound()
                            saveNotification(newNotif)
                        }
                    }
                }

                fun checkGoalsAndNotify() {
                    allGoals.forEach { goal ->
                        val target = goal.targetAmount ?: 0.0
                        if (balance >= target && target > 0) {
                            val alreadyNotified = notifications.any { 
                                it.type == NotificationType.SAVINGS && it.title?.contains(goal.title ?: "") == true
                            }
                            if (!alreadyNotified) {
                                playNotificationSound()
                                saveNotification(NotificationItem(
                                    title = "Goal Reached! 🥳",
                                    message = "Congratulations! You have successfully reached your goal: ${goal.title}",
                                    category = "Goal",
                                    type = NotificationType.SAVINGS,
                                    userId = currentUser?.id
                                ))
                            }
                        }
                    }
                }

                fun performLogout() {
                    auth.signOut()
                    currentScreen = Screen.LOGIN
                    currentUser = null 
                    selectedCycleRange = null
                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (currentScreen) {
                            Screen.LOGIN -> LoginScreen(
                                onLoginClick = { username, password ->
                                    val email = if (username.contains("@")) username else "$username@tipidtrack.com"
                                    auth.signInWithEmailAndPassword(email, password)
                                        .addOnSuccessListener { result ->
                                            val uid = result.user?.uid ?: ""
                                            db.collection("users").document(uid).get()
                                                .addOnSuccessListener { document ->
                                                    val user = document.toObject(User::class.java)?.copy(id = uid)
                                                    if (user != null) {
                                                        currentUser = user
                                                        if (user.role == UserRole.STUDENT) {
                                                            currentScreen = Screen.MPIN
                                                        } else {
                                                            Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                                                            currentScreen = when (user.role) {
                                                                UserRole.STAFF -> Screen.STAFF_DASHBOARD
                                                                UserRole.ADMIN -> Screen.ADMIN_DASHBOARD
                                                                else -> Screen.HOME
                                                            }
                                                        }
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(context, "Failed to fetch user data", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Invalid credentials", Toast.LENGTH_SHORT).show()
                                        }
                                },
                                onRegisterClick = { currentScreen = Screen.REGISTER }
                            )

                            Screen.REGISTER -> RegisterScreen(
                                existingUsers = allUsers,
                                onRegisterComplete = { newUser ->
                                    val email = if (newUser.email?.contains("@") == true) newUser.email else "${newUser.email}@tipidtrack.com"
                                    auth.createUserWithEmailAndPassword(email, newUser.password ?: "")
                                        .addOnSuccessListener { result ->
                                            val uid = result.user?.uid ?: ""
                                            val startDateStr = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                                            val userToSave = newUser.copy(
                                                id = uid,
                                                cycleStartDate = startDateStr
                                            )
                                            
                                            db.collection("users").document(uid).set(userToSave)
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "Registration Successful", Toast.LENGTH_SHORT).show()
                                                    currentScreen = Screen.LOGIN
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
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
                                    allowance = totalBudgetsInCycle,
                                    expensesAmount = totalExpenses,
                                    goals = allGoals.toMutableStateList(),
                                    user = currentUser,
                                    onUpdateBalance = { },
                                    onUpdateAllowance = { amount -> 
                                        val newAllowance = (currentUser?.totalAllowance ?: 0.0) + amount
                                        val updatedUser = currentUser?.copy(totalAllowance = newAllowance)
                                        if (updatedUser != null) {
                                            currentUser = updatedUser
                                            saveData()
                                            checkGoalsAndNotify()
                                        }
                                    },
                                    onUpdateExpensesAmount = { },
                                    onUpdateProfileImage = { uri -> 
                                        currentUser = currentUser?.copy(profileImageUri = uri.toString())
                                        saveData()
                                    },
                                    onLogout = { performLogout() },
                                    onNavigateToExpenses = { currentScreen = Screen.EXPENSES },
                                    onNavigateToBudgets = { currentScreen = Screen.BUDGETS },
                                    onNavigateToReports = { currentScreen = Screen.REPORTS },
                                    onAddExpenseItem = { amount, category, notes ->
                                        val currentDate = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                                        val newItem = ExpenseItem(
                                            date = currentDate,
                                            category = category,
                                            amount = "₱$amount",
                                            notes = notes,
                                            userId = currentUser?.id
                                        )
                                        saveExpense(newItem)
                                        checkBudgetsAndNotify(category, parseAmount(amount))
                                    },
                                    onAddGoal = { goal: Goal ->
                                        val newItem = goal.copy(
                                            userId = currentUser?.id,
                                            createdAt = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                                        )
                                        saveGoal(newItem)
                                        // Trigger a check immediately after adding a goal
                                        checkGoalsAndNotify()
                                    },
                                    selectedCycle = selectedCycleRange,
                                    availableCycles = CycleManager.getAllCycles(currentUser?.cycleStartDate),
                                    onCycleSelected = { selectedCycleRange = it },
                                    onNotificationClick = { 
                                        currentScreen = Screen.NOTIFICATIONS 
                                        notifications.forEach { notif ->
                                            if (notif.isRead == false) {
                                                // Ideally update in Firestore
                                            }
                                        }
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
                                        currentUser = currentUser?.copy(profileImageUri = uri.toString())
                                        saveData()
                                    }
                                )
                            }

                            Screen.ADMIN_DASHBOARD -> {
                                AdminDashboard(
                                    user = currentUser,
                                    allUsers = allUsers,
                                    onDeleteUser = { target ->
                                        db.collection("users").document(target.id).delete()
                                            .addOnSuccessListener { Toast.makeText(context, "User deleted", Toast.LENGTH_SHORT).show() }
                                    },
                                    onUpdateUser = { target ->
                                        db.collection("users").document(target.id).set(target)
                                            .addOnSuccessListener { Toast.makeText(context, "User updated", Toast.LENGTH_SHORT).show() }
                                    },
                                    onAddUser = { name, email, role ->
                                        val newUser = User(name = name, email = email, role = role)
                                        db.collection("users").document(newUser.id).set(newUser)
                                            .addOnSuccessListener { Toast.makeText(context, "User added", Toast.LENGTH_SHORT).show() }
                                    },
                                    onLogout = { performLogout() },
                                    onUpdateProfileImage = { uri ->
                                        currentUser = currentUser?.copy(profileImageUri = uri.toString())
                                        saveData()
                                    }
                                )
                            }

                            Screen.EXPENSES -> {
                                ExpensesScreen(
                                    expenses = filteredExpenses,
                                    user = currentUser,
                                    onUpdateProfileImage = { uri -> 
                                        currentUser = currentUser?.copy(profileImageUri = uri.toString())
                                        saveData()
                                    },
                                    onLogout = { performLogout() },
                                    onHomeClick = { currentScreen = Screen.HOME },
                                    onBudgetsClick = { currentScreen = Screen.BUDGETS },
                                    onReportsClick = { currentScreen = Screen.REPORTS },
                                    onAddExpense = { amount, category, notes ->
                                        val currentDate = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                                        val newItem = ExpenseItem(
                                            date = currentDate,
                                            category = category,
                                            amount = "₱$amount",
                                            notes = notes,
                                            userId = currentUser?.id
                                        )
                                        saveExpense(newItem)
                                        checkBudgetsAndNotify(category, parseAmount(amount))
                                    },
                                    selectedCycle = selectedCycleRange,
                                    availableCycles = CycleManager.getAllCycles(currentUser?.cycleStartDate),
                                    onCycleSelected = { selectedCycleRange = it },
                                    onNotificationClick = { 
                                        currentScreen = Screen.NOTIFICATIONS 
                                    },
                                    unreadNotificationsCount = notifications.count { it.isRead == false }
                                )
                            }

                            Screen.BUDGETS -> {
                                BudgetsScreen(
                                    budgets = budgetsWithSpent,
                                    hasExpenses = filteredExpenses.isNotEmpty(),
                                    onAddBudget = { budget -> 
                                        val currentDate = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                                        val newItem = budget.copy(
                                            date = currentDate,
                                            userId = currentUser?.id
                                        )
                                        saveBudget(newItem)
                                    },
                                    user = currentUser,
                                    onUpdateProfileImage = { uri -> 
                                        currentUser = currentUser?.copy(profileImageUri = uri.toString())
                                        saveData()
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
                                        currentUser = currentUser?.copy(profileImageUri = uri.toString())
                                        saveData()
                                    },
                                    onLogout = { performLogout() },
                                    user = currentUser,
                                    selectedCycle = selectedCycleRange,
                                    availableCycles = CycleManager.getAllCycles(currentUser?.cycleStartDate),
                                    onCycleSelected = { selectedCycleRange = it },
                                    onNotificationClick = { 
                                        currentScreen = Screen.NOTIFICATIONS 
                                    },
                                    unreadNotificationsCount = notifications.count { it.isRead == false },
                                    onSaveReport = { notes: String ->
                                        val report = ReportItem(
                                            userId = currentUser?.id,
                                            cycleRange = selectedCycleRange?.let { CycleManager.formatCycle(it) } ?: "All Time",
                                            totalSpent = filteredExpenses.sumOf { parseAmount(it.amount ?: "0") },
                                            categoryBreakdown = filteredExpenses.groupBy { (it.category ?: "Other").uppercase() }
                                                .mapValues { entry -> entry.value.sumOf { parseAmount(it.amount ?: "0") } },
                                            notes = notes
                                        )
                                        saveReport(report)
                                    }
                                )
                            }

                            Screen.NOTIFICATIONS -> {
                                NotificationScreen(
                                    notifications = notifications,
                                    onBackClick = { currentScreen = Screen.HOME },
                                    onClearAllClick = {
                                        // Clear notifications from Firestore
                                        db.collection("notifications")
                                            .whereEqualTo("userId", currentUser?.id)
                                            .get()
                                            .addOnSuccessListener { snapshot ->
                                                for (doc in snapshot) {
                                                    doc.reference.delete()
                                                }
                                            }
                                    },
                                    onNotificationClick = { notification ->
                                        // Mark as read in Firestore
                                        db.collection("notifications")
                                            .whereEqualTo("userId", currentUser?.id)
                                            .whereEqualTo("timestamp", notification.timestamp)
                                            .get()
                                            .addOnSuccessListener { snapshot ->
                                                for (doc in snapshot) {
                                                    doc.reference.update("isRead", true)
                                                }
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
