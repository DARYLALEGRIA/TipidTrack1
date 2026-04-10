package com.example.tipidtrack.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    fun parseAmount(amountStr: String): Double {
        return amountStr.replace("₱", "").replace(",", "").toDoubleOrNull() ?: 0.0
    }

    fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    // Dynamically derive categories from user-defined input in expenses
    val categories = remember(expenses) {
        val userCats = expenses.map { (it.category ?: "").trim().capitalize() }.distinct().sorted()
        listOf("All") + userCats
    }

    // Aggregation Logic: Group expenses by category and sum amounts
    val aggregatedExpenses by remember(expenses, selectedCategory, selectedDate) {
        derivedStateOf {
            expenses
                .filter { expense ->
                    selectedDate == "All" || expense.date == selectedDate
                }
                .groupBy { (it.category ?: "").trim().capitalize() }
                .mapValues { entry ->
                    entry.value.sumOf { parseAmount(it.amount ?: "") }
                }
                .filter { (category, _) ->
                    selectedCategory == "All" || category.equals(selectedCategory, ignoreCase = true)
                }
                .toList()
                .sortedByDescending { it.second }
        }
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
        ) {
            Text(
                text = "EXPENSES",
                color = Color.Black,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Cycle Selector
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

            // Expenses Table (Aggregated by Category)
            if (aggregatedExpenses.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Black)
                ) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFBBDEFB).copy(alpha = 0.5f))
                    ) {
                        ExpenseHeaderCell("CATEGORY", Modifier.weight(1f))
                        ExpenseHeaderCell("TOTAL AMOUNT", Modifier.weight(1f))
                    }
                    
                    // Table Rows
                    Column(modifier = Modifier.fillMaxWidth()) {
                        aggregatedExpenses.forEach { (category, total) ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                ExpenseTableCell(category, Modifier.weight(1f))
                                ExpenseTableCell("₱${String.format(Locale.US, "%,.2f", total)}", Modifier.weight(1f))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Text(
                    text = "No expenses recorded yet.",
                    color = Color.DarkGray,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = { showAddExpenseDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBBDEFB)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Add Expense", color = Color.Black, fontWeight = FontWeight.Bold)
            }
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
        Text(text = text, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
        Text(text = text, color = Color.Black, fontSize = 12.sp)
    }
}
