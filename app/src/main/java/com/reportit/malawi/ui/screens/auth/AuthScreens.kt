package com.reportit.malawi.ui.screens.auth

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.reportit.malawi.R
import com.reportit.malawi.data.ReportStore
import com.reportit.malawi.ui.components.Field
import com.reportit.malawi.ui.components.FormScaffold
import com.reportit.malawi.ui.components.LogoMark
import com.reportit.malawi.ui.components.fieldColors
import com.reportit.malawi.ui.theme.*
import com.reportit.malawi.util.Amber
import com.reportit.malawi.util.Red

/**
 * AuthScreens: I created this file to contain all authentication screens for ReportIt Malawi,
 * including Splash, Login, Registration (Sign Up), and Password Reset (Forgot Password).
 */

/* ---------------------------------------------------------
   SPLASH SCREEN
   I created the splash screen as the initial landing visual
   displaying the ReportIt Malawi logo and mission slogan.
   --------------------------------------------------------- */

@Composable
fun SplashScreen(onContinue: () -> Unit) {
    Box(Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(R.drawable.malawi_road_splash),
            contentDescription = "Malawi road",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Spacer(Modifier.height(60.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LogoMark(96.dp)

                Spacer(Modifier.height(22.dp))

                Text(
                    "ReportIt",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    "Malawi",
                    color = Color(0xFFFFC107),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    "Report. Track. Improve.",
                    color = Color.White.copy(alpha = .92f),
                    fontSize = 15.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Better roads. A stronger Malawi.",
                    color = Color.White.copy(alpha = .88f),
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(22.dp))

                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenPrimary
                    )
                ) {
                    Text(
                        "Get Started",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(26.dp))
            }
        }
    }
}

/* ---------------------------------------------------------
   LOGIN SCREEN
   --------------------------------------------------------- */

@Composable
fun LoginScreen(
    onLogin: (String, String, (String) -> Unit) -> Unit,
    onSignUp: () -> Unit,
    onForgot: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Surface(
        Modifier.fillMaxSize(),
        color = BackgroundLight
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LogoMark(76.dp)

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        "REPORTIT MALAWI",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GreenDark
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        "CITIZEN PORTAL",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Amber
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {

                    Text(
                        "Welcome back",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "Sign in to report and track road problems.",
                        color = TextSecondary
                    )

                    Spacer(Modifier.height(22.dp))

                    Field(
                        "Email address",
                        email,
                        {
                            email = it
                            error = ""
                        },
                        keyboard = KeyboardType.Email
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            error = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password", color = Color.Black) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    showPassword = !showPassword
                                }
                            ) {
                                Icon(
                                    if (showPassword) {
                                        Icons.Default.VisibilityOff
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                    contentDescription = "Toggle password visibility",
                                    tint = Color.Black
                                )
                            }
                        },
                        visualTransformation = if (showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors()
                    )

                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))

                        Text(
                            error,
                            color = Red,
                            fontSize = 12.sp
                        )
                    }

                    TextButton(
                        onClick = onForgot,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            "Forgot password?",
                            color = GreenDark
                        )
                    }

                    Button(
                        onClick = {
                            error = when {
                                email.isBlank() || password.isBlank() ->
                                    "Enter your email and password."

                                !Patterns.EMAIL_ADDRESS.matcher(
                                    email.trim()
                                ).matches() ->
                                    "Enter a valid email address."

                                else -> ""
                            }

                            if (error.isEmpty()) {
                                loading = true

                                onLogin(
                                    email.trim(),
                                    password
                                ) { message ->
                                    loading = false
                                    error = message
                                }
                            }
                        },
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary
                        )
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )

                            Spacer(Modifier.width(8.dp))

                            Text("Signing in...")
                        } else {
                            Text(
                                "Login",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(Modifier.weight(1f))

                        Text(
                            "  OR  ",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        HorizontalDivider(Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onSignUp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = GreenDark
                        )
                    ) {
                        Text(
                            "Create an account",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Roads Authority officers: sign in with your official account.",
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "By continuing, you agree to the ReportIt Malawi terms and privacy policy.",
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

/* ---------------------------------------------------------
   CREATE ACCOUNT SCREEN
   --------------------------------------------------------- */

@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onCreated: (String, String, String, (String) -> Unit) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showConfirmPassword by rememberSaveable { mutableStateOf(false) }

    var error by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    FormScaffold(
        "Create account",
        "Join ReportIt Malawi",
        onBack
    ) {

        Text(
            "Create your account while connected. After your first sign-in, you can log in offline on this device.",
            color = TextSecondary,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(16.dp))

        // FULL NAME
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                error = ""
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Full name", color = Color.Black) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors()
        )

        Spacer(Modifier.height(12.dp))

        // EMAIL ADDRESS
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                error = ""
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email address", color = Color.Black) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors()
        )

        Spacer(Modifier.height(12.dp))

        // ENTER PASSWORD
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                error = ""
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Enter password", color = Color.Black) },
            trailingIcon = {
                IconButton(
                    onClick = {
                        showPassword = !showPassword
                    }
                ) {
                    Icon(
                        if (showPassword) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = "Toggle password visibility",
                        tint = Color.Black
                    )
                }
            },
            visualTransformation = if (showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors()
        )

        Spacer(Modifier.height(12.dp))

        // CONFIRM PASSWORD
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                error = ""
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Confirm password", color = Color.Black) },
            trailingIcon = {
                IconButton(
                    onClick = {
                        showConfirmPassword = !showConfirmPassword
                    }
                ) {
                    Icon(
                        if (showConfirmPassword) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = "Toggle confirmation visibility",
                        tint = Color.Black
                    )
                }
            },
            visualTransformation = if (showConfirmPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors()
        )

        // ERROR MESSAGE
        if (error.isNotBlank()) {
            Spacer(Modifier.height(10.dp))

            Text(
                text = error,
                color = Red,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(22.dp))

        // CREATE ACCOUNT BUTTON
        Button(
            onClick = {
                error = when {
                    name.isBlank() ->
                        "Enter your full name."

                    email.isBlank() ->
                        "Enter your email address."

                    !Patterns.EMAIL_ADDRESS.matcher(
                        email.trim()
                    ).matches() ->
                        "Enter a valid email address."

                    password.isBlank() ->
                        "Enter your password."

                    password.length < 8 ->
                        "Please use a strong password."

                    confirmPassword.isBlank() ->
                        "Please confirm your password."

                    password != confirmPassword ->
                        "Passwords do not match."

                    else -> ""
                }

                if (error.isEmpty()) {
                    loading = true

                    onCreated(
                        name.trim(),
                        email.trim(),
                        password
                    ) { message ->
                        loading = false
                        error = message
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenPrimary
            )
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )

                Spacer(Modifier.width(8.dp))

                Text("Creating...")
            } else {
                Text(
                    "Create account",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/* ---------------------------------------------------------
   FORGOT PASSWORD SCREEN
   Sends a reset link through Firebase Authentication.
   --------------------------------------------------------- */

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var successMsg by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    FormScaffold(
        "Reset password",
        "Recover your account",
        onBack
    ) {

        Text("We’ll email you a secure link to choose a new password. You need an internet connection.", color = TextSecondary)

        Spacer(Modifier.height(18.dp))

        Field(
            "Email address",
            email,
            {
                email = it
                error = ""
                successMsg = ""
            },
            keyboard = KeyboardType.Email
        )

        if (error.isNotBlank()) {
            Spacer(Modifier.height(8.dp))

            Text(
                error,
                color = Red,
                fontSize = 12.sp
            )
        }

        if (successMsg.isNotBlank()) {
            Spacer(Modifier.height(10.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F8F0)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = successMsg,
                    color = Color(0xFF13794F),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        Button(
            onClick = {
                val cleanEmail = email.trim()

                error = if (cleanEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) "Enter a valid email address." else ""

                if (error.isNotBlank()) return@Button

                loading = true
                error = ""
                successMsg = ""

                ReportStore.resetPassword(
                    cleanEmail,
                    onSuccess = {
                        loading = false
                        successMsg = "If an account matches this address, Firebase will send a password reset link. Open it to choose your new password, then sign in again."
                    },
                    onFailure = { message ->
                        loading = false
                        error = message
                    }
                )
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenPrimary
            )
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )

                Spacer(Modifier.width(8.dp))

                Text("Resetting...")
            } else {
                Text(
                    "Reset Password",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
