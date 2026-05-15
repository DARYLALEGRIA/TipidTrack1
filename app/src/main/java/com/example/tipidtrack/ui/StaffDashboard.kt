package com.example.tipidtrack.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tipidtrack.model.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StaffDashboard(
    user: User?,
    allExpenses: List<ExpenseItem>,
    allBudgets: List<BudgetItem>,
    allUsers: List<User> = emptyList(),
    onLogout: () -> Unit,
    onUpdateProfileImage: (Uri) -> Unit
) {
    var showAccountDetailsDialog by remember { mutableStateOf(false) }
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PH"))

    fun parseAmount(amountStr: String?): Double {
        if (amountStr == null) return 0.0
        return amountStr.replace("₱", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
    }

    val students = allUsers.filter { it.role == UserRole.STUDENT }
    val studentIds = students.map { it.id }.toSet()

    val studentExpenses = if (studentIds.isNotEmpty()) {
        allExpenses.filter { it.userId in studentIds }
    } else {
        allExpenses.filter { it.userId != user?.id }
    }

    val totalSpending = studentExpenses.sumOf { parseAmount(it.amount) }
    val activeStudentIds = studentExpenses.mapNotNull { it.userId }.distinct()
    val uniqueStudentsCount = if (students.isNotEmpty()) students.size else activeStudentIds.count().coerceAtLeast(1)
    val averageSpending = if (uniqueStudentsCount > 0) totalSpending / uniqueStudentsCount else 0.0

    val categoryDistribution = studentExpenses.groupBy { it.category?.trim() ?: "Other" }
        .mapValues { entry -> entry.value.sumOf { parseAmount(it.amount) } }
        .toList()
        .sortedByDescending { it.second }

    val monthlyTrends = studentExpenses.groupBy { 
        try {
            val dateStr = it.date ?: ""
            if (dateStr.isEmpty()) "Unknown"
            else {
                val date = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).parse(dateStr)
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date!!)
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }.mapValues { entry -> entry.value.sumOf { parseAmount(it.amount) } }
    .toList()
    .sortedByDescending { it.first }

    val anonymousBreakdown = if (students.isNotEmpty()) {
        students.map { s ->
            allExpenses.filter { it.userId == s.id }.sumOf { parseAmount(it.amount) }
        }
    } else {
        activeStudentIds.map { id ->
            allExpenses.filter { it.userId == id }.sumOf { parseAmount(it.amount) }
        }
    }.sortedByDescending { it }
     .mapIndexed { index, spent -> "Student ${index + 1}" to spent }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE), Color(0xFFE0F7FA))
                )
            )
    ) {
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
                    text = "TipidTrack Staff",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                Icons.Default.Person, 
                contentDescription = "Account", 
                tint = Color.White,
                modifier = Modifier.size(32.dp).clickable { showAccountDetailsDialog = true }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Student Financial Insights",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Aggregated Student Overview", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1976D2))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Student Spending:", color = Color.Black)
                        Text(currencyFormatter.format(totalSpending), fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Average Spending per Student:", color = Color.Black)
                        Text(currencyFormatter.format(averageSpending), fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tracked Individuals:", color = Color.Black)
                        Text("${if (students.isNotEmpty()) students.size else activeStudentIds.size}", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Anonymous Spending Habits",
                color = Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            if (anonymousBreakdown.isEmpty()) {
                Text("No spending data recorded yet.", color = Color.DarkGray, fontSize = 14.sp)
            } else {
                anonymousBreakdown.forEach { (label, amount) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = label, fontWeight = FontWeight.Medium, color = Color.Black)
                            Text(text = currencyFormatter.format(amount), fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Top Spending Categories",
                color = Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            categoryDistribution.take(5).forEach { (category, amount) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2).copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = category, fontWeight = FontWeight.Medium, color = Color.Black)
                        Text(text = currencyFormatter.format(amount), fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Monthly Spending Trends",
                color = Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            monthlyTrends.forEach { (month, amount) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = month, fontSize = 16.sp, color = Color.Black)
                    LinearProgressIndicator(
                        progress = { if (totalSpending > 0) (amount / totalSpending).toFloat() else 0f },
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp).height(8.dp),
                        color = Color(0xFF1976D2),
                        trackColor = Color.Black.copy(alpha = 0.1f)
                    )
                    Text(text = currencyFormatter.format(amount), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Adviser Insight", fontWeight = FontWeight.Bold, color = Color(0xFFF57C00))
                    Text(
                        text = if (categoryDistribution.isNotEmpty()) {
                            "Students are spending most on ${categoryDistribution.first().first}. Consider organizing a workshop on managing these specific costs."
                        } else {
                            "Track student expenses to see insights here."
                        },
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
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
}
