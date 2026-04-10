package com.example.tipidtrack.ui

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
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tipidtrack.ui.theme.*

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit, // email, password
    onRegisterClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

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
        // Logo
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
            label = { Text("Email Address", color = Color.White) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = registerTextFieldColors()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = Color.White) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible)
                    Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff

                val description = if (passwordVisible) "Hide password" else "Show password"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description, tint = Color.White)
                }
            },
            colors = registerTextFieldColors()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onLoginClick(email, password) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Login", color = Color(0xFF2D4B8E), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Don't have an account? Register here",
            color = Color.White,
            modifier = Modifier.clickable { onRegisterClick() }
        )
    }
}

enum class RegisterStep {
    DETAILS, ROLE_SELECTION, MPIN_SETUP, TERMS
}

@Composable
fun RegisterScreen(
    onRegisterComplete: (User) -> Unit,
    onBackToLogin: () -> Unit
) {
    var currentStep by remember { mutableStateOf(RegisterStep.DETAILS) }
    
    // User Data State
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }
    var mpin by remember { mutableStateOf("") }
    var confirmMpin by remember { mutableStateOf("") }
    var agreedToTerms by remember { mutableStateOf(false) }

    // Visibility States
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Validation State
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf(false) }
    var mpinConfirmError by remember { mutableStateOf(false) }

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
                RegisterStep.ROLE_SELECTION -> "Select Role"
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = registerTextFieldColors()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = registerTextFieldColors()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = registerTextFieldColors()
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Password Field
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
                            val image = if (passwordVisible)
                                Icons.Filled.Visibility
                            else Icons.Filled.VisibilityOff

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
                
                // Confirm Password Field
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
                        val image = if (confirmPasswordVisible)
                            Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff

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
                        } else if (name.isNotEmpty() && email.isNotEmpty() && phone.isNotEmpty()) {
                            currentStep = RegisterStep.ROLE_SELECTION
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Next", color = Color(0xFF2D4B8E), fontWeight = FontWeight.Bold)
                }
            }
            RegisterStep.ROLE_SELECTION -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("Choose your role:", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    UserRole.values().forEach { role ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRole = role }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = selectedRole == role,
                                onClick = { selectedRole = role },
                                colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = Color.White.copy(alpha = 0.6f))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when(role) {
                                    UserRole.STUDENT -> "Student"
                                    UserRole.STAFF -> "School Staff / Adviser"
                                    UserRole.ADMIN -> "IT Administrator"
                                },
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { currentStep = RegisterStep.MPIN_SETUP },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Next", color = Color(0xFF2D4B8E), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { currentStep = RegisterStep.DETAILS }) {
                    Text("Back", color = Color.White)
                }
            }
            RegisterStep.MPIN_SETUP -> {
                OutlinedTextField(
                    value = mpin,
                    onValueChange = { if (it.length <= 4) mpin = it },
                    label = { Text("Setup 4-Digit MPIN", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = registerTextFieldColors()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = confirmMpin,
                    onValueChange = { 
                        if (it.length <= 4) {
                            confirmMpin = it
                            mpinConfirmError = false
                        }
                    },
                    label = { Text("Confirm MPIN", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = mpinConfirmError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = registerTextFieldColors(mpinConfirmError),
                    supportingText = {
                        if (mpinConfirmError) {
                            Text("MPINs do not match!")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { 
                        if (mpin.length == 4) {
                            if (mpin == confirmMpin) {
                                currentStep = RegisterStep.TERMS
                            } else {
                                mpinConfirmError = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Next", color = Color(0xFF2D4B8E), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { currentStep = RegisterStep.ROLE_SELECTION }) {
                    Text("Back", color = Color.White)
                }
            }
            RegisterStep.TERMS -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                        Text(
                            text = "Terms and Conditions\n\n" +
                                   "1. Introduction: Welcome to TipidTrack. By using our app, you agree to these terms.\n\n" +
                                   "2. Privacy: We value your privacy. Your data is stored locally and used only for budget tracking.\n\n" +
                                   "3. Security: You are responsible for maintaining the confidentiality of your MPIN.\n\n" +
                                   "4. Disclaimer: This app is for financial tracking purposes and does not provide financial advice.\n\n" +
                                   "5. Usage: You agree not to use the app for any illegal purposes.",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = agreedToTerms,
                        onCheckedChange = { agreedToTerms = it },
                        colors = CheckboxDefaults.colors(checkmarkColor = Color(0xFF2D4B8E), uncheckedColor = Color.White)
                    )
                    Text("I agree to the Terms and Conditions", color = Color.White, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { 
                        if (agreedToTerms) {
                            onRegisterComplete(User(
                                name = name,
                                email = email,
                                phone = phone,
                                password = password,
                                mpin = mpin,
                                role = selectedRole
                            ))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    enabled = agreedToTerms
                ) {
                    Text("Register", color = Color(0xFF2D4B8E), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { currentStep = RegisterStep.MPIN_SETUP }) {
                    Text("Back", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Already have an account? Login",
            color = Color.White,
            modifier = Modifier.clickable { onBackToLogin() }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun registerTextFieldColors(isError: Boolean = false) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = if (isError) Color.Red else Color.White,
    unfocusedBorderColor = if (isError) Color.Red else Color.White.copy(alpha = 0.7f),
    errorBorderColor = Color.Red,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = Color.White,
    errorLabelColor = Color.Red,
    errorSupportingTextColor = Color.Red
)

@Composable
fun MPINScreen(
    userName: String = "ka-Tipid",
    onMpinComplete: (String) -> Unit
) {
    var mpin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4facfe),
                        Color(0xFF00f2fe)
                    )
                )
            )
    ) {
        // Top Section (Blue Area)
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxWidth()
        ) {
            // Main Centered Content
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // TipidTrack Logo Row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    repeat(4) { index ->
                        val isFilled = index < mpin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .border(2.dp, Color.White, CircleShape)
                                .background(
                                    if (isFilled) Color.White else Color.Transparent,
                                    CircleShape
                                )
                        )
                    }
                }
            }
            
            // Footer text pinned to bottom of blue area
            Text(
                text = "Never share your MPIN or OTP with anyone",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }

        // Bottom Section (Keypad Area)
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
