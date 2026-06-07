package com.yourname.ayanami.learn.ui.screens.settings

import android.app.Activity
import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.ayanami.learn.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.yourname.ayanami.learn.data.local.ActivityStatus
import com.yourname.ayanami.learn.data.local.NativeLanguage
import com.yourname.ayanami.learn.data.local.UserPreferences
import com.yourname.ayanami.learn.data.repository.AccountConnectionState
import com.yourname.ayanami.learn.ui.components.ReiAssetImage
import com.yourname.ayanami.learn.ui.localization.LocalAppStrings
import com.yourname.ayanami.learn.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val preferences by viewModel.preferences.collectAsState()
    val connections by viewModel.connections.collectAsState()
    val message by viewModel.message.collectAsState()
    var editProfileOpen by remember { mutableStateOf(false) }
    var diagnosticsOpen by remember { mutableStateOf(false) }
    var aboutOpen by remember { mutableStateOf(false) }
    var goalValue by remember(preferences.dailyGoalMinutes) {
        mutableFloatStateOf(preferences.dailyGoalMinutes.toFloat())
    }
    val appVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "debug"
        }.getOrDefault("debug")
    }

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
                    viewModel.connectGoogle(idToken)
                }
            }
            .onFailure { error ->
                Toast.makeText(context, error.localizedMessage ?: strings.googleSignInFailed, Toast.LENGTH_SHORT).show()
            }
    }

    LaunchedEffect(message) {
        val value = message ?: return@LaunchedEffect
        Toast.makeText(context, value, Toast.LENGTH_SHORT).show()
        viewModel.clearMessage()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    text = strings.settings,
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
            }

            ProfileCard(
                preferences = preferences,
                onEdit = { editProfileOpen = true }
            )

            SettingsSection(title = strings.learning) {
                SettingsItem(
                    icon = Icons.Default.Star,
                    title = strings.dailyGoal,
                    description = strings.minutesPerDay(preferences.dailyGoalMinutes)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBg)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Slider(
                        value = goalValue,
                        onValueChange = { goalValue = it },
                        onValueChangeFinished = { viewModel.updateDailyGoal(goalValue.toInt()) },
                        valueRange = 5f..60f,
                        steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentBlue,
                            activeTrackColor = AccentBlue,
                            inactiveTrackColor = BorderColor
                        )
                    )
                }
                SettingsItem(
                    icon = Icons.Default.Mic,
                    title = strings.speakingExercises,
                    rightElement = {
                        AyanamiSwitch(
                            checked = preferences.speakingExercises,
                            onCheckedChange = viewModel::updateSpeakingExercises
                        )
                    }
                )
            }

            SettingsSection(title = strings.preferences) {
                SettingsItem(
                    icon = Icons.Default.VolumeUp,
                    title = strings.voiceReplies,
                    rightElement = {
                        AyanamiSwitch(
                            checked = preferences.voiceReplies,
                            onCheckedChange = viewModel::updateVoiceReplies
                        )
                    }
                )
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = strings.darkMode,
                    description = strings.ayanamiTheme,
                    rightElement = {
                        AyanamiSwitch(
                            checked = preferences.darkTheme,
                            onCheckedChange = viewModel::updateDarkTheme
                        )
                    }
                )
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = strings.studyReminders,
                    rightElement = {
                        AyanamiSwitch(
                            checked = preferences.studyReminders,
                            onCheckedChange = viewModel::updateStudyReminders
                        )
                    }
                )
                SettingsItem(
                    icon = Icons.Default.VolumeUp,
                    title = strings.soundEffects,
                    rightElement = {
                        AyanamiSwitch(
                            checked = preferences.soundEffects,
                            onCheckedChange = viewModel::updateSoundEffects
                        )
                    }
                )
            }

            SettingsSection(title = strings.accountAndSupport) {
                ProviderItem(
                    iconRes = R.drawable.google_icon_logo_symbol,
                    title = if (connections.googleConnected) strings.disconnectGoogle else strings.connectGoogle,
                    connected = connections.googleConnected,
                    onClick = {
                        if (connections.googleConnected) {
                            viewModel.disconnectGoogle()
                        } else {
                            googleLauncher.launch(viewModel.getGoogleSignInClient().signInIntent)
                        }
                    }
                )
                ProviderItem(
                    iconRes = R.drawable.github_white_icon,
                    title = if (connections.githubConnected) strings.disconnectGithub else strings.connectGithub,
                    connected = connections.githubConnected,
                    onClick = {
                        val activity = context as? Activity
                        if (connections.githubConnected) {
                            viewModel.disconnectGithub()
                        } else if (activity != null) {
                            viewModel.connectGithub(activity)
                        }
                    }
                )
                SettingsItem(Icons.Default.Help, strings.appDiagnostics, onClick = { diagnosticsOpen = true })
                SettingsItem(Icons.Default.Info, strings.about, onClick = { aboutOpen = true })
                SettingsItem(
                    icon = Icons.Default.Logout,
                    title = strings.logout,
                    destructive = true,
                    onClick = { viewModel.logout(onLogout) }
                )
            }
        }

        if (editProfileOpen) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { editProfileOpen = false },
                sheetState = sheetState,
                containerColor = SheetBg,
                contentColor = TextPrimary,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                scrimColor = Color.Black.copy(alpha = 0.62f)
            ) {
                EditProfileSheet(
                    preferences = preferences,
                    onDismiss = { editProfileOpen = false },
                    onSaveProfile = { name, bio, uri ->
                        viewModel.updateProfile(name, bio, uri)
                        editProfileOpen = false
                    },
                    onNativeLanguageSelected = viewModel::updateNativeLanguage,
                    onActivityStatusSelected = viewModel::updateActivityStatus
                )
            }
        }

        if (diagnosticsOpen) {
            InfoDialog(
                title = strings.appDiagnostics,
                lines = listOf(
                    strings.signedInAs(preferences.email ?: preferences.profileName.ifBlank { "-" }),
                    strings.firebaseSession(connections.firebaseSignedIn),
                    "${strings.nativeLanguage}: ${preferences.nativeLanguage.displayName(preferences.nativeLanguage)}",
                    "${strings.activityStatus}: ${strings.statusLabel(preferences.activityStatus)}",
                    strings.minutesPerDay(preferences.dailyGoalMinutes)
                ),
                onDismiss = { diagnosticsOpen = false }
            )
        }

        if (aboutOpen) {
            InfoDialog(
                title = strings.about,
                lines = listOf(
                    "Ayanami Learn",
                    strings.appVersion(appVersion),
                    strings.ayanamiTheme
                ),
                onDismiss = { aboutOpen = false }
            )
        }
    }
}

@Composable
private fun InfoDialog(
    title: String,
    lines: List<String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SheetBg,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(title, fontWeight = FontWeight.Black)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                lines.forEach { line ->
                    Text(line, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = AccentBlue, fontWeight = FontWeight.Black)
            }
        }
    )
}

@Composable
private fun ProfileCard(
    preferences: UserPreferences,
    onEdit: () -> Unit
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 24.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(CardBg)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileImage(preferences.photoUri, Modifier.size(68.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            Text(
                text = preferences.profileName.ifBlank { strings.completeYourProfile },
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusDot(preferences.activityStatus)
                Text(
                    text = strings.statusLabel(preferences.activityStatus),
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(AccentBlue.copy(alpha = 0.14f))
                .clickable(onClick = onEdit)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(strings.edit, color = AccentBlue, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = title.uppercase(),
            color = TextSecondary,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 14.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CardBg),
            content = content
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    description: String? = null,
    destructive: Boolean = false,
    rightElement: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (destructive) ErrorColor.copy(alpha = 0.12f) else IconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (destructive) ErrorColor else TextSecondary, modifier = Modifier.size(22.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            Text(title, color = if (destructive) ErrorColor else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (description != null) {
                Text(description, color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
        if (rightElement != null) {
            rightElement()
        } else if (onClick != null) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
private fun ProviderItem(
    @DrawableRes iconRes: Int,
    title: String,
    connected: Boolean,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(IconBg),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                text = if (connected) strings.connected else strings.notConnected,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
    }
}

@Composable
private fun AyanamiSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = AccentBlue,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0xFF334155)
        )
    )
}

@Composable
private fun EditProfileSheet(
    preferences: UserPreferences,
    onDismiss: () -> Unit,
    onSaveProfile: (String, String, Uri?) -> Unit,
    onNativeLanguageSelected: (NativeLanguage) -> Unit,
    onActivityStatusSelected: (ActivityStatus, Boolean) -> Unit
) {
    val strings = LocalAppStrings.current
    var name by remember(preferences.displayName) { mutableStateOf(TextFieldValue(preferences.displayName)) }
    var bio by remember(preferences.bio) { mutableStateOf(TextFieldValue(preferences.bio)) }
    var selectedPhoto by remember { mutableStateOf<Uri?>(null) }
    var rememberStatus by remember(preferences.rememberActivityStatus) {
        mutableStateOf(preferences.rememberActivityStatus)
    }
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> selectedPhoto = uri }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(bottom = 28.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 4.dp, bottom = 16.dp)
                .size(width = 48.dp, height = 5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(BorderColor)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(strings.editProfile, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 21.sp, modifier = Modifier.weight(1f))
            Text(
                text = strings.save,
                color = AccentBlue,
                fontWeight = FontWeight.Black,
                modifier = Modifier.clickable {
                    onSaveProfile(name.text, bio.text, selectedPhoto)
                }
            )
        }
        Spacer(Modifier.height(22.dp))
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(118.dp)
                .clip(CircleShape)
                .background(IconBg)
                .clickable { photoLauncher.launch(arrayOf("image/*")) },
            contentAlignment = Alignment.Center
        ) {
            ProfileImage(selectedPhoto?.toString() ?: preferences.photoUri, Modifier.size(118.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(AccentBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = strings.changePhoto, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        SheetTextField(strings.displayName, name, { name = it }, singleLine = true)
        Spacer(Modifier.height(14.dp))
        SheetTextField(strings.bio, bio, { bio = it }, singleLine = false)
        Spacer(Modifier.height(22.dp))
        SheetLabel(strings.activityStatus)
        StatusSelector(
            selectedStatus = preferences.activityStatus,
            rememberStatus = rememberStatus,
            onStatusSelected = { status -> onActivityStatusSelected(status, rememberStatus) },
            onRememberChanged = { remember ->
                rememberStatus = remember
                onActivityStatusSelected(preferences.activityStatus, remember)
            }
        )
        Spacer(Modifier.height(22.dp))
        SheetLabel(strings.nativeLanguage)
        LanguageSelector(
            selected = preferences.nativeLanguage,
            onSelected = onNativeLanguageSelected
        )
    }
}

@Composable
private fun SheetTextField(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    singleLine: Boolean
) {
    Column {
        SheetLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = FieldBg,
                unfocusedContainerColor = FieldBg,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = AccentBlue
            )
        )
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = TextSecondary,
        fontWeight = FontWeight.Black,
        fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun StatusSelector(
    selectedStatus: ActivityStatus,
    rememberStatus: Boolean,
    onStatusSelected: (ActivityStatus) -> Unit,
    onRememberChanged: (Boolean) -> Unit
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(FieldBg)
    ) {
        ActivityStatus.entries.forEach { status ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStatusSelected(status) }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusDot(status)
                Text(
                    text = strings.statusLabel(status),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
                RadioIndicator(selected = selectedStatus == status)
            }
        }
    }
    Row(
        modifier = Modifier.padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = rememberStatus, onCheckedChange = onRememberChanged)
        Text(strings.rememberThisSetting, color = TextSecondary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LanguageSelector(
    selected: NativeLanguage,
    onSelected: (NativeLanguage) -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(FieldBg)
    ) {
        NativeLanguage.entries.forEach { language ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(language) }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageFlag(language)
                Text(
                    text = language.displayName(selected),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
                RadioIndicator(selected = selected == language)
            }
        }
    }
}

@Composable
private fun RadioIndicator(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(21.dp)
            .clip(CircleShape)
            .background(if (selected) AccentBlue else BorderColor),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun ProfileImage(uri: String?, modifier: Modifier) {
    if (uri.isNullOrBlank()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            ReiAssetImage(
                resId = R.drawable.reiayanamiplush,
                modifier = Modifier.size(86.dp),
                contentDescription = "Rei Ayanami"
            )
        }
    } else {
        AndroidView(
            modifier = modifier.clip(CircleShape),
            factory = { context ->
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setImageURI(Uri.parse(uri))
                }
            },
            update = { imageView -> imageView.setImageURI(Uri.parse(uri)) }
        )
    }
}

@Composable
private fun StatusDot(status: ActivityStatus) {
    val color = when (status) {
        ActivityStatus.Online -> Color(0xFF10B981)
        ActivityStatus.Idle -> Color(0xFFF59E0B)
        ActivityStatus.Offline -> Color(0xFF64748B)
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun LanguageFlag(language: NativeLanguage) {
    Canvas(
        modifier = Modifier
            .size(width = 28.dp, height = 18.dp)
            .clip(RoundedCornerShape(3.dp))
    ) {
        when (language) {
            NativeLanguage.Portuguese -> {
                drawRect(Color(0xFF046A38), size = size.copy(width = size.width * 0.4f))
                drawRect(
                    Color(0xFFDA291C),
                    topLeft = Offset(size.width * 0.4f, 0f),
                    size = size.copy(width = size.width * 0.6f)
                )
                drawCircle(Color(0xFFFFD100), radius = size.height * 0.23f, center = Offset(size.width * 0.4f, size.height / 2f))
            }
            NativeLanguage.Ukrainian -> {
                drawRect(Color(0xFF0057B7), size = size.copy(height = size.height / 2f))
                drawRect(Color(0xFFFFD700), topLeft = Offset(0f, size.height / 2f), size = size.copy(height = size.height / 2f))
            }
            NativeLanguage.Russian -> {
                drawRect(Color.White, size = size.copy(height = size.height / 3f))
                drawRect(Color(0xFF0039A6), topLeft = Offset(0f, size.height / 3f), size = size.copy(height = size.height / 3f))
                drawRect(Color(0xFFD52B1E), topLeft = Offset(0f, size.height * 2f / 3f), size = size.copy(height = size.height / 3f))
            }
        }
    }
}

private val SettingsBg = Color(0xFF0B101E)
private val SheetBg = Color(0xFF131B2F)
private val CardBg = Color(0xFF151D30)
private val FieldBg = Color(0xFF1A233A)
private val IconBg = Color(0xFF1E2943)
private val BorderColor = Color(0xFF1E293B)
private val AccentBlue = Color(0xFF5B8CFF)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextSecondary = Color(0xFF94A3B8)
private val ErrorColor = Color(0xFFFF6B7A)
