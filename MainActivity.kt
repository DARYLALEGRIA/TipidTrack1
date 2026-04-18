package com.example.tipidtrack

import android.content.Context
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
import com.example.tipidtrack.ui.BudgetItem
import com.example.tipidtrack.ui.BudgetsScreen
import com.example.tipidtrack.ui.ExpenseItem
import com.example.tipidtrack.ui.ExpensesScreen
import com.example.tipidtrack.ui.Goal
import com.example.tipidtrack.ui.HomeScreen
import com.example.tipidtrack.ui.LoginScreen
import com.example.tipidtrack.ui.MPINScreen
import com.example.tipidtrack.ui.RegisterScreen
import com.example.tipidtrack.ui.ReportsScreen
import com.example.tipidtrack.ui.User
import com.example.tipidtrack.ui.CycleManager
import com.example.tipidtrack.ui.theme.TipidTrackTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Screen {
    LOGIN, REGISTER, MPIN, HOME, EXPENSES, BUDGETS, REPORTS
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
                
                // Load Registered User from SharedPreferences
                var registeredUser by remember {
                    mutableStateOf<User?>(
                        sharedPrefs.getString("user_data", null)?.let {
                            try {
                                gson.fromJson(it, User::class.java)
                            } catch (e: Exception) {
                                null
                            }
                        }
                    )
                }

                // Shared State (Current session user)
                var currentUser by remember { mutableStateOf<User?>(null) }

                // All historical data
                val allExpenses = remember { mutableStateListOf<ExpenseItem>() }
                val allBudgets = remember { mutableStateListOf<BudgetItem>() }
                val goals = remember { mutableStateListOf<Goal>() }
                
                var totalAllowance by remember { 
                    mutableDoubleStateOf(sharedPrefs.getFloat("total_allowance", 0f).toDouble()) 
                }

                // Cycle State
                var selectedCycleRange by remember { mutableStateOf<CycleManager.CycleRange?>(null) }

                // Helper to save data to persistent storage
                val saveData = {
                    sharedPrefs.edit().apply {
                        putString("user_data", gson.toJson(registeredUser))
                        putString("expenses", gson.toJson(allExpenses.toList()))
                        putString("budgets", gson.toJson(allBudgets.toList()))
                        putString("goals", gson.toJson(goals.toList()))
                        putFloat("total_allowance", totalAllowance.toFloat())
                        apply()
                    }
                }

                // Persistence: Load all data on app startup
                LaunchedEffect(Unit) {
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
                }

                // Set initial cycle when user logs in
                LaunchedEffect(currentUser) {
                    currentUser?.cycleStartDate?.let { startStr ->
                        if (selectedCycleRange == null) {
                            selectedCycleRange = CycleManager.getCycleRange(startStr)
                        }
                    }
                }

                // Filtered Lists based on selected cycle
                val filteredExpenses = remember(allExpenses, selectedCycleRange) {
                    derivedStateOf {
                        selectedCycleRange?.let { range ->
                            allExpenses.filter { CycleManager.isDateInCycle(it.date ?: "", range) }
                        } ?: allExpenses.toList()
                    }
                }

                val filteredBudgets = remember(allBudgets, selectedCycleRange) {
                    derivedStateOf {
                        selectedCycleRange?.let { range ->
                            allBudgets.filter { CycleManager.isDateInCycle(it.date ?: "", range) }
                        } ?: allBudgets.toList()
                    }
                }

                // Helper to parse currency strings for calculations
                fun parseAmount(amountStr: String): Double {
                    return amountStr.replace("₱", "").replace(",", "").toDoubleOrNull() ?: 0.0
                }

                val totalBudgets by remember { derivedStateOf { filteredBudgets.value.sumOf { parseAmount(it.budget) } } }
                val totalExpenses by remember { derivedStateOf { filteredExpenses.value.sumOf { parseAmount(it.amount) } } }
                val balance by remember { derivedStateOf { totalAllowance - totalExpenses } }

                val budgetsWithSpent by remember {
                    derivedStateOf {
                        filteredBudgets.value.map { budget ->
                            val spent = allExpenses
                                .filter { it.category.trim().equals(budget.category?.trim() ?: "", ignoreCase = true) }
                                .sumOf { parseAmount(it.amount) }
                            budget.copy(spent = "₱$spent")
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
                                onLoginClick = { phone, email, password ->
                                    if (phone.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                                        val regUser = registeredUser
                                        if (regUser != null && 
                                            phone == regUser.phone && 
                                            email == regUser.email && 
                                            password == regUser.password) {
                                            currentUser = regUser
                                            currentScreen = Screen.MPIN
                                        } else {
                                            Toast.makeText(context, "Invalid credentials", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onRegisterClick = { currentScreen = Screen.REGISTER }
                            )

                            Screen.REGISTER -> RegisterScreen(
                                onRegisterComplete = { newUser ->
                                    // Set cycleStartDate on registration
                                    val startDateStr = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                                    val userWithCycle = newUser.copy(cycleStartDate = startDateStr)
                                    registeredUser = userWithCycle
                                    
                                    // Clear data for new account registration
                                    allExpenses.clear()
                                    allBudgets.clear()
                                    goals.clear()
                                    totalAllowance = 0.0

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
                                        currentScreen = Screen.HOME
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
                                        saveData()
                                    },
                                    onUpdateExpensesAmount = { },
                                    onUpdateProfileImage = { uri -> 
                                        currentUser = currentUser?.copy(profileImageUri = uri.toString())
                                        registeredUser = registeredUser?.copy(profileImageUri = uri.toString())
                                        saveData()
                                    },
                                    onLogout = { performLogout() },
                                    onNavigateToExpenses = { currentScreen = Screen.EXPENSES },
                                    onNavigateToBudgets = { currentScreen = Screen.BUDGETS },
                                    onNavigateToReports = { currentScreen = Screen.REPORTS },
                                    onAddExpenseItem = { amount, category, notes ->
                                        val currentDate = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                                        allExpenses.add(0, ExpenseItem(currentDate, category, "₱$amount", notes))
                                        saveData()
                                    },
                                    selectedCycle = selectedCycleRange,
                                    availableCycles = CycleManager.getAllCycles(currentUser?.cycleStartDate),
                                    onCycleSelected = { selectedCycleRange = it }
                                )
                            }

                            Screen.EXPENSES -> {
                                ExpensesScreen(
                                    expenses = filteredExpenses.value,
                                    user = currentUser,
                                    onUpdateProfileImage = { uri -> 
                                        currentUser = currentUser?.copy(profileImageUri = uri.toString())
                                        registeredUser = registeredUser?.copy(profileImageUri = uri.toString())
                                        saveData()
                                    },
                                    onLogout = { performLogout() },
                                    onHomeClick = { currentScreen = Screen.HOME },
                                    onBudgetsClick = { currentScreen = Screen.BUDGETS },
                                    onReportsClick = { currentScreen = Screen.REPORTS },
                                    onAddExpense = { amount, category, notes ->
                                        val currentDate = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                                        allExpenses.add(0, ExpenseItem(currentDate, category, "₱$amount", notes))
                                        saveData()
                                    },
                                    selectedCycle = selectedCycleRange,
                                    availableCycles = CycleManager.getAllCycles(currentUser?.cycleStartDate),
                                    onCycleSelected = { selectedCycleRange = it }
                                )
                            }

                            Screen.BUDGETS -> {
                                BudgetsScreen(
                                    budgets = budgetsWithSpent.value,
                                    hasExpenses = filteredExpenses.value.isNotEmpty(),
                                    onAddBudget = { budget -> 
                                        val currentDate = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                                        allBudgets.add(budget.copy(date = currentDate))
                                        saveData()
                                    },
                                    user = currentUser,
                                    onUpdateProfileImage = { uri -> 
                                        currentUser = currentUser?.copy(profileImageUri = uri.toString())
                                        registeredUser = registeredUser?.copy(profileImageUri = uri.toString())
                                        saveData()
                                    },
                                    onLogout = { performLogout() },
                                    onHomeClick = { currentScreen = Screen.HOME },
                                    onExpensesClick = { currentScreen = Screen.EXPENSES },
                                    onReportsClick = { currentScreen = Screen.REPORTS },
                                    selectedCycle = selectedCycleRange,
                                    availableCycles = CycleManager.getAllCycles(currentUser?.cycleStartDate),
                                    onCycleSelected = { selectedCycleRange = it }
                                )
                            }

                            Screen.REPORTS -> {
                                ReportsScreen(
                                    expenses = filteredExpenses.value,
                                    budgets = filteredBudgets.value,
                                    onHomeClick = { currentScreen = Screen.HOME },
                                    onExpensesClick = { currentScreen = Screen.EXPENSES },
                                    onBudgetsClick = { currentScreen = Screen.BUDGETS },
                                    onUpdateProfileImage = { uri -> 
                                        currentUser = currentUser?.copy(profileImageUri = uri.toString())
                                        registeredUser = registeredUser?.copy(profileImageUri = uri.toString())
                                        saveData()
                                    },
                                    onLogout = { performLogout() },
                                    user = currentUser,
                                    selectedCycle = selectedCycleRange,
                                    availableCycles = CycleManager.getAllCycles(currentUser?.cycleStartDate),
                                    onCycleSelected = { selectedCycleRange = it }
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
