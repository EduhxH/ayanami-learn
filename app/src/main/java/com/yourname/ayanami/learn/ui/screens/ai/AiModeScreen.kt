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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.ayanami.learn.R
import com.yourname.ayanami.learn.ui.components.ReiAssetImage
import com.yourname.ayanami.learn.ui.feedback.rememberClickFeedback
import com.yourname.ayanami.learn.ui.localization.LocalAppStrings

@Composable
fun AiModeScreen(
    onBack: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenVoice: () -> Unit
) {
    val strings = LocalAppStrings.current
    val clickFeedback = rememberClickFeedback()
    val transition = rememberInfiniteTransition(label = "ai-mode-dance")
    val tileLift by transition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tile-lift"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AiBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    clickFeedback()
                    onBack()
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(AiIconDark)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ReiAssetImage(
                resId = R.drawable.reidancing,
                contentDescription = "Rei Ayanami dancing",
                modifier = Modifier.size(240.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = strings.aiMode,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.choosePractice,
                color = AiMuted,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 28.dp, end = 28.dp, bottom = 30.dp)
                .widthIn(max = 460.dp)
                .graphicsLayer { translationY = tileLift },
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AiChoiceTile(
                title = strings.aiVoiceTile,
                icon = Icons.Default.Mic,
                primary = true,
                modifier = Modifier.weight(1f),
                onClick = {
                    clickFeedback()
                    onOpenVoice()
                }
            )
            AiChoiceTile(
                title = strings.aiChatTile,
                icon = Icons.Default.Message,
                primary = false,
                modifier = Modifier.weight(1f),
                onClick = {
                    clickFeedback()
                    onOpenChat()
                }
            )
        }
    }
}

@Composable
private fun AiChoiceTile(
    title: String,
    icon: ImageVector,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background = if (primary) {
        Brush.verticalGradient(listOf(AiBlue, AiBlueDeep))
    } else {
        Brush.verticalGradient(listOf(AiCard, AiCard))
    }
    val iconBg = if (primary) Color.White.copy(alpha = 0.18f) else AiIconDark

    Column(
        modifier = modifier
            .height(116.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

private val AiBackground = Color(0xFF0B101E)
private val AiCard = Color(0xFF151D30)
private val AiIconDark = Color(0xFF1E2943)
private val AiBlue = Color(0xFF4F77E8)
private val AiBlueDeep = Color(0xFF456BDD)
private val AiMuted = Color(0xFF94A3B8)
