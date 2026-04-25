package com.example.tipidtrack.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import java.text.SimpleDateFormat
import java.util.*

enum class UserRole {
    STUDENT, STAFF, ADMIN
}

data class Goal(
    val title: String? = "",
    val subtitle: String? = "",
    val targetAmount: Double? = 0.0,
    val currentAmount: Double? = 0.0,
    val icon: String? = "🎯",
    val targetDate: String? = null,
    val createdAt: String? = "",
    val userId: String? = null
)

data class ExpenseItem(
    val date: String? = null,
    val category: String? = "",
    val amount: String? = "₱0.0",
    val notes: String? = "",
    val userId: String? = null // Added at the end
)

data class BudgetItem(
    val category: String? = "",
    val budget: String? = "₱0.0",
    val spent: String? = "₱0.0",
    val date: String? = "",
    val userId: String? = null // Added at the end
)

data class User(
    val name: String? = "",
    val email: String? = "",
    val phone: String? = "",
    val password: String? = "",
    val mpin: String? = "",
    val profileImageUri: String? = null,
    val cycleStartDate: String? = null,
    val role: UserRole = UserRole.STUDENT,
    val totalAllowance: Double = 0.0,
    val id: String = "" // Default to empty, will be set to Firebase UID
)

enum class NotificationType {
    OVERSPENDING, WARNING, BUDGET_REACHED, SAVINGS, GENERAL
}

data class NotificationItem(
    val id: String? = UUID.randomUUID().toString(),
    val title: String? = "Notification",
    val message: String? = "",
    val category: String? = "General",
    val type: NotificationType? = NotificationType.GENERAL,
    val timestamp: Long? = System.currentTimeMillis(),
    val isRead: Boolean? = false,
    val userId: String? = null
)

data class ReportItem(
    val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val cycleRange: String? = "",
    val totalSpent: Double = 0.0,
    val categoryBreakdown: Map<String, Double> = emptyMap(),
    val generatedAt: Long = System.currentTimeMillis(),
    val notes: String? = ""
)

@Composable
fun BottomNavItem(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF1976D2) else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isSelected) Color(0xFF1976D2) else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun TipidTrackBottomNavigation(
    currentScreen: String,
    onHomeClick: () -> Unit,
    onBudgetsClick: () -> Unit,
    onExpensesClick: () -> Unit,
    onReportsClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BottomNavItem("HOME", Icons.Default.Home, currentScreen == "HOME", onClick = onHomeClick)
                BottomNavItem("BUDGETS", Icons.Default.AccountBalanceWallet, currentScreen == "BUDGETS", onClick = onBudgetsClick)
                BottomNavItem("EXPENSES", Icons.AutoMirrored.Filled.List, currentScreen == "EXPENSES", onClick = onExpensesClick)
                BottomNavItem("REPORTS", Icons.Default.Assessment, currentScreen == "REPORTS", onClick = onReportsClick)
            }
        }
    }
}

@Composable
fun CycleSelector(
    selectedCycle: CycleManager.CycleRange?,
    availableCycles: List<CycleManager.CycleRange>,
    onCycleSelected: (CycleManager.CycleRange) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(vertical = 8.dp)) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Viewing Period", fontSize = 12.sp, color = Color.DarkGray)
                    Text(
                        text = selectedCycle?.let { CycleManager.formatCycle(it) } ?: "Select Period",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            availableCycles.forEach { range ->
                DropdownMenuItem(
                    text = { Text(CycleManager.formatCycle(range)) },
                    onClick = {
                        onCycleSelected(range)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SharedExpenseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(8.dp),
        readOnly = readOnly,
        trailingIcon = trailingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF3F51B5),
            unfocusedBorderColor = Color.Gray
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Initialize with today's date if selectedDate is empty
    val initialMillis = if (selectedDate.isNotEmpty()) {
        try {
            val formatter = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            formatter.parse(selectedDate)?.time
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    } else {
        System.currentTimeMillis()
    }
    
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    Box(modifier = Modifier.fillMaxWidth()) {
        SharedExpenseTextField(
            value = selectedDate,
            onValueChange = { },
            label = label,
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                }
            }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDatePicker = true }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis ?: initialMillis
                    val date = Date(millis ?: System.currentTimeMillis())
                    val formatter = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
                    formatter.timeZone = TimeZone.getTimeZone("UTC")
                    onDateSelected(formatter.format(date))
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

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
                    text = "ADD EXPENSE",
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
                Spacer(modifier = Modifier.height(16.dp))
                SharedExpenseTextField(
                    value = notesText, 
                    onValueChange = { notesText = it }, 
                    label = "Notes"
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onAdd(amountText, categoryText, notesText) },
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
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onSavingsClick: () -> Unit,
    onItemClick: () -> Unit
) {
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
                    text = "CHOOSE GOAL",
                    color = Color.Black,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onSavingsClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBBDEFB)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SAVINGS GOAL", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onItemClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBBDEFB)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ITEM GOAL", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddSavingsGoalDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }

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
                    text = "SAVINGS GOAL",
                    color = Color.Black,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                SharedExpenseTextField(
                    value = amountText, 
                    onValueChange = { amountText = it }, 
                    label = "Target Amount", 
                    keyboardType = KeyboardType.Number
                )
                Spacer(modifier = Modifier.height(16.dp))
                DatePickerField(
                    label = "Target Date",
                    selectedDate = dateText,
                    onDateSelected = { dateText = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onAdd(amountText, dateText) },
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
fun AddItemGoalDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var itemText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }

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
                    text = "ITEM GOAL",
                    color = Color.Black,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                SharedExpenseTextField(
                    value = itemText, 
                    onValueChange = { itemText = it }, 
                    label = "What do you want to buy?"
                )
                Spacer(modifier = Modifier.height(16.dp))
                SharedExpenseTextField(
                    value = amountText, 
                    onValueChange = { amountText = it }, 
                    label = "Price", 
                    keyboardType = KeyboardType.Number
                )
                Spacer(modifier = Modifier.height(16.dp))
                DatePickerField(
                    label = "Target Date",
                    selectedDate = dateText,
                    onDateSelected = { dateText = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onAdd(itemText, amountText, dateText) },
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
fun AccountDetailsDialog(
    user: User?,
    onDismiss: () -> Unit,
    onLogout: () -> Unit = {},
    onImageSelected: (Uri) -> Unit = {}
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }

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
                    text = "Account Details",
                    color = Color.Black,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.size(100.dp)
                ) {
                    if (user?.profileImageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(user.profileImageUri),
                            contentDescription = "Profile Image",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color.White, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.Gray)
                        }
                    }
                    
                    SmallFloatingActionButton(
                        onClick = { launcher.launch("image/*") },
                        containerColor = Color(0xFF3F51B5),
                        contentColor = Color.White,
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Change Image", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = user?.name ?: "User Name", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(text = user?.email ?: "user@email.com", fontSize = 14.sp, color = Color.DarkGray)
                Text(text = user?.phone ?: "09XXXXXXXXX", fontSize = 14.sp, color = Color.DarkGray)
                Text(text = "Role: ${user?.role ?: UserRole.STUDENT}", fontSize = 14.sp, color = Color(0xFF3F51B5), fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Logout", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close", color = Color(0xFF3F51B5))
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationScreen(
    notifications: List<NotificationItem>,
    onBackClick: () -> Unit,
    onClearAllClick: () -> Unit,
    onNotificationClick: (NotificationItem) -> Unit
) {
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Notifications",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClearAllClick) {
                Text("Clear All", color = Color.White)
            }
        }

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No notifications yet.", color = Color.DarkGray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Defensive filtering and sorting. Removed key for stability.
                val sortedNotifications = notifications
                    .filterNotNull()
                    .sortedByDescending { it.timestamp ?: 0L }
                
                items(sortedNotifications) { notification ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically()
                    ) {
                        NotificationCard(notification = notification, onClick = { onNotificationClick(notification) })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun NotificationCard(notification: NotificationItem, onClick: () -> Unit) {
    val sdf = SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault())
    
    // Triple-safe access for runtime nulls from Gson
    val nType = notification.type ?: NotificationType.GENERAL
    val nTitle = notification.title ?: "Notice"
    val nMessage = notification.message ?: ""
    val isRead = notification.isRead ?: false
    val nTimestamp = notification.timestamp ?: System.currentTimeMillis()
    
    val (primaryColor, containerColor, icon) = when (nType) {
        NotificationType.OVERSPENDING -> Triple(Color(0xFFD32F2F), Color(0xFFFFEBEE), Icons.Default.Error)
        NotificationType.WARNING -> Triple(Color(0xFFF57C00), Color(0xFFFFF3E0), Icons.Default.Warning)
        NotificationType.BUDGET_REACHED -> Triple(Color(0xFF1976D2), Color(0xFFE3F2FD), Icons.Default.Info)
        NotificationType.SAVINGS -> Triple(Color(0xFF388E3C), Color(0xFFE8F5E9), Icons.Default.Done)
        NotificationType.GENERAL -> Triple(Color(0xFF616161), Color(0xFFF5F5F5), Icons.Default.Notifications)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .alpha(if (isRead) 0.7f else 1f),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRead) 1.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = primaryColor.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = nTitle,
                        fontWeight = if (isRead) FontWeight.SemiBold else FontWeight.Bold,
                        fontSize = 16.sp,
                        color = primaryColor
                    )
                    if (!isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(primaryColor, CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = nMessage, 
                    fontSize = 14.sp, 
                    color = Color.DarkGray.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = sdf.format(Date(nTimestamp)),
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
