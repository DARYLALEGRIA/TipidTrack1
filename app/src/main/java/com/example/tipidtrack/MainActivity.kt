package com.example.tipidtrack

import android.media.RingtoneManager
import android.os.Bundle
import android.util.Log
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
import com.example.tipidtrack.model.User
import com.example.tipidtrack.model.UserRole
import com.example.tipidtrack.repository.*
import com.example.tipidtrack.ui.*
import com.example.tipidtrack.ui.theme.TipidTrackTheme
import com.example.tipidtrack.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.*

enum class Screen {
    LOGIN, REGISTER, MPIN, HOME, EXPENSES, BUDGETS, REPORTS, NOTIFICATIONS, STAFF_DASHBOARD, ADMIN_DASHBOARD
}

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val auth = Firebase.auth
        val db = Firebase.firestore

        val userRepository = FirebaseUserRepository(auth, db)
        val expenseRepository = FirebaseExpenseRepository(db)
        val budgetRepository = FirebaseBudgetRepository(db)
        val goalRepository = FirebaseGoalRepository(db)
        val notificationRepository = FirebaseNotificationRepository(db)
        val reportRepository = FirebaseReportRepository(db)

        viewModel = MainViewModel(
            userRepository,
            expenseRepository,
            budgetRepository,
            goalRepository,
            notificationRepository,
            reportRepository
        )

        setContent {
            TipidTrackTheme {
                val context = LocalContext.current
                var currentScreen by remember { mutableStateOf(Screen.LOGIN) }

                val currentUser = viewModel.currentUser
                val allUsers = viewModel.allUsers
                val allExpenses = viewModel.allExpenses
                val allBudgets = viewModel.allBudgets
                val allGoals = viewModel.allGoals
                val notifications = viewModel.notifications

                // Trigger sound whenever the latest notification changes and it is unread
                val latestNotificationId = notifications.firstOrNull()?.id
                LaunchedEffect(latestNotificationId) {
                    val latest = notifications.firstOrNull()
                    if (latest != null && latest.isRead == false) {
                        try {
                            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                            val r = RingtoneManager.getRingtone(context, notificationUri)
                            r?.play()
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error playing notification sound", e)
                        }
                    }
                }

                LaunchedEffect(currentUser) {
                    if (currentUser != null && currentScreen == Screen.LOGIN) {
                        currentScreen = when (currentUser.role) {
                            UserRole.STAFF -> Screen.STAFF_DASHBOARD
                            UserRole.ADMIN -> Screen.ADMIN_DASHBOARD
                            else -> if (currentUser.role == UserRole.STUDENT) Screen.MPIN else Screen.HOME
                        }
                    }
                }

                fun performLogout() {
                    auth.signOut()
                    currentScreen = Screen.LOGIN
                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (currentScreen) {
                            Screen.LOGIN -> LoginScreen(
                                onLoginClick = { username, password ->
                                    if (username.isBlank() || password.isBlank()) {
                                        Toast.makeText(context, "Please enter both username and password", Toast.LENGTH_SHORT).show()
                                        return@LoginScreen
                                    }
                                    val email = if (username.contains("@")) username else "$username@tipidtrack.com"
                                    
                                    Toast.makeText(context, "Logging in...", Toast.LENGTH_SHORT).show()
                                    
                                    auth.signInWithEmailAndPassword(email, password)
                                        .addOnSuccessListener {
                                            Log.d("MainActivity", "Auth success for $email")
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "Login failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            Log.e("MainActivity", "Login error", e)
                                        }
                                },
                                onRegisterClick = { currentScreen = Screen.REGISTER }
                            )

                            Screen.REGISTER -> RegisterScreen(
                                existingUsers = allUsers,
                                onRegisterComplete = { newUser ->
                                    val emailInput = newUser.email ?: ""
                                    val passwordInput = newUser.password ?: ""
                                    if (emailInput.isBlank() || passwordInput.isBlank()) {
                                        Toast.makeText(context, "Invalid registration details", Toast.LENGTH_SHORT).show()
                                        return@RegisterScreen
                                    }
                                    val email = if (emailInput.contains("@")) emailInput else "$emailInput@tipidtrack.com"
                                    
                                    Toast.makeText(context, "Registering...", Toast.LENGTH_SHORT).show()
                                    
                                    auth.createUserWithEmailAndPassword(email, passwordInput)
                                        .addOnSuccessListener { result ->
                                            val uid = result.user?.uid ?: ""
                                            val startDateStr = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                                            val userToSave = newUser.copy(
                                                id = uid,
                                                email = email,
                                                cycleStartDate = startDateStr
                                            )
                                            db.collection("users").document(uid).set(userToSave)
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "Registration Successful", Toast.LENGTH_SHORT).show()
                                                    currentScreen = Screen.LOGIN
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(context, "Firestore Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "Auth Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                },
                                onBackToLogin = { currentScreen = Screen.LOGIN }
                            )

                            Screen.MPIN -> MPINScreen(
                                userName = currentUser?.name ?: "ka-Tipid",
                                onMpinComplete = { mpin ->
                                    if (currentUser != null && mpin != currentUser.mpin) {
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

                            Screen.HOME -> HomeScreen(
                                balance = viewModel.balance,
                                allowance = viewModel.totalBudgetsInCycle,
                                expensesAmount = viewModel.totalExpenses,
                                goals = allGoals,
                                user = currentUser,
                                onUpdateAllowance = { viewModel.addAllowance(it) },
                                onUpdateProfileImage = { viewModel.updateProfileImage(it.toString()) },
                                onLogout = { performLogout() },
                                onNavigateToExpenses = { currentScreen = Screen.EXPENSES },
                                onNavigateToBudgets = { currentScreen = Screen.BUDGETS },
                                onNavigateToReports = { currentScreen = Screen.REPORTS },
                                onAddExpenseItem = { amount, category, notes ->
                                    viewModel.addExpense(amount, category, notes)
                                },
                                onAddGoal = { viewModel.addGoal(it) },
                                onDeleteGoal = { viewModel.deleteGoal(it) },
                                selectedCycle = viewModel.selectedCycleRange,
                                availableCycles = CycleManager.getAllCycles(currentUser?.cycleStartDate),
                                onCycleSelected = { viewModel.selectedCycleRange = it },
                                onNotificationClick = { currentScreen = Screen.NOTIFICATIONS },
                                unreadNotificationsCount = notifications.count { it.isRead == false }
                            )

                            Screen.STAFF_DASHBOARD -> StaffDashboard(
                                user = currentUser,
                                allExpenses = allExpenses,
                                allBudgets = allBudgets,
                                allUsers = allUsers,
                                onLogout = { performLogout() },
                                onUpdateProfileImage = { viewModel.updateProfileImage(it.toString()) }
                            )

                            Screen.ADMIN_DASHBOARD -> AdminDashboard(
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
                                    val newUser = User(name = name, email = email, role = role, id = UUID.randomUUID().toString())
                                    db.collection("users").document(newUser.id).set(newUser)
                                        .addOnSuccessListener { Toast.makeText(context, "User added", Toast.LENGTH_SHORT).show() }
                                },
                                onLogout = { performLogout() },
                                onUpdateProfileImage = { viewModel.updateProfileImage(it.toString()) }
                            )

                            Screen.EXPENSES -> ExpensesScreen(
                                expenses = viewModel.filteredExpenses,
                                user = currentUser,
                                onUpdateProfileImage = { viewModel.updateProfileImage(it.toString()) },
                                onLogout = { performLogout() },
                                onHomeClick = { currentScreen = Screen.HOME },
                                onBudgetsClick = { currentScreen = Screen.BUDGETS },
                                onReportsClick = { currentScreen = Screen.REPORTS },
                                onAddExpense = { amount, category, notes ->
                                    viewModel.addExpense(amount, category, notes)
                                },
                                onDeleteExpense = { viewModel.deleteExpense(it) },
                                selectedCycle = viewModel.selectedCycleRange,
                                availableCycles = CycleManager.getAllCycles(currentUser?.cycleStartDate),
                                onCycleSelected = { viewModel.selectedCycleRange = it },
                                onNotificationClick = { currentScreen = Screen.NOTIFICATIONS },
                                unreadNotificationsCount = notifications.count { it.isRead == false }
                            )

                            Screen.BUDGETS -> BudgetsScreen(
                                budgets = viewModel.budgetsWithSpent,
                                hasExpenses = true,
                                user = currentUser,
                                onUpdateProfileImage = { viewModel.updateProfileImage(it.toString()) },
                                onLogout = { performLogout() },
                                onHomeClick = { currentScreen = Screen.HOME },
                                onExpensesClick = { currentScreen = Screen.EXPENSES },
                                onReportsClick = { currentScreen = Screen.REPORTS },
                                onAddBudget = { budget ->
                                    viewModel.addBudget(budget)
                                },
                                onDeleteBudget = { viewModel.deleteBudget(it) },
                                selectedCycle = viewModel.selectedCycleRange,
                                availableCycles = CycleManager.getAllCycles(currentUser?.cycleStartDate),
                                onCycleSelected = { viewModel.selectedCycleRange = it },
                                onNotificationClick = { currentScreen = Screen.NOTIFICATIONS },
                                unreadNotificationsCount = notifications.count { it.isRead == false }
                            )

                            Screen.REPORTS -> ReportsScreen(
                                expenses = viewModel.filteredExpenses,
                                budgets = allBudgets,
                                user = currentUser,
                                onUpdateProfileImage = { viewModel.updateProfileImage(it.toString()) },
                                onLogout = { performLogout() },
                                onHomeClick = { currentScreen = Screen.HOME },
                                onExpensesClick = { currentScreen = Screen.EXPENSES },
                                onBudgetsClick = { currentScreen = Screen.BUDGETS },
                                selectedCycle = viewModel.selectedCycleRange,
                                availableCycles = CycleManager.getAllCycles(currentUser?.cycleStartDate),
                                onCycleSelected = { viewModel.selectedCycleRange = it },
                                onNotificationClick = { currentScreen = Screen.NOTIFICATIONS },
                                unreadNotificationsCount = notifications.count { it.isRead == false }
                            )

                            Screen.NOTIFICATIONS -> NotificationScreen(
                                notifications = notifications,
                                onBackClick = { currentScreen = Screen.HOME },
                                onClearAllClick = { viewModel.clearNotifications() },
                                onNotificationClick = { notification ->
                                    notification.id?.let { viewModel.markNotificationAsRead(it) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
