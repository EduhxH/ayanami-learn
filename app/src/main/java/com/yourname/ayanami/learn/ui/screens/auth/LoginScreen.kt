package com.yourname.ayanami.learn.ui.screens.auth

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.yourname.ayanami.learn.R
import com.yourname.ayanami.learn.ui.components.ReiAssetImage
import com.yourname.ayanami.learn.ui.localization.LocalAppStrings
import com.yourname.ayanami.learn.ui.viewmodel.AuthState
import com.yourname.ayanami.learn.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val notice by viewModel.notice.collectAsState()
    val loading = authState is AuthState.Loading

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        runCatching { task.getResult(ApiException::class.java) }
            .onSuccess { account ->
                val idToken = account.idToken
                if (idToken == null) {
                    Toast.makeText(context, strings.googleIdTokenMissing, Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.signInWithGoogle(idToken)
                }
            }
            .onFailure { error ->
                Toast.makeText(context, error.localizedMessage ?: strings.googleSignInFailed, Toast.LENGTH_SHORT).show()
            }
    }

    LaunchedEffect(notice) {
        val value = notice ?: return@LaunchedEffect
        Toast.makeText(context, value, Toast.LENGTH_SHORT).show()
        viewModel.clearNotice()
    }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                    launchSingleTop = true
                }
                viewModel.clearState()
            }
            is AuthState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.clearState()
            }
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthOuter)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AuthBackground)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 42.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 22.dp)
                    .size(168.dp),
                contentAlignment = Alignment.Center
            ) {
                ReiAssetImage(
                    resId = R.drawable.reispinning,
                    modifier = Modifier.size(164.dp),
                    contentDescription = "Rei Ayanami"
                )
            }

            Text(
                text = buildAnnotatedString {
                    append(strings.authWelcomePrefix)
                    append("\n")
                    withStyle(SpanStyle(color = AuthBlue)) {
                        append("Ayanami Learn")
                    }
                },
                color = AuthText,
                fontSize = 30.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = strings.authTagline,
                color = AuthMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp, bottom = 34.dp)
            )

            AuthTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = strings.authEmail,
                icon = Icons.Default.Email,
                keyboardType = KeyboardType.Email
            )
            Spacer(Modifier.height(14.dp))
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = strings.authPassword,
                icon = Icons.Default.Lock,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility",
                            tint = AuthMuted
                        )
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = strings.authForgotPassword,
                    color = AuthBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(enabled = !loading) {
                        viewModel.sendPasswordResetEmail(email)
                    }
                )
            }

            AuthPrimaryButton(
                text = strings.authSignIn,
                loading = loading,
                enabled = email.isNotBlank() && password.isNotBlank(),
                onClick = { viewModel.signInWithEmail(email, password) }
            )
            Spacer(Modifier.height(12.dp))
            AuthGoogleButton(
                text = strings.authSignInWithGoogle,
                enabled = !loading,
                onClick = {
                    googleLauncher.launch(viewModel.getGoogleSignInClient().signInIntent)
                }
            )

            Column(
                modifier = Modifier.padding(top = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings.authNoAccount,
                    color = AuthMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(14.dp))
                AuthSecondaryButton(
                    text = strings.authCreateFreeAccount,
                    enabled = !loading,
                    onClick = { navController.navigate("account_creation") }
                )
            }

            AnimatedVisibility(visible = loading) {
                Text(
                    text = strings.authConnecting,
                    color = AuthMuted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder, color = AuthPlaceholder) },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = AuthPlaceholder, modifier = Modifier.size(22.dp))
        },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AuthField,
            unfocusedContainerColor = AuthField,
            focusedBorderColor = AuthBlue,
            unfocusedBorderColor = AuthBorder,
            cursorColor = AuthBlue,
            focusedTextColor = AuthText,
            unfocusedTextColor = AuthText
        )
    )
}

@Composable
private fun AuthPrimaryButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled && !loading) AuthBlue else AuthDisabled)
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun AuthSecondaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AuthCard)
            .border(1.dp, AuthBorder, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = AuthText, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun AuthGoogleButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AuthCard)
            .border(1.dp, AuthBorder, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.google_icon_logo_symbol),
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = text,
            color = AuthText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
    }
}

private val AuthOuter = Color(0xFF080B13)
private val AuthBackground = Color(0xFF0B101E)
private val AuthCard = Color(0xFF151D30)
private val AuthField = Color(0xFF1A233A)
private val AuthBorder = Color(0xFF334155)
private val AuthBlue = Color(0xFF5B8CFF)
private val AuthDisabled = Color(0xFF334155)
private val AuthText = Color(0xFFF8FAFC)
private val AuthMuted = Color(0xFF94A3B8)
private val AuthPlaceholder = Color(0xFF64748B)
