package com.yourname.ayanami.learn.ui.screens.auth

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.yourname.ayanami.learn.R
import com.yourname.ayanami.learn.data.local.NativeLanguage
import com.yourname.ayanami.learn.ui.components.ReiAssetImage
import com.yourname.ayanami.learn.ui.localization.LocalAppStrings
import com.yourname.ayanami.learn.ui.viewmodel.AuthState
import com.yourname.ayanami.learn.ui.viewmodel.AuthViewModel

@Composable
fun AccountCreationScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val loading = authState is AuthState.Loading

    var step by remember { mutableIntStateOf(1) }
    var fullName by remember { mutableStateOf("") }
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
            .background(SignupOuter)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SignupBackground)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(top = 42.dp, bottom = 38.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SignupHeader(
                step = step,
                onBack = {
                    if (step == 1) navController.popBackStack() else step = 1
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp, bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                ReiAssetImage(
                    resId = R.drawable.reiayanamiplush,
                    modifier = Modifier.size(118.dp),
                    contentDescription = "Rei Ayanami"
                )
            }

            Text(
                text = if (step == 1) strings.authLetsStart else strings.authAlmostThere,
                color = SignupText,
                fontSize = 26.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = if (step == 1) {
                    strings.authStepOneDescription
                } else {
                    strings.authStepTwoDescription
                },
                color = SignupMuted,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
                    .fillMaxWidth()
            )

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "signup-step"
            ) { targetStep ->
                if (targetStep == 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SignupTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            placeholder = strings.authFullName,
                            icon = Icons.Default.Person
                        )
                        SignupTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = strings.authEmail,
                            icon = Icons.Default.Email,
                            keyboardType = KeyboardType.Email
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SignupTextField(
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
                                        tint = SignupPlaceholder
                                    )
                                }
                            }
                        )
                        SignupLanguageSelector(
                            selected = preferences.nativeLanguage,
                            label = strings.nativeLanguage,
                            onSelected = viewModel::updateNativeLanguage
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            SignupPrimaryButton(
                text = if (step == 1) strings.authContinue else strings.authCreateAccount,
                loading = loading,
                enabled = if (step == 1) {
                    fullName.isNotBlank() && email.isNotBlank()
                } else {
                    password.length >= 6
                },
                onClick = {
                    if (step == 1) {
                        step = 2
                    } else {
                        viewModel.createAccount(
                            fullName = fullName,
                            email = email,
                            password = password,
                            nativeLanguage = preferences.nativeLanguage.code
                        )
                    }
                }
            )
            Spacer(Modifier.height(12.dp))
            SignupGoogleButton(
                text = strings.authCreateWithGoogle,
                enabled = !loading,
                onClick = {
                    googleLauncher.launch(viewModel.getGoogleSignInClient().signInIntent)
                }
            )

            AnimatedVisibility(visible = loading) {
                Text(
                    text = strings.authCreatingAccount,
                    color = SignupMuted,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun SignupHeader(
    step: Int,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SignupText, modifier = Modifier.size(28.dp))
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 46.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            StepPill(active = step >= 1)
            Spacer(Modifier.size(8.dp))
            StepPill(active = step >= 2)
        }
    }
}

@Composable
private fun StepPill(active: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 34.dp, height = 6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) SignupBlue else SignupBorder)
    )
}

@Composable
private fun SignupTextField(
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
        placeholder = { Text(placeholder, color = SignupPlaceholder) },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = SignupPlaceholder, modifier = Modifier.size(22.dp))
        },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SignupField,
            unfocusedContainerColor = SignupField,
            focusedBorderColor = SignupBlue,
            unfocusedBorderColor = SignupBorder,
            cursorColor = SignupBlue,
            focusedTextColor = SignupText,
            unfocusedTextColor = SignupText
        )
    )
}

@Composable
private fun SignupLanguageSelector(
    selected: NativeLanguage,
    label: String,
    onSelected: (NativeLanguage) -> Unit
) {
    Column {
        Text(
            text = label,
            color = SignupMuted,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NativeLanguage.entries.forEach { language ->
                val active = language == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(if (active) SignupBlue else SignupCard)
                        .border(1.dp, if (active) SignupBlue else SignupBorder, RoundedCornerShape(15.dp))
                        .clickable { onSelected(language) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = language.displayName(selected),
                        color = if (active) Color.White else SignupText,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SignupPrimaryButton(
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
            .background(if (enabled && !loading) SignupBlue else SignupDisabled)
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(23.dp))
        }
    }
}

@Composable
private fun SignupGoogleButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SignupCard)
            .border(1.dp, SignupBorder, RoundedCornerShape(16.dp))
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
            color = SignupText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
    }
}

private val SignupOuter = Color(0xFF080B13)
private val SignupBackground = Color(0xFF0B101E)
private val SignupCard = Color(0xFF151D30)
private val SignupField = Color(0xFF1A233A)
private val SignupBorder = Color(0xFF334155)
private val SignupBlue = Color(0xFF5B8CFF)
private val SignupDisabled = Color(0xFF334155)
private val SignupText = Color(0xFFF8FAFC)
private val SignupMuted = Color(0xFF94A3B8)
private val SignupPlaceholder = Color(0xFF64748B)
