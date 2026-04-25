package com.example.tipidtrack.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.NumberFormat
import java.util.*

@Composable
fun HomeScreen(
    balance: Double,
    allowance: Double,
    expensesAmount: Double,
    goals: SnapshotStateList<Goal>,
    user: User? = null,
    onUpdateBalance: (Double) -> Unit,
    onUpdateAllowance: (Double) -> Unit,
    onUpdateExpensesAmount: (Double) -> Unit,
    onUpdateProfileImage: (Uri) -> Unit = {},
    onLogout: () -> Unit = {},
    onAddExpenseItem: (String, String, String) -> Unit,
    onNavigateToExpenses: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    selectedCycle: CycleManager.CycleRange? = null,
    availableCycles: List<CycleManager.CycleRange> = emptyList(),
    onCycleSelected: (CycleManager.CycleRange) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    unreadNotificationsCount: Int = 0,
    onAddGoal: (Goal) -> Unit = {}
) {
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddAllowanceDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddSavingsGoalDialog by remember { mutableStateOf(false) }
    var showAddItemGoalDialog by remember { mutableStateOf(false) }
    var showAccountDetailsDialog by remember { mutableStateOf(false) }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PH"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE), Color(0xFFE0F7FA))
                )
            )
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    PiggyBankIcon(modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TipidTrack",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row {
                BadgedBox(
                    badge = {
                        if (unreadNotificationsCount > 0) {
                            Badge(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            ) {
                                Text(unreadNotificationsCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Notifications, 
                        contentDescription = "Notifications", 
                        tint = Color.White,
                        modifier = Modifier.clickable { onNotificationClick() }
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    Icons.Default.Person, 
                    contentDescription = "Account", 
                    tint = Color.White,
                    modifier = Modifier.clickable { showAccountDetailsDialog = true }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Current Balance",
                color = Color.White,
                fontSize = 18.sp
            )
            Text(
                text = currencyFormatter.format(balance),
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { showAddAllowanceDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Stats Row
            Row(modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    title = "Budgets",
                    amount = currencyFormatter.format(allowance),
                    modifier = Modifier.weight(1f).clickable { onNavigateToBudgets() },
                    isIncrease = true
                )
                Spacer(modifier = Modifier.width(16.dp))
                StatCard(
                    title = "Expenses",
                    amount = currencyFormatter.format(expensesAmount),
                    modifier = Modifier.weight(1f).clickable { onNavigateToExpenses() },
                    isIncrease = false
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display Goals
            goals.forEach { goal ->
                val target = goal.targetAmount ?: 0.0
                val progress = if (target > 0) (balance.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
                val remainingAmount = (target - balance).coerceAtLeast(0.0)
                
                GoalCard(
                    title = goal.title ?: "",
                    subtitle = goal.subtitle ?: "",
                    progress = progress,
                    currentAmount = currencyFormatter.format(balance),
                    targetAmount = currencyFormatter.format(target),
                    footerText = if (target > 0) {
                        if (remainingAmount > 0) {
                            "Need ${currencyFormatter.format(remainingAmount)} to reach your goal"
                        } else {
                            "Goal Reached! 🎉"
                        }
                    } else null,
                    icon = goal.icon ?: "🎯"
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Button(
                    onClick = { showAddGoalDialog = true },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBBDEFB)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Goal", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Dialogs
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onAdd = { amountStr, category, notes ->
                onAddExpenseItem(amountStr, category, notes)
                showAddExpenseDialog = false
                onNavigateToExpenses()
            }
        )
    }

    if (showAddAllowanceDialog) {
        AmountInputDialog(
            title = "Add Allowance",
            onDismiss = { showAddAllowanceDialog = false },
            onConfirm = { amount ->
                onUpdateAllowance(amount)
                showAddAllowanceDialog = false
            }
        )
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onSavingsClick = {
                showAddGoalDialog = false
                showAddSavingsGoalDialog = true
            },
            onItemClick = {
                showAddGoalDialog = false
                showAddItemGoalDialog = true
            }
        )
    }

    if (showAddSavingsGoalDialog) {
        AddSavingsGoalDialog(
            onDismiss = { showAddSavingsGoalDialog = false },
            onAdd = { amountStr, date ->
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                val subtitleText = if (date.isNotBlank()) "Targeted by $date" else "General Savings"
                onAddGoal(
                    Goal(
                        title = "Savings Goal",
                        subtitle = subtitleText,
                        targetAmount = amount,
                        currentAmount = balance,
                        icon = "🎯",
                        targetDate = date.ifBlank { null }
                    )
                )
                showAddSavingsGoalDialog = false
            }
        )
    }

    if (showAddItemGoalDialog) {
        AddItemGoalDialog(
            onDismiss = { showAddItemGoalDialog = false },
            onAdd = { item, amountStr, date ->
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                val subtitleText = if (date.isNotBlank()) "$item (Targeted by $date)" else item
                onAddGoal(
                    Goal(
                        title = "Item Goal",
                        subtitle = subtitleText,
                        targetAmount = amount,
                        currentAmount = balance,
                        icon = getItemEmoji(item),
                        targetDate = date.ifBlank { null }
                    )
                )
                showAddItemGoalDialog = false
            }
        )
    }

    if (showAccountDetailsDialog) {
        AccountDetailsDialog(
            user = user,
            onDismiss = { showAccountDetailsDialog = false },
            onLogout = onLogout,
            onImageSelected = onUpdateProfileImage
        )
    }

    // Fixed Bottom Navigation Bar
    TipidTrackBottomNavigation(
        currentScreen = "HOME",
        onHomeClick = { },
        onBudgetsClick = onNavigateToBudgets,
        onExpensesClick = onNavigateToExpenses,
        onReportsClick = onNavigateToReports
    )
}

fun getItemEmoji(item: String): String {
    val lowerItem = item.lowercase(Locale.getDefault())
    return when {
        lowerItem.contains("shoes") || lowerItem.contains("sneakers") -> "👟"
        lowerItem.contains("bag") || lowerItem.contains("backpack") -> "🎒"
        lowerItem.contains("phone") || lowerItem.contains("mobile") || lowerItem.contains("iphone") -> "📱"
        lowerItem.contains("laptop") || lowerItem.contains("computer") || lowerItem.contains("pc") -> "💻"
        lowerItem.contains("car") || lowerItem.contains("vehicle") -> "🚗"
        lowerItem.contains("house") || lowerItem.contains("home") -> "🏠"
        lowerItem.contains("food") || lowerItem.contains("pizza") || lowerItem.contains("burger") -> "🍕"
        lowerItem.contains("travel") || lowerItem.contains("trip") || lowerItem.contains("flight") -> "✈️"
        lowerItem.contains("watch") -> "⌚"
        lowerItem.contains("headphones") || lowerItem.contains("airpods") -> "🎧"
        lowerItem.contains("shirt") || lowerItem.contains("clothes") -> "👕"
        lowerItem.contains("pants") -> "👖"
        lowerItem.contains("dress") -> "👗"
        lowerItem.contains("glasses") -> "👓"
        lowerItem.contains("bike") || lowerItem.contains("bicycle") -> "🚲"
        lowerItem.contains("camera") -> "📷"
        lowerItem.contains("game") || lowerItem.contains("ps5") || lowerItem.contains("xbox") || lowerItem.contains("switch") -> "🎮"
        lowerItem.contains("console") -> "🕹️"
        lowerItem.contains("book") -> "📖"
        lowerItem.contains("coffee") -> "☕"
        else -> "📦"
    }
}

@Composable
fun StatCard(title: String, amount: String, modifier: Modifier, isIncrease: Boolean) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFBBDEFB).copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isIncrease) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (isIncrease) Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = title, fontSize = 12.sp, color = Color(0xFF1976D2))
                    Text(text = amount, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D4B8E))
                }
            }
        }
    }
}

@Composable
fun GoalCard(
    title: String,
    subtitle: String,
    progress: Float,
    currentAmount: String,
    targetAmount: String,
    footerText: String? = null,
    icon: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFBBDEFB).copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = icon, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF2D4B8E))
                        Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF1976D2))
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Progress", fontSize = 10.sp, color = Color(0xFF1976D2))
                    Text(text = "${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2D4B8E))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = Color(0xFF4FACFE),
                trackColor = Color(0xFF2D4B8E).copy(alpha = 0.3f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = currentAmount, fontSize = 10.sp, color = Color(0xFF2D4B8E))
                Text(text = targetAmount, fontSize = 10.sp, color = Color(0xFF2D4B8E))
            }

            footerText?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = Color(0xFF2D4B8E),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AmountInputDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amountText = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                val amount = amountText.toDoubleOrNull() ?: 0.0
                onConfirm(amount)
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
