package com.example.tipidtrack.ui

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tipidtrack.model.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    expenses: List<ExpenseItem>,
    budgets: List<BudgetItem>,
    onHomeClick: () -> Unit = {},
    onExpensesClick: () -> Unit = {},
    onBudgetsClick: () -> Unit = {},
    onUpdateProfileImage: (Uri) -> Unit = {},
    onLogout: () -> Unit = {},
    user: User? = null,
    selectedCycle: CycleManager.CycleRange? = null,
    availableCycles: List<CycleManager.CycleRange> = emptyList(),
    onCycleSelected: (CycleManager.CycleRange) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    unreadNotificationsCount: Int = 0
) {
    var showAccountDetailsDialog by remember { mutableStateOf(false) }

    fun parseAmount(amountStr: String): Double {
        return amountStr.replace("₱", "").replace(",", "").toDoubleOrNull() ?: 0.0
    }

    val categoryData = remember(expenses) {
        expenses.groupBy { (it.category ?: "").uppercase() }
            .mapValues { entry -> entry.value.sumOf { parseAmount(it.amount ?: "") } }
    }

    val totalSpent = categoryData.values.sum()

    // Weekly and Daily Spending Logic
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("MM/dd/yy", Locale.getDefault())

    val dailySpending = remember(expenses) {
        val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val map = days.associateWith { 0.0 }.toMutableMap()
        
        expenses.forEach { expense ->
            try {
                val date = dateFormat.parse(expense.date ?: "")
                if (date != null) {
                    calendar.time = date
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    val dayName = days[dayOfWeek - 1]
                    map[dayName] = (map[dayName] ?: 0.0) + parseAmount(expense.amount ?: "")
                }
            } catch (e: Exception) {}
        }
        map.toList()
    }

    val weeklySpending = remember(expenses) {
        val map = mutableMapOf<Int, Double>()
        expenses.forEach { expense ->
            try {
                val date = dateFormat.parse(expense.date ?: "")
                if (date != null) {
                    calendar.time = date
                    val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
                    map[weekOfYear] = (map[weekOfYear] ?: 0.0) + parseAmount(expense.amount ?: "")
                }
            } catch (e: Exception) {}
        }
        // Sort by week number and take last 3 weeks
        map.toList()
            .sortedBy { it.first }
            .takeLast(3)
            .mapIndexed { index, pair -> "WEEK ${index + 1}" to pair.second }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "REPORTS",
                color = Color.Black,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                textAlign = TextAlign.Start
            )

            // Cycle Selector
            CycleSelector(
                selectedCycle = selectedCycle,
                availableCycles = availableCycles,
                onCycleSelected = onCycleSelected
            )

            if (expenses.isEmpty()) {
                Text(
                    "No data available for reports.",
                    modifier = Modifier.padding(top = 50.dp),
                    color = Color.Gray
                )
            } else {
                Text(
                    text = "EXPENSE DISTRIBUTION",
                    color = Color(0xFF444444),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pie Chart
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = -90f
                        val colors = listOf(Color(0xFF4682B4), Color(0xFF87CEEB), Color(0xFFB0C4DE), Color(0xFF5DADE2), Color(0xFF2E86C1))
                        
                        categoryData.entries.forEachIndexed { index, entry ->
                            val sweepAngle = (entry.value / totalSpent).toFloat() * 360f
                            drawArc(
                                color = colors[index % colors.size],
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true
                            )
                            startAngle += sweepAngle
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Legend
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val colors = listOf(Color(0xFF4682B4), Color(0xFF87CEEB), Color(0xFFB0C4DE), Color(0xFF5DADE2), Color(0xFF2E86C1))
                    categoryData.entries.forEachIndexed { index, entry ->
                        val percentage = (entry.value / totalSpent * 100).toInt()
                        LegendItem(colors[index % colors.size], "${entry.key} ($percentage%)")
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = "WEEKLY SPENDING",
                    color = Color(0xFF444444),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Horizontal Weekly Spending Chart
                HorizontalWeeklySpendingChart(data = weeklySpending)

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = "DAILY SPENDING (THIS WEEK)",
                    color = Color(0xFF444444),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3D-ish Line Chart
                ThreeDLineChart(data = dailySpending)
            }
            
            Spacer(modifier = Modifier.height(100.dp))
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

    // Fixed Bottom Navigation Bar
    TipidTrackBottomNavigation(
        currentScreen = "REPORTS",
        onHomeClick = onHomeClick,
        onBudgetsClick = onBudgetsClick,
        onExpensesClick = onExpensesClick,
        onReportsClick = { }
    )
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 12.sp, color = Color.DarkGray)
    }
}

@Composable
fun HorizontalWeeklySpendingChart(data: List<Pair<String, Double>>) {
    val maxSpending = data.maxOfOrNull { it.second } ?: 1.0
    Column(modifier = Modifier.fillMaxWidth()) {
        data.forEach { (week, amount) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = week, modifier = Modifier.width(60.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((amount / maxSpending).toFloat().coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(listOf(Color(0xFF4FACFE), Color(0xFF00F2FE))),
                                shape = RoundedCornerShape(12.dp)
                            )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "₱${String.format(Locale.getDefault(), "%.2f", amount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ThreeDLineChart(data: List<Pair<String, Double>>) {
    val maxSpending = data.maxOfOrNull { it.second } ?: 1.0
    val points = data.map { it.second }
    
    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp).padding(16.dp)) {
            val width = size.width
            val height = size.height
            val spacing = width / (points.size - 1).coerceAtLeast(1)
            
            val path = Path()
            points.forEachIndexed { index, value ->
                val x = index * spacing
                val y = height - (value / maxSpending).toFloat() * height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            
            drawPath(
                path = path,
                color = Color(0xFF3F51B5),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            
            points.forEachIndexed { index, value ->
                val x = index * spacing
                val y = height - (value / maxSpending).toFloat() * height
                drawCircle(Color(0xFF3F51B5), radius = 6.dp.toPx(), center = Offset(x, y))
                drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(x, y))
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            data.forEach { (day, _) ->
                Text(text = day, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}
