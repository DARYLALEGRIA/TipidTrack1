package com.example.tipidtrack.ui

import android.net.Uri
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.tipidtrack.model.*

@Composable
fun BudgetsScreen(
    budgets: List<BudgetItem>,
    hasExpenses: Boolean = false,
    onAddBudget: (BudgetItem) -> Unit = {},
    onDeleteBudget: (String) -> Unit = {},
    user: User? = null,
    onHomeClick: () -> Unit = {},
    onExpensesClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    onUpdateProfileImage: (Uri) -> Unit = {},
    onLogout: () -> Unit = {},
    selectedCycle: CycleManager.CycleRange? = null,
    availableCycles: List<CycleManager.CycleRange> = emptyList(),
    onCycleSelected: (CycleManager.CycleRange) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    unreadNotificationsCount: Int = 0
) {
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showAccountDetailsDialog by remember { mutableStateOf(false) }
    var budgetToDelete by remember { mutableStateOf<BudgetItem?>(null) }

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
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "BUDGETS",
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

            Text("(Long press to delete)", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))

            // Budget Table
            if (budgets.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Black)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    // Header
                    Row(modifier = Modifier.fillMaxWidth()) {
                        BudgetHeaderCell("CATEGORY", Modifier.weight(1f))
                        BudgetHeaderCell("BUDGET", Modifier.weight(1f))
                        if (hasExpenses) {
                            BudgetHeaderCell("SPENT", Modifier.weight(1f))
                        }
                    }
                    // Data Rows
                    budgets.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectTapGestures(onLongPress = { budgetToDelete = item })
                                }
                        ) {
                            BudgetTableCell(item.category ?: "", Modifier.weight(1f))
                            BudgetTableCell(item.budget ?: "₱0.0", Modifier.weight(1f))
                            if (hasExpenses) {
                                BudgetTableCell(item.spent ?: "₱0.0", Modifier.weight(1f))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Text(
                    text = "No budgets set yet.",
                    color = Color.DarkGray,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Add Budget Button
            Button(
                onClick = { showAddBudgetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBBDEFB)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Add Budget", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showAddBudgetDialog) {
        AddBudgetDialog(
            onDismiss = { showAddBudgetDialog = false },
            onAdd = { amount, categoryName ->
                if (amount.isNotEmpty() && categoryName.isNotEmpty()) {
                    onAddBudget(BudgetItem(category = categoryName, budget = "₱$amount"))
                }
                showAddBudgetDialog = false
            }
        )
    }

    if (budgetToDelete != null) {
        AlertDialog(
            onDismissRequest = { budgetToDelete = null },
            title = { Text("Delete Budget") },
            text = { Text("Are you sure you want to delete this budget record?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBudget(budgetToDelete!!.id)
                        budgetToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { budgetToDelete = null }) {
                    Text("Cancel")
                }
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
        currentScreen = "BUDGETS",
        onHomeClick = onHomeClick,
        onBudgetsClick = { },
        onExpensesClick = onExpensesClick,
        onReportsClick = onReportsClick
    )
}

@Composable
fun AddBudgetDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF81D4FA))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ADD BUDGET",
                    color = Color.Black,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                SharedExpenseTextField(
                    value = amountText, 
                    onValueChange = { amountText = it }, 
                    label = "Amount", 
                    keyboardType = KeyboardType.Number
                )
                Spacer(modifier = Modifier.height(16.dp))
                SharedExpenseTextField(
                    value = categoryText, 
                    onValueChange = { categoryText = it }, 
                    label = "Category"
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onAdd(amountText, categoryText) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text("ADD", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun BudgetHeaderCell(text: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .border(0.5.dp, Color.Black)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BudgetTableCell(text: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .border(0.5.dp, Color.Black)
            .padding(4.dp)
            .height(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}
