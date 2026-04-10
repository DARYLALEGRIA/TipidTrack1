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
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Bottom Navigation Bar
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
                BottomNavItem("HOME", Icons.Default.Home, false, onClick = onHomeClick)
                BottomNavItem("BUDGETS", Icons.Default.AccountBalanceWallet, false, onClick = onBudgetsClick)
                BottomNavItem("EXPENSES", Icons.AutoMirrored.Filled.List, false, onClick = onExpensesClick)
                BottomNavItem("REPORTS", Icons.Default.Assessment, true, onClick = {})
            }
        }
    }
}

@Composable
fun HorizontalWeeklySpendingChart(data: List<Pair<String, Double>>) {
    val maxVal = (data.maxByOrNull { it.second }?.second ?: 1.0).toDouble().coerceAtLeast(1000.0)
    val chartMax = if (maxVal > 1000) (Math.ceil(maxVal / 100.0) * 100.0).toFloat() else 1000f
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val leftPadding = 120f
            val bottomPadding = 60f
            val chartWidth = canvasWidth - leftPadding - 40f
            val chartHeight = canvasHeight - bottomPadding
            
            val barHeight = 40f
            val spacing = (chartHeight - (data.size * barHeight)) / (data.size + 1)

            // Draw X-axis labels and vertical grid lines
            val steps = 10
            for (i in 0..steps) {
                val x = leftPadding + (i * (chartWidth / steps))
                val value = (i * (chartMax / steps)).toInt()
                
                // Grid line
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(x, 0f),
                    end = Offset(x, chartHeight),
                    strokeWidth = 1f
                )
                
                // Label
                val textResult = textMeasurer.measure(
                    text = AnnotatedString(value.toString()),
                    style = TextStyle(fontSize = 10.sp, color = Color(0xFF555555))
                )
                drawText(
                    textLayoutResult = textResult,
                    topLeft = Offset(x - textResult.size.width / 2, chartHeight + 10f)
                )
            }

            // Draw bars and Y-axis labels
            data.forEachIndexed { index, pair ->
                val y = spacing + index * (barHeight + spacing)
                val barWidth = (pair.second / chartMax * chartWidth).toFloat()
                
                // Label (WEEK 1, etc.)
                val labelResult = textMeasurer.measure(
                    text = AnnotatedString(pair.first),
                    style = TextStyle(fontSize = 12.sp, color = Color(0xFF555555), fontWeight = FontWeight.Bold)
                )
                drawText(
                    textLayoutResult = labelResult,
                    topLeft = Offset(leftPadding - labelResult.size.width - 20f, y + (barHeight - labelResult.size.height) / 2)
                )
                
                // Bar with rounded corners
                drawRoundRect(
                    color = Color(0xFF6DAEDC),
                    topLeft = Offset(leftPadding, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(14.dp).background(Color(0xFF6DAEDC)))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "WEEKLY SPENDING", 
                fontSize = 14.sp, 
                color = Color(0xFF555555).copy(alpha = 0.8f), 
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ThreeDLineChart(data: List<Pair<String, Double>>) {
    val maxVal = (data.maxByOrNull { it.second }?.second ?: 1.0).toFloat().coerceAtLeast(1f)
    val textMeasurer = rememberTextMeasurer()
    
    Canvas(modifier = Modifier.fillMaxWidth().height(240.dp).padding(horizontal = 24.dp, vertical = 40.dp)) {
        val canvasWidth = size.width
        val canvasHeight = size.height - 40f
        val pointSpacing = canvasWidth / (data.size - 1)
        
        val points = data.mapIndexed { index, pair ->
            Offset(index * pointSpacing, canvasHeight - (pair.second / maxVal * canvasHeight).toFloat())
        }
        
        val offset3d = 10f
        
        // Draw 3D Shadow/Depth Area
        val shadowPath = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x + offset3d, points.last().y + offset3d)
            for (i in points.size - 1 downTo 0) {
                lineTo(points[i].x + offset3d, points[i].y + offset3d)
            }
            close()
        }
        drawPath(shadowPath, color = Color.Gray.copy(alpha = 0.2f))

        // Draw Line with 3D offset effect
        for (i in 0 until points.size - 1) {
            // Shadow line
            drawLine(
                color = Color.DarkGray.copy(alpha = 0.3f),
                start = points[i] + Offset(offset3d, offset3d),
                end = points[i+1] + Offset(offset3d, offset3d),
                strokeWidth = 4f
            )
            // Main line
            drawLine(
                color = Color(0xFF3F51B5),
                start = points[i],
                end = points[i+1],
                strokeWidth = 6f
            )
        }
        
        // Draw Points, Values and Labels
        points.forEachIndexed { index, point ->
            drawCircle(Color(0xFF3F51B5), radius = 6f, center = point)
            drawCircle(Color.White, radius = 3f, center = point)

            // Draw Value (Number)
            val valueText = "₱${data[index].second.toInt()}"
            val valueResult = textMeasurer.measure(
                text = AnnotatedString(valueText),
                style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            )
            drawText(
                textLayoutResult = valueResult,
                topLeft = Offset(point.x - valueResult.size.width / 2, point.y - 25f)
            )

            // Draw Label (Day)
            val labelResult = textMeasurer.measure(
                text = AnnotatedString(data[index].first),
                style = TextStyle(fontSize = 10.sp, color = Color.DarkGray)
            )
            drawText(
                textLayoutResult = labelResult,
                topLeft = Offset(point.x - labelResult.size.width / 2, canvasHeight + 10f)
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Box(modifier = Modifier.size(12.dp).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, fontSize = 12.sp, color = Color.DarkGray)
    }
}

@Composable
fun BarItem(label: String, progress: Float, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(text = value, fontSize = 10.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(12.dp),
            color = if (progress > 0.9f) Color.Red else Color(0xFF5DADE2),
            trackColor = Color.LightGray.copy(alpha = 0.5f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
