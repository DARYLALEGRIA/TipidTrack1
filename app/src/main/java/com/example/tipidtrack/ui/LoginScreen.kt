package com.example.tipidtrack.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tipidtrack.model.User
import com.example.tipidtrack.model.UserRole
import kotlinx.coroutines.delay
import java.util.UUID

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onRegisterClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Safety: Reset loading if user changes input or if it's been too long
    LaunchedEffect(email, password) {
        isLoading = false
    }
    
    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(10000) // Re-enable after 10 seconds if nothing happens
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF4facfe), Color(0xFF00f2fe))
                )
            )
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            PiggyBankIcon(modifier = Modifier.size(60.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "TipidTrack",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Username or Email", color = Color.White) },
            modifier = Modifier.fillMaxWidth(),
            colors = registerTextFieldColors(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = Color.White) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !isLoading,
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null, tint = Color.White)
                }
            },
            colors = registerTextFieldColors()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { 
                if (email.isNotBlank() && password.isNotBlank()) {
                    isLoading = true
                    onLoginClick(email.trim(), password.trim())
                } else {
                    onLoginClick(email.trim(), password.trim()) // Triggers blank error toast
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF2D4B8E))
            } else {
                Text("Login", color = Color(0xFF2D4B8E), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Don't have an account? Register here",
            color = Color.White,
            modifier = Modifier.clickable(enabled = !isLoading) { onRegisterClick() }
        )
    }
}

enum class RegisterStep {
    DETAILS, MPIN_SETUP, TERMS
}

@Composable
fun RegisterScreen(
    existingUsers: List<User> = emptyList(),
    onRegisterComplete: (User) -> Unit,
    onBackToLogin: () -> Unit
) {
    var currentStep by remember { mutableStateOf(RegisterStep.DETAILS) }
    
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }
    var mpin by remember { mutableStateOf("") }
    var confirmMpin by remember { mutableStateOf("") }
    var agreedToTerms by remember { mutableStateOf(false) }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf(false) }
    var mpinConfirmError by remember { mutableStateOf(false) }

    val adminExists = remember(existingUsers) { existingUsers.any { it.role == UserRole.ADMIN } }

    fun validatePassword(pass: String): String? {
        if (pass.length < 8) return "Password must be at least 8 characters"
        if (!pass.any { it.isUpperCase() }) return "Add at least one uppercase letter"
        if (!pass.any { it.isDigit() }) return "Add at least one number"
        if (!pass.any { !it.isLetterOrDigit() }) return "Add at least one special character (@, #, etc.)"
        return null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF4facfe), Color(0xFF00f2fe))
                )
            )
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = when(currentStep) {
                RegisterStep.DETAILS -> "Create Account"
                RegisterStep.MPIN_SETUP -> "Setup MPIN"
                RegisterStep.TERMS -> "Terms & Conditions"
            },
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (currentStep) {
            RegisterStep.DETAILS -> {
                // Role Selection - Only display if admin does not exist yet
                if (!adminExists) {
                    Text(
                        text = "Select Role",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Student Role
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clickable { selectedRole = UserRole.STUDENT },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedRole == UserRole.STUDENT) Color.White else Color.White.copy(alpha = 0.2f),
                            border = if (selectedRole == UserRole.STUDENT) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "STUDENT",
                                    color = if (selectedRole == UserRole.STUDENT) Color(0xFF2D4B8E) else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Admin Role
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clickable { selectedRole = UserRole.ADMIN },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedRole == UserRole.ADMIN) Color.White else Color.White.copy(alpha = 0.2f),
                            border = if (selectedRole == UserRole.ADMIN) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "ADMIN",
                                    color = if (selectedRole == UserRole.ADMIN) Color(0xFF2D4B8E) else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    // Force student role if admin already exists
                    SideEffect {
                        if (selectedRole == UserRole.ADMIN) {
                            selectedRole = UserRole.STUDENT
                        }
                    }
                }

                if (selectedRole != UserRole.ADMIN) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name", color = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = registerTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Username", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = registerTextFieldColors()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (selectedRole != UserRole.ADMIN) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number", color = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = registerTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Column {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            passwordError = null
                        },
                        label = { Text("Password", color = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = passwordError != null,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null, tint = Color.White)
                            }
                        },
                        colors = registerTextFieldColors()
                    )
                    passwordError?.let {
                        Text(text = it, color = Color.Yellow, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { 
                        confirmPassword = it
                        confirmPasswordError = false 
                    },
                    label = { Text("Confirm Password", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = confirmPasswordError,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(imageVector = image, contentDescription = null, tint = Color.White)
                        }
                    },
                    colors = registerTextFieldColors(confirmPasswordError),
                    supportingText = {
                        if (confirmPasswordError) {
                            Text("Passwords do not match!")
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { 
                        val error = validatePassword(password)
                        if (error != null) {
                            passwordError = error
                        } else if (password != confirmPassword) {
                            confirmPasswordError = true
                        } else {
                            if (selectedRole == UserRole.ADMIN) {
                                if (email.isNotBlank()) {
                                    currentStep = RegisterStep.TERMS
                                }
                            } else {
                                if (name.isNotBlank() && email.isNotBlank() && phone.isNotBlank()) {
                                    currentStep = RegisterStep.MPIN_SETUP
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Next", color = Color(0xFF2D4B8E), fontWeight = FontWeight.Bold)
                }
            }
            
            RegisterStep.MPIN_SETUP -> {
                MpinSetupStep(
                    mpin = mpin,
                    confirmMpin = confirmMpin,
                    onMpinChange = { mpin = it },
                    onConfirmMpinChange = { confirmMpin = it },
                    onNext = { 
                        if (mpin == confirmMpin && mpin.length == 4) {
                            currentStep = RegisterStep.TERMS
                        } else {
                            mpinConfirmError = true
                        }
                    },
                    error = mpinConfirmError
                )
            }
            
            RegisterStep.TERMS -> {
                TermsStep(
                    agreed = agreedToTerms,
                    onAgreedChange = { agreedToTerms = it },
                    onComplete = {
                        if (agreedToTerms) {
                            val newUser = User(
                                id = UUID.randomUUID().toString(),
                                name = if (selectedRole == UserRole.ADMIN) "Administrator" else name.trim(),
                                email = email.trim(),
                                phone = if (selectedRole == UserRole.ADMIN) "" else phone.trim(),
                                password = password,
                                role = selectedRole,
                                mpin = if (selectedRole == UserRole.ADMIN) "" else mpin
                            )
                            onRegisterComplete(newUser)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBackToLogin) {
            Text("Back to Login", color = Color.White)
        }
    }
}

@Composable
fun MpinSetupStep(
    mpin: String,
    confirmMpin: String,
    onMpinChange: (String) -> Unit,
    onConfirmMpinChange: (String) -> Unit,
    onNext: () -> Unit,
    error: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = mpin,
            onValueChange = { if (it.length <= 4) onMpinChange(it) },
            label = { Text("Enter 4-digit MPIN", color = Color.White) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            colors = registerTextFieldColors()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = confirmMpin,
            onValueChange = { if (it.length <= 4) onConfirmMpinChange(it) },
            label = { Text("Confirm MPIN", color = Color.White) },
            modifier = Modifier.fillMaxWidth(),
            isError = error,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            colors = registerTextFieldColors(error)
        )
        if (error) {
            Text("MPINs do not match", color = Color.Yellow, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                if (mpin == confirmMpin && mpin.length == 4) {
                    onNext()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Continue", color = Color(0xFF2D4B8E), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TermsStep(
    agreed: Boolean,
    onAgreedChange: (Boolean) -> Unit,
    onComplete: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF))
        ) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = """
                        Welcome to TipidTrack! By using this application, you agree to the following terms and conditions:

                        1. Data Privacy: We value your privacy. All your financial data (expenses, budgets, and goals) are stored securely in our database. We do not share your personal information with third parties.

                        2. User Responsibility: You are responsible for maintaining the confidentiality of your account credentials, including your password and MPIN.

                        3. Accuracy of Data: TipidTrack is a tool to help you manage your finances. While we strive for accuracy, the responsibility for the data entered lies with the user.

                        4. Usage: This app is intended for personal financial tracking and educational purposes.

                        5. Modifications: We reserve the right to modify these terms at any time. Continued use of the app constitutes acceptance of the updated terms.

                        Thank you for choosing TipidTrack to help you stay on track with your savings!
                    """.trimIndent(),
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = agreed,
                onCheckedChange = onAgreedChange,
                colors = CheckboxDefaults.colors(uncheckedColor = Color.White, checkedColor = Color.White, checkmarkColor = Color(0xFF2D4B8E))
            )
            Text("I agree to the Terms and Conditions", color = Color.White)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onComplete,
            enabled = agreed,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, disabledContainerColor = Color.LightGray),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Complete Registration", color = Color(0xFF2D4B8E), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun registerTextFieldColors(isError: Boolean = false) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.White,
    unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
    focusedLabelColor = Color.White,
    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
    cursorColor = Color.White,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    errorBorderColor = Color.Yellow,
    errorLabelColor = Color.Yellow,
    errorSupportingTextColor = Color.Yellow
)

@Composable
fun MPINScreen(
    userName: String,
    onMpinComplete: (String) -> Unit
) {
    var mpin by remember { mutableStateOf("") }
    
    LaunchedEffect(mpin) {
        if (mpin.length == 4) {
            delay(1000)
            mpin = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF4facfe), Color(0xFF00f2fe))
                )
            )
    ) {
        Box(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        PiggyBankIcon(modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "TipidTrack",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Good Day ka-Tipid!",
                    color = Color.White,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName,
                        color = Color.White,
                        fontSize = 24.sp,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = "Enter your MPIN",
                    color = Color.White,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(4) { index ->
                        val isFilled = index < mpin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .border(2.dp, Color.White, CircleShape)
                                .background(if (isFilled) Color.White else Color.Transparent, CircleShape)
                        )
                    }
                }
            }
            
            Text(
                text = "Never share your MPIN or OTP with anyone",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5))
        ) {
            NumericKeypad(
                onNumberClick = { num ->
                    if (mpin.length < 4) {
                        mpin += num
                        if (mpin.length == 4) {
                            onMpinComplete(mpin)
                        }
                    }
                },
                onDeleteClick = {
                    if (mpin.isNotEmpty()) {
                        mpin = mpin.dropLast(1)
                    }
                }
            )
        }
    }
}

@Composable
fun NumericKeypad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "backspace")
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { digit ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clickable(enabled = digit.isNotEmpty()) {
                                if (digit == "backspace") onDeleteClick()
                                else if (digit.isNotEmpty()) onNumberClick(digit)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (digit == "backspace") {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Delete",
                                tint = Color(0xFF2D4B8E),
                                modifier = Modifier.size(32.dp)
                            )
                        } else if (digit.isNotEmpty()) {
                            Text(
                                text = digit,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D4B8E)
                            )
                        }
                    }
                }
            }
        }
    }
}
