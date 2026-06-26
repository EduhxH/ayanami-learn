package com.yourname.ayanami.learn.ui.screens.voice

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.ayanami.learn.R
import com.yourname.ayanami.learn.ui.components.ReiAssetImage
import com.yourname.ayanami.learn.ui.feedback.rememberClickFeedback
import com.yourname.ayanami.learn.ui.localization.AppStrings
import com.yourname.ayanami.learn.ui.localization.LocalAppStrings
import com.yourname.ayanami.learn.ui.viewmodel.LiveVoiceUiState
import com.yourname.ayanami.learn.ui.viewmodel.LiveVoiceViewModel

@Composable
fun LiveVoiceScreen(
    onBack: () -> Unit,
    viewModel: LiveVoiceViewModel = hiltViewModel()
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val clickFeedback = rememberClickFeedback()
    val state by viewModel.uiState.collectAsState()
    val transition = rememberInfiniteTransition(label = "voice-call")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "voice-pulse"
    )
    val avatarScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (state.isSpeaking) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar-scale"
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startListening()
        } else {
            viewModel.onMicrophonePermissionDenied()
            Toast.makeText(context, "Microphone permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    val startMic: () -> Unit = {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.endSession() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoiceBackground)
    ) {
        VoiceHeader(
            title = strings.ayanamiLive,
            status = state.callStatus(strings),
            onClose = {
                clickFeedback()
                viewModel.endSession()
                onBack()
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            VoiceAvatarStage(
                state = state,
                pulse = pulse,
                avatarScale = avatarScale
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = state.callStatus(strings),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            val error = state.error
            if (error != null) {
                Text(
                    text = error,
                    color = VoiceError,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = state.callHint(strings),
                    color = VoiceMuted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )
            }

            val caption = state.liveCaption()
            if (caption.isNotBlank()) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = caption,
                    color = Color.White.copy(alpha = 0.78f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 320.dp)
                )
            }
        }

        VoiceCallControls(
            strings = strings,
            isListening = state.isListening,
            onMic = {
                clickFeedback()
                when {
                    state.isSpeaking -> {
                        viewModel.interruptPlayback()
                        startMic()
                    }
                    state.isListening -> viewModel.stopListening()
                    else -> startMic()
                }
            },
            onEnd = {
                clickFeedback()
                viewModel.endSession()
                onBack()
            }
        )
    }
}

@Composable
private fun VoiceHeader(
    title: String,
    status: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(VoiceControlDark)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(25.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(status, color = VoiceBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        // Keeps the title visually centered against the close button on the left.
        Spacer(modifier = Modifier.width(48.dp))
    }
}

@Composable
private fun VoiceAvatarStage(
    state: LiveVoiceUiState,
    pulse: Float,
    avatarScale: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.22f
            val listeningPulse = if (state.isListening) pulse else 0f
            val speakingPulse = if (state.isSpeaking) 0.22f else 0f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        VoiceBlue.copy(alpha = 0.50f + speakingPulse),
                        VoiceBlue.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    radius = size.minDimension * (0.28f + listeningPulse * 0.36f)
                ),
                radius = size.minDimension * (0.33f + listeningPulse * 0.18f),
                center = center
            )
            drawCircle(
                color = VoiceAvatarRing.copy(alpha = 0.9f),
                radius = radius + 6.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.88f),
                radius = radius,
                center = center
            )
        }
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(avatarScale)
                .clip(CircleShape)
                .background(Color(0xFF07111F)),
            contentAlignment = Alignment.Center
        ) {
            ReiAssetImage(
                resId = R.drawable.reiayanamiplush,
                modifier = Modifier.size(102.dp),
                contentDescription = "Rei Ayanami"
            )
        }
    }
}

@Composable
private fun VoiceCallControls(
    strings: AppStrings,
    isListening: Boolean,
    onMic: () -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = 8.dp, bottom = 30.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top
    ) {
        CallButtonWithLabel(
            label = strings.voiceMic,
            icon = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
            background = if (isListening) VoiceBlue.copy(alpha = 0.26f) else VoiceControlDark,
            size = 70.dp,
            onClick = onMic
        )
        Spacer(modifier = Modifier.width(44.dp))
        CallButtonWithLabel(
            label = strings.voiceEnd,
            icon = Icons.Default.CallEnd,
            background = VoiceRed,
            size = 70.dp,
            onClick = onEnd
        )
    }
}

@Composable
private fun CallButtonWithLabel(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(background)
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(size * 0.42f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = VoiceMuted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

private fun LiveVoiceUiState.callStatus(strings: AppStrings): String {
    return when {
        error != null -> "Erro"
        isConnecting -> strings.voiceConnecting
        isSpeaking -> strings.voiceSpeaking
        isListening -> strings.voiceListening
        userTranscript.isNotBlank() && !isSpeaking -> strings.voiceThinking
        isConnected -> strings.voiceReady
        else -> strings.voiceReady
    }
}

private fun LiveVoiceUiState.callHint(strings: AppStrings): String {
    return when {
        isConnecting -> strings.voiceHintConnecting
        isSpeaking -> strings.voiceHintSpeaking
        isListening -> strings.voiceHintListening
        else -> strings.voiceHintReady
    }
}

private fun LiveVoiceUiState.liveCaption(): String {
    return when {
        isSpeaking && assistantTranscript.isNotBlank() -> assistantTranscript.trim()
        isListening && userTranscript.isNotBlank() -> userTranscript.trim()
        else -> ""
    }
}

private val VoiceBackground = Color(0xFF0E1728)
private val VoiceControlDark = Color(0xFF172235)
private val VoiceAvatarRing = Color(0xFF243046)
private val VoiceBlue = Color(0xFF4A90E2)
private val VoiceRed = Color(0xFFFF2D3D)
private val VoiceError = Color(0xFFFF6B7A)
private val VoiceMuted = Color(0xFF93A4BE)
