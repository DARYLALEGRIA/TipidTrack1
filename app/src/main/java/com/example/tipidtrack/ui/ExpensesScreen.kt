package com.example.tipidtrack.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tipidtrack.model.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    expenses: List<ExpenseItem>,
    user: User? = null,
    onHomeClick: () -> Unit,
    onBudgetsClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    onUpdateProfileImage: (Uri) -> Unit = {},
    onLogout: () -> Unit = {},
    onAddExpense: (String, String, String) -> Unit = { _, _, _ -> },
    onDeleteExpense: (String) -> Unit = {},
    selectedCycle: CycleManager.CycleRange? = null,
    availableCycles: List<CycleManager.CycleRange> = emptyList(),
    onCycleSelected: (CycleManager.CycleRange) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    unreadNotificationsCount: Int = 0
) {
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAccountDetailsDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedDate by remember { mutableStateOf("All") }
    var categoryExpanded by remember { mutableStateOf(false) }
    
    var expenseToDelete by remember { mutableStateOf<ExpenseItem?>(null) }

    fun parseAmount(amountStr: String): Double {
        return amountStr.replace("₱", "").replace(",", "").toDoubleOrNull() ?: 0.0
    }

    fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    val categories = remember(expenses) {
        val userCats = expenses.map { (it.category ?: "").trim().capitalize() }.distinct().sorted()
        listOf("All") + userCats
    }

    val filteredExpenses = remember(expenses, selectedCategory, selectedDate) {
        expenses.filter { expense ->
            val matchesCategory = selectedCategory == "All" || (expense.category ?: "").trim().equals(selectedCategory, ignoreCase = true)
            val matchesDate = selectedDate == "All" || expense.date == selectedDate
            matchesCategory && matchesDate
        }.sortedByDescending { it.date }
    }

    val aggregatedData = remember(filteredExpenses) {
        filteredExpenses
            .groupBy { (it.category ?: "").trim().capitalize() }
            .mapValues { entry -> entry.value.sumOf { parseAmount(it.amount ?: "") } }
            .toList()
            .sortedByDescending { it.second }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val sdf = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
                        selectedDate = sdf.format(Date(it))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedDate = "All"
                    showDatePicker = false
                }) {
                    Text("Clear")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAccountDetailsDialog) {
        AccountDetailsDialog(
            user = user,
            onDismiss = { showAccountDetailsDialog = false },
            onLogout = onLogout,
            onImageSelected = onUpdateProfileImage
        )
    }

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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "EXPENSES",
                color = Color.Black,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            CycleSelector(
                selectedCycle = selectedCycle,
                availableCycles = availableCycles,
                onCycleSelected = onCycleSelected
            )

            // Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.Black)
                Text(text = "Filter", color = Color.Black, modifier = Modifier.padding(start = 4.dp, end = 8.dp))
                
                Box(modifier = Modifier.weight(1f)) {
                    FilterDropdown(
                        label = if (selectedCategory == "All") "Category" else selectedCategory,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { categoryExpanded = true }
                    )
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                FilterDropdown(
                    label = if (selectedDate == "All") "Date" else selectedDate,
                    modifier = Modifier.weight(1f),
                    onClick = { showDatePicker = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Summary Table
            if (aggregatedData.isNotEmpty()) {
                Text("SUMMARY BY CATEGORY", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black)) {
                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFBBDEFB).copy(alpha = 0.5f))) {
                        ExpenseHeaderCell("CATEGORY", Modifier.weight(1f))
                        ExpenseHeaderCell("TOTAL", Modifier.weight(1f))
                    }
                    aggregatedData.forEach { (category, total) ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ExpenseTableCell(category, Modifier.weight(1f))
                            ExpenseTableCell("₱${String.format(Locale.US, "%,.2f", total)}", Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Individual Transactions
            Text("TRANSACTION HISTORY", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.DarkGray)
            Text("(Long press to delete)", fontSize = 10.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            if (filteredExpenses.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black)) {
                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFBBDEFB).copy(alpha = 0.5f))) {
                        ExpenseHeaderCell("DATE", Modifier.weight(0.8f))
                        ExpenseHeaderCell("CAT", Modifier.weight(0.7f))
                        ExpenseHeaderCell("AMOUNT", Modifier.weight(1f))
                    }
                    filteredExpenses.forEach { expense ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectTapGestures(onLongPress = { expenseToDelete = expense })
                                }
                        ) {
                            ExpenseTableCell(expense.date ?: "", Modifier.weight(0.8f))
                            ExpenseTableCell(expense.category ?: "", Modifier.weight(0.7f))
                            ExpenseTableCell(expense.amount ?: "", Modifier.weight(1f))
                        }
                    }
                }
            } else {
                Text(
                    text = "No expenses found for this selection.",
                    color = Color.DarkGray,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showAddExpenseDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBBDEFB)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Add Expense", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onAdd = { amount, category, notes ->
                onAddExpense(amount, category, notes)
                showAddExpenseDialog = false
            }
        )
    }

    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Expense") },
            text = { Text("Are you sure you want to delete this expense record?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExpense(expenseToDelete!!.id)
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Fixed Bottom Navigation Bar
    TipidTrackBottomNavigation(
        currentScreen = "EXPENSES",
        onHomeClick = onHomeClick,
        onBudgetsClick = onBudgetsClick,
        onExpensesClick = { },
        onReportsClick = onReportsClick
    )
}

@Composable
fun FilterDropdown(label: String, modifier: Modifier, onClick: () -> Unit = {}) {
    Surface(
        modifier = modifier.height(36.dp).clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.Black),
        color = Color(0xFFBBDEFB).copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.Black, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun ExpenseHeaderCell(text: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .border(0.5.dp, Color.Black)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun ExpenseTableCell(text: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .border(0.5.dp, Color.Black)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.Black, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}
