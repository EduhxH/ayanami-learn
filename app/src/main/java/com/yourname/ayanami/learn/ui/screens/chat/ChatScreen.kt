package com.yourname.ayanami.learn.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.ayanami.learn.R
import com.yourname.ayanami.learn.ui.components.ReiAssetImage
import com.yourname.ayanami.learn.ui.localization.LocalAppStrings
import com.yourname.ayanami.learn.ui.viewmodel.ChatMessage
import com.yourname.ayanami.learn.ui.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    onBack: (() -> Unit)? = null,
    onOpenVoice: (() -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val strings = LocalAppStrings.current
    val messages by viewModel.messages.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatBackground)
    ) {
        ChatHeader(
            title = "Rei Ayanami",
            subtitle = strings.ayanamiLearnAi,
            darkTheme = preferences.darkTheme,
            onBack = onBack,
            onToggleTheme = viewModel::toggleDarkTheme,
            onOpenVoice = onOpenVoice
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            item {
                ChatProfileIntro(
                    title = "Rei Ayanami",
                    subtitle = strings.ayanamiLearnAi,
                    actionLabel = strings.viewProfile,
                    onOpenProfile = onOpenProfile
                )
            }
            itemsIndexed(messages) { index, message ->
                val previous = messages.getOrNull(index - 1)
                val next = messages.getOrNull(index + 1)
                MessageBubble(
                    message = message,
                    isFirstInGroup = previous?.role != message.role,
                    isLastInGroup = next?.role != message.role
                )
            }
        }

        ChatComposer(
            value = inputText,
            placeholder = strings.messagePlaceholder,
            onValueChange = { inputText = it },
            onSend = {
                val text = inputText.trim()
                if (text.isNotEmpty()) {
                    viewModel.sendTextMessage(text)
                    inputText = ""
                }
            }
        )
    }
}

@Composable
private fun ChatHeader(
    title: String,
    subtitle: String,
    darkTheme: Boolean,
    onBack: (() -> Unit)?,
    onToggleTheme: () -> Unit,
    onOpenVoice: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(ChatBackground)
            .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ChatText, modifier = Modifier.size(28.dp))
            }
        }
        MiniReiAvatar(modifier = Modifier.size(34.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text(title, color = ChatText, fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 1)
            Text(subtitle, color = ChatBlueSoft, fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1)
        }
        IconButton(onClick = onToggleTheme, modifier = Modifier.size(42.dp)) {
            Icon(
                imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle theme",
                tint = ChatText,
                modifier = Modifier.size(23.dp)
            )
        }
        if (onOpenVoice != null) {
            IconButton(onClick = onOpenVoice, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Default.Call, contentDescription = "Voice call", tint = ChatText, modifier = Modifier.size(23.dp))
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 1.dp)
            .background(ChatDivider)
    )
}

@Composable
private fun ChatProfileIntro(
    title: String,
    subtitle: String,
    actionLabel: String,
    onOpenProfile: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 54.dp, bottom = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MiniReiAvatar(modifier = Modifier.size(96.dp))
        Text(title, color = ChatText, fontWeight = FontWeight.Black, fontSize = 23.sp, modifier = Modifier.padding(top = 12.dp))
        Text(subtitle, color = ChatMuted, fontWeight = FontWeight.Medium, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
        if (onOpenProfile != null) {
            Box(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(ChatBubbleAssistant)
                    .clickable(onClick = onOpenProfile)
                    .padding(horizontal = 17.dp, vertical = 8.dp)
            ) {
                Text(actionLabel, color = ChatText, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean
) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLastInGroup) 14.dp else 2.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            if (isLastInGroup) {
                MiniReiAvatar(modifier = Modifier.size(28.dp))
            } else {
                Spacer(modifier = Modifier.width(28.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier
                .widthIn(max = 282.dp)
                .clip(messageShape(isUser, isFirstInGroup, isLastInGroup))
                .background(if (isUser) ChatBlue else ChatBubbleAssistant)
                .padding(horizontal = 15.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.content,
                color = ChatText,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium
            )
            if (message.audioReplyUrl != null) {
                Text(
                    text = "Audio",
                    color = ChatText.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatComposer(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatBackground)
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(ChatComposerBg)
            .padding(start = 18.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 38.dp, max = 108.dp),
            maxLines = 4,
            textStyle = TextStyle(color = ChatText, fontSize = 15.sp, lineHeight = 20.sp),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            color = ChatMuted,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            }
        )
        IconButton(
            onClick = onSend,
            enabled = value.isNotBlank(),
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = if (value.isNotBlank()) ChatBlue else ChatMuted,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun MiniReiAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(ChatAvatarBg),
        contentAlignment = Alignment.Center
    ) {
        ReiAssetImage(
            resId = R.drawable.reiayanamiplush,
            modifier = Modifier.size(70.dp),
            contentDescription = "Rei Ayanami"
        )
    }
}

private fun messageShape(
    isUser: Boolean,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean
): RoundedCornerShape {
    val large = 18.dp
    val small = 6.dp
    return if (isUser) {
        RoundedCornerShape(
            topStart = large,
            topEnd = if (isFirstInGroup) large else small,
            bottomStart = large,
            bottomEnd = if (isLastInGroup) large else small
        )
    } else {
        RoundedCornerShape(
            topStart = if (isFirstInGroup) large else small,
            topEnd = large,
            bottomStart = if (isLastInGroup) large else small,
            bottomEnd = large
        )
    }
}

private val ChatBackground = Color.Black
private val ChatDivider = Color(0xFF1E293B)
private val ChatText = Color.White
private val ChatMuted = Color(0xFF9CA3AF)
private val ChatBlue = Color(0xFF4A90E2)
private val ChatBlueSoft = Color(0xFF8BC4FF)
private val ChatBubbleAssistant = Color(0xFF262626)
private val ChatComposerBg = Color(0xFF262626)
private val ChatAvatarBg = Color(0xFF0F172A)
