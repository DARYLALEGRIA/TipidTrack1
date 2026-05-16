package com.example.tipidtrack.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.tipidtrack.model.User
import com.example.tipidtrack.model.UserRole

@Composable
fun AdminDashboard(
    user: User?,
    allUsers: List<User>,
    onDeleteUser: (User) -> Unit,
    onUpdateUser: (User) -> Unit,
    onAddUser: (String, String, UserRole) -> Unit,
    onLogout: () -> Unit,
    onUpdateProfileImage: (Uri) -> Unit
) {
    var showAccountDetailsDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<User?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = allUsers.filter {
        it.name?.contains(searchQuery, ignoreCase = true) == true ||
        it.email?.contains(searchQuery, ignoreCase = true) == true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFFBBDEFB))
                )
            )
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFF0D47A1), modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "TipidTrack Admin",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            IconButton(onClick = { showAccountDetailsDialog = true }) {
                Icon(
                    Icons.Default.AccountCircle, 
                    contentDescription = "Account", 
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Account Management",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name or email...", color = Color.White.copy(alpha = 0.7f)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All Users (${filteredUsers.size})",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showAddUserDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add User", color = Color(0xFF1976D2), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredUsers) { targetUser ->
                    UserAdminCard(
                        user = targetUser,
                        onDelete = { onDeleteUser(targetUser) },
                        onEdit = { userToEdit = targetUser }
                    )
                }
            }
        }
    }

    if (showAddUserDialog) {
        UserDialog(
            title = "Add New Account",
            onDismiss = { showAddUserDialog = false },
            onConfirm = { name, email, role ->
                onAddUser(name, email, role)
                showAddUserDialog = false
            }
        )
    }

    userToEdit?.let { target ->
        UserDialog(
            title = "Update Account",
            initialName = target.name ?: "",
            initialEmail = target.email ?: "",
            initialRole = target.role,
            onDismiss = { userToEdit = null },
            onConfirm = { name, email, role ->
                onUpdateUser(target.copy(name = name, email = email, role = role))
                userToEdit = null
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
}

@Composable
fun UserDialog(
    title: String,
    initialName: String = "",
    initialEmail: String = "",
    initialRole: UserRole = UserRole.STUDENT,
    onDismiss: () -> Unit,
    onConfirm: (String, String, UserRole) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail) }
    var selectedRole by remember { mutableStateOf(initialRole) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A237E))
                Spacer(modifier = Modifier.height(20.dp))
                
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Full Name") }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = email, 
                    onValueChange = { email = it }, 
                    label = { Text("Email/Username") }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                Text("Select Account Role", fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    UserRole.values().forEach { role ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRole = role }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = (selectedRole == role), onClick = { selectedRole = role })
                            Text(text = role.name, fontSize = 16.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { onConfirm(name, email, selectedRole) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("SAVE CHANGES", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("CANCEL", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun UserAdminCard(
    user: User,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High-contrast Avatar
            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = when(user.role) {
                    UserRole.ADMIN -> Color(0xFFFFEBEE)
                    UserRole.STAFF -> Color(0xFFE3F2FD)
                    UserRole.STUDENT -> Color(0xFFE8F5E9)
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (user.name?.take(1) ?: "U").uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = when(user.role) {
                            UserRole.ADMIN -> Color.Red
                            UserRole.STAFF -> Color(0xFF1976D2)
                            UserRole.STUDENT -> Color(0xFF2E7D32)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name ?: "Unnamed User", 
                    fontWeight = FontWeight.ExtraBold, 
                    fontSize = 20.sp,
                    color = Color.Black // Pure black for maximum contrast
                )
                Text(
                    text = user.email ?: "No email provided", 
                    fontSize = 15.sp, 
                    color = Color(0xFF424242) // Dark grey
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                // Role Badge
                Surface(
                    color = when(user.role) {
                        UserRole.ADMIN -> Color.Red
                        UserRole.STAFF -> Color(0xFF1976D2)
                        UserRole.STUDENT -> Color(0xFF2E7D32)
                    }.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = " ${user.role} ", 
                        fontSize = 13.sp, 
                        color = when(user.role) {
                            UserRole.ADMIN -> Color.Red
                            UserRole.STAFF -> Color(0xFF1976D2)
                            UserRole.STUDENT -> Color(0xFF2E7D32)
                        },
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF1976D2), modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
