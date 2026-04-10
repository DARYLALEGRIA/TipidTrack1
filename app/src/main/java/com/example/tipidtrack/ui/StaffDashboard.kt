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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StaffDashboard(
    user: User?,
    allExpenses: List<ExpenseItem>,
    allBudgets: List<BudgetItem>,
    onLogout: () -> Unit,
    onUpdateProfileImage: (Uri) -> Unit
) {
    var showAccountDetailsDialog by remember { mutableStateOf(false) }
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PH"))

    // Aggregated Data Calculations
    val totalSpending = allExpenses.sumOf { it.amount?.replace("₱", "")?.replace(",", "")?.toDoubleOrNull() ?: 0.0 }
    val uniqueUsersCount = allExpenses.map { it.userId }.distinct().count().coerceAtLeast(1)
    val averageSpending = totalSpending / uniqueUsersCount

    val categoryDistribution = allExpenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount?.replace("₱", "")?.replace(",", "")?.toDoubleOrNull() ?: 0.0 } }
        .toList()
        .sortedByDescending { it.second }

    // Monthly Trends
    val monthlyTrends = allExpenses.groupBy { 
        try {
            val date = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).parse(it.date ?: "")
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date!!)
        } catch (e: Exception) {
            "Unknown"
        }
    }.mapValues { entry -> entry.value.sumOf { it.amount?.replace("₱", "")?.replace(",", "")?.toDoubleOrNull() ?: 0.0 } }
    .toList()
    .sortedByDescending { it.first } // Simple sort by month name/year string (not perfect but works for now)

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

            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Aggregated Overview", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1976D2))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total System Spending:")
                        Text(currencyFormatter.format(totalSpending), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Average Spending per Student:")
                        Text(currencyFormatter.format(averageSpending), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Active Tracked Students:")
                        Text("$uniqueUsersCount", fontWeight = FontWeight.Bold)
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFBBDEFB).copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = category ?: "Unknown", fontWeight = FontWeight.Medium)
                        Text(text = currencyFormatter.format(amount), fontWeight = FontWeight.Bold, color = Color(0xFF2D4B8E))
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
                    Text(text = month, fontSize = 16.sp)
                    LinearProgressIndicator(
                        progress = { (amount / totalSpending.coerceAtLeast(1.0)).toFloat() },
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp).height(8.dp),
                        color = Color(0xFF1976D2),
                        trackColor = Color.White.copy(alpha = 0.5f)
                    )
                    Text(text = currencyFormatter.format(amount), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Educational Tip
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Adviser Insight", fontWeight = FontWeight.Bold, color = Color(0xFFF57C00))
                    Text(
                        text = "Students are spending most on ${categoryDistribution.firstOrNull()?.first ?: "various categories"}. Consider organizing a workshop on managing these specific costs.",
                        fontSize = 14.sp
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
