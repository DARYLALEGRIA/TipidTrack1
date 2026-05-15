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
                    colors = listOf(Color(0xFF1A237E), Color(0xFF3F51B5), Color(0xFFC5CAE9))
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
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFF1A237E), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TipidTrack Admin",
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

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "System Administration",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Users", color = Color.White) },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showAddUserDialog = true },
                    modifier = Modifier.background(Color.White, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add User", tint = Color(0xFF1A237E))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "User Management (${filteredUsers.size} users)",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(filteredUsers) { targetUser ->
                    UserAdminCard(
                        user = targetUser,
                        onDelete = { onDeleteUser(targetUser) },
                        onEdit = { userToEdit = targetUser }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showAddUserDialog) {
        UserDialog(
            title = "Create New User",
            onDismiss = { showAddUserDialog = false },
            onConfirm = { name, email, role ->
                onAddUser(name, email, role)
                showAddUserDialog = false
            }
        )
    }

    userToEdit?.let { target ->
        UserDialog(
            title = "Edit User",
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
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Text("Role:")
                UserRole.values().forEach { role ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedRole = role }) {
                        RadioButton(selected = selectedRole == role, onClick = { selectedRole = role })
                        Text(
                            text = when(role) {
                                UserRole.STUDENT -> "Student"
                                UserRole.STAFF -> "Staff/Adviser"
                                UserRole.ADMIN -> "Admin"
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onConfirm(name, email, selectedRole) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirm")
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = user.email ?: "", fontSize = 12.sp, color = Color.Gray)
                Text(
                    text = "Role: ${user.role}", 
                    fontSize = 12.sp, 
                    color = when(user.role) {
                        UserRole.ADMIN -> Color.Red
                        UserRole.STAFF -> Color(0xFF1976D2)
                        UserRole.STUDENT -> Color(0xFF2E7D32)
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF1976D2))
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}
