package com.yourname.ayanami.learn.ui.screens.ai

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.ayanami.learn.R
import com.yourname.ayanami.learn.ui.components.ReiAssetImage
import com.yourname.ayanami.learn.ui.localization.LocalAppStrings

@Composable
fun AiModeScreen(
    onBack: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenVoice: () -> Unit
) {
    val strings = LocalAppStrings.current
    val transition = rememberInfiniteTransition(label = "ai-mode-dance")
    val buttonLift by transition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "button-lift"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AiBackground)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(start = 20.dp, top = 24.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(AiIconDark)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }

        ReiAssetImage(
            resId = R.drawable.reidancing,
            contentDescription = "Rei Ayanami dancing",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 112.dp)
                .size(260.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 116.dp)
                .padding(horizontal = 38.dp)
                .widthIn(max = 360.dp)
                .graphicsLayer { translationY = buttonLift },
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AiChoiceButton(
                title = strings.voiceAi,
                description = strings.voiceAiDescription,
                icon = Icons.Default.Mic,
                selected = true,
                onClick = onOpenVoice
            )
            AiChoiceButton(
                title = strings.textAi,
                description = strings.textAiDescription,
                icon = Icons.Default.Message,
                selected = false,
                onClick = onOpenChat
            )
        }
    }
}

@Composable
private fun AiChoiceButton(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) {
        Brush.horizontalGradient(listOf(AiBlue, AiBlueDeep))
    } else {
        Brush.horizontalGradient(listOf(AiCard, AiCard))
    }
    val iconBg = if (selected) Color.White.copy(alpha = 0.16f) else AiIconDark
    val subtitleColor = if (selected) Color.White.copy(alpha = 0.72f) else AiMuted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .shadow(
                elevation = if (selected) 12.dp else 0.dp,
                shape = RoundedCornerShape(15.dp),
                ambientColor = AiBlue.copy(alpha = 0.24f),
                spotColor = AiBlue.copy(alpha = 0.24f)
            )
            .clip(RoundedCornerShape(15.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 13.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 17.sp
            )
            Text(
                text = description,
                color = subtitleColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 13.sp
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = if (selected) Color.White.copy(alpha = 0.46f) else AiMuted,
            modifier = Modifier.size(23.dp)
        )
    }
}

private val AiBackground = Color(0xFF0B101E)
private val AiCard = Color(0xFF151D30)
private val AiIconDark = Color(0xFF1E2943)
private val AiBlue = Color(0xFF4F77E8)
private val AiBlueDeep = Color(0xFF456BDD)
private val AiMuted = Color(0xFF94A3B8)
