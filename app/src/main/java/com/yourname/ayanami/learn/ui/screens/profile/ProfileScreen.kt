package com.yourname.ayanami.learn.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.ayanami.learn.R
import com.yourname.ayanami.learn.data.local.ActivityStatus
import com.yourname.ayanami.learn.ui.components.ReiAssetImage
import com.yourname.ayanami.learn.ui.localization.LocalAppStrings
import com.yourname.ayanami.learn.ui.viewmodel.HomeViewModel
import com.yourname.ayanami.learn.ui.viewmodel.SettingsViewModel

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val strings = LocalAppStrings.current
    val progress by homeViewModel.progress.collectAsState()
    val preferences by settingsViewModel.preferences.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileBg)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(strings.profile, color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = strings.settingsTab, tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(ProfileCard)
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(ProfileInner),
                contentAlignment = Alignment.Center
            ) {
                ReiAssetImage(
                    resId = R.drawable.reiayanamiplush,
                    modifier = Modifier.size(104.dp),
                    contentDescription = "Rei Ayanami"
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = preferences.profileName.ifBlank { strings.completeYourProfile },
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
            if (preferences.bio.isNotBlank()) {
                Text(
                    text = preferences.bio,
                    color = ProfileMuted,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusDot(status = preferences.activityStatus)
                Text(strings.statusLabel(preferences.activityStatus), color = ProfileMuted, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ProfileStat(
                icon = Icons.Default.Star,
                label = "Streak",
                value = progress.streakDays.toString(),
                modifier = Modifier.weight(1f)
            )
            ProfileStat(
                icon = Icons.Default.WorkspacePremium,
                label = "XP",
                value = progress.totalXp.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(ProfileCard)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(strings.nativeLanguage, color = ProfileMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
                preferences.nativeLanguage.displayName(preferences.nativeLanguage),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
            Text(strings.dailyGoal, color = ProfileMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
                strings.minutesPerDay(preferences.dailyGoalMinutes),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun ProfileStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(ProfileCard)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = ProfileBlue, modifier = Modifier.size(30.dp))
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(label, color = ProfileMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
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

private val ProfileBg = Color(0xFF0B101E)
private val ProfileCard = Color(0xFF151D30)
private val ProfileInner = Color(0xFF1A233A)
private val ProfileBlue = Color(0xFF5B8CFF)
private val ProfileMuted = Color(0xFF94A3B8)
