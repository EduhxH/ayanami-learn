package com.yourname.ayanami.learn.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.ayanami.learn.R
import com.yourname.ayanami.learn.data.model.LearnerProgress
import com.yourname.ayanami.learn.ui.components.ReiAssetImage
import com.yourname.ayanami.learn.ui.viewmodel.HomeViewModel
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    onOpenExercise: (String) -> Unit,
    onOpenAiMode: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val progress by viewModel.progress.collectAsState()

    Scaffold(
        containerColor = AyanamiCanvas,
        bottomBar = {
            AyanamiBottomBar(
                onOpenExercise = onOpenExercise,
                onOpenAiMode = onOpenAiMode,
                onOpenProfile = onOpenProfile,
                onOpenSettings = onOpenSettings
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AyanamiCanvas)
                .padding(paddingValues)
        ) {
            LearningPathHeader(
                progress = progress,
                onOpenSettings = onOpenSettings,
                onLogout = onLogout
            )
            LearningPath(
                onOpenExercise = onOpenExercise
            )
        }
    }
}

@Composable
private fun LearningPathHeader(
    progress: LearnerProgress,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricPill(
                icon = Icons.Default.Star,
                value = progress.streakDays.toString(),
                tint = AyanamiBlue,
                contentDescription = "Streak"
            )
            MetricPill(
                icon = Icons.Default.WorkspacePremium,
                value = progress.totalXp.toString(),
                tint = AyanamiSapphire,
                contentDescription = "XP"
            )
            MiniProfileButton(
                onOpenSettings = onOpenSettings,
                onLogout = onLogout
            )
        }

        GummyUnitBanner()
    }
}

@Composable
private fun MetricPill(
    icon: ImageVector,
    value: String,
    tint: Color,
    contentDescription: String
) {
    Row(
        modifier = Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.72f))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            color = AyanamiInk.copy(alpha = 0.62f),
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun MiniProfileButton(
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(AyanamiIce)
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center
        ) {
            ReiAssetImage(
                resId = R.drawable.reiayanamiplush,
                modifier = Modifier.size(38.dp),
                contentDescription = "Rei Ayanami"
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.68f))
                .clickable(onClick = onLogout),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Logout",
                tint = AyanamiMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun GummyUnitBanner() {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(98.dp)
                .offset(y = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AyanamiDeep)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(98.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AyanamiBlue)
                .padding(start = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "UNIT 1, PATH 1",
                    color = Color.White.copy(alpha = 0.86f),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                Text(
                    text = "Daily conversation",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 25.sp,
                    lineHeight = 29.sp
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .size(width = 92.dp, height = 98.dp)
                    .background(AyanamiDeep.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
private fun LearningPath(
    onOpenExercise: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        val containerWidth = maxWidth
        val density = LocalDensity.current
        val nodeSize = 76.dp
        val treasureSize = 92.dp
        val pathItems = learningPathItems
        val pathPoints = pathItems.filterIsInstance<PathItem.Lesson>()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1_150.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawLearningPath(
                    lessons = pathPoints,
                    nodeSize = nodeSize,
                    density = density
                )
            }

            ReiAssetImage(
                resId = R.drawable.reiayanamiplush,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 34.dp, y = 0.dp)
                    .size(width = 158.dp, height = 158.dp),
                contentDescription = "Rei Ayanami"
            )

            pathItems.forEach { item ->
                when (item) {
                    is PathItem.Lesson -> {
                        val x = with(density) {
                            ((containerWidth - nodeSize).toPx() * item.x).roundToInt()
                        }
                        val y = with(density) { item.y.toPx().roundToInt() }
                        LessonNode(
                            icon = item.skill.icon,
                            state = item.state,
                            modifier = Modifier
                                .offset { IntOffset(x, y) }
                                .size(nodeSize),
                            onClick = when {
                                item.state == LessonState.Locked -> ({})
                                else -> ({ onOpenExercise(item.skill.route) })
                            }
                        )
                    }
                    is PathItem.Treasure -> {
                        val x = with(density) {
                            ((containerWidth - treasureSize).toPx() * item.x).roundToInt()
                        }
                        val y = with(density) { item.y.toPx().roundToInt() }
                        TreasureChest(
                            modifier = Modifier
                                .offset { IntOffset(x, y) }
                                .size(treasureSize)
                        )
                    }
                    is PathItem.Mascot -> {
                        val x = with(density) {
                            ((containerWidth - 118.dp).toPx() * item.x).roundToInt()
                        }
                        val y = with(density) { item.y.toPx().roundToInt() }
                        ReiAssetImage(
                            resId = R.drawable.reiayanamiplush,
                            modifier = Modifier
                                .offset { IntOffset(x, y) }
                                .size(width = 118.dp, height = 140.dp),
                            contentDescription = "Rei Ayanami"
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawLearningPath(
    lessons: List<PathItem.Lesson>,
    nodeSize: Dp,
    density: androidx.compose.ui.unit.Density
) {
    if (lessons.size < 2) return

    val nodePx = with(density) { nodeSize.toPx() }
    val centers = lessons.map { lesson ->
        Offset(
            x = (size.width - nodePx) * lesson.x + nodePx / 2f,
            y = with(density) { lesson.y.toPx() } + nodePx / 2f
        )
    }

    val path = Path().apply {
        moveTo(centers.first().x, centers.first().y)
        centers.zipWithNext().forEach { (start, end) ->
            val controlY = (start.y + end.y) / 2f
            cubicTo(
                start.x,
                controlY,
                end.x,
                controlY,
                end.x,
                end.y
            )
        }
    }

    drawPath(
        path = path,
        color = AyanamiPathShadow,
        style = Stroke(width = 19.dp.toPx(), cap = StrokeCap.Round)
    )
    drawPath(
        path = path,
        color = AyanamiPath,
        style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
    )
}

@Composable
private fun LessonNode(
    icon: ImageVector,
    state: LessonState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val topColor = when (state) {
        LessonState.Active -> AyanamiBlue
        LessonState.Done -> AyanamiSapphire
        LessonState.Locked -> AyanamiLocked
    }
    val shadowColor = when (state) {
        LessonState.Active -> AyanamiDeep
        LessonState.Done -> AyanamiDeep
        LessonState.Locked -> AyanamiLockedShadow
    }
    val contentColor = if (state == LessonState.Locked) AyanamiMuted else Color.White

    Box(
        modifier = modifier.clickable(enabled = state != LessonState.Locked, onClick = onClick),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 8.dp)
                .clip(CircleShape)
                .background(shadowColor)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(bottom = 8.dp)
                .clip(CircleShape)
                .background(topColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(if (state == LessonState.Active) 38.dp else 34.dp)
            )
            if (state != LessonState.Locked) {
                NodeGloss()
            }
        }
    }
}

@Composable
private fun NodeGloss() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawArc(
            color = Color.White.copy(alpha = 0.28f),
            startAngle = 205f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(size.width * 0.17f, size.height * 0.12f),
            size = Size(size.width * 0.64f, size.height * 0.62f),
            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun TreasureChest(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawOval(
            color = AyanamiLockedShadow.copy(alpha = 0.55f),
            topLeft = Offset(w * 0.13f, h * 0.78f),
            size = Size(w * 0.78f, h * 0.18f)
        )
        drawRoundRect(
            color = AyanamiDeep,
            topLeft = Offset(w * 0.19f, h * 0.38f),
            size = Size(w * 0.62f, h * 0.43f),
            cornerRadius = CornerRadius(9.dp.toPx(), 9.dp.toPx())
        )
        drawRoundRect(
            color = AyanamiBlue,
            topLeft = Offset(w * 0.17f, h * 0.29f),
            size = Size(w * 0.66f, h * 0.24f),
            cornerRadius = CornerRadius(13.dp.toPx(), 13.dp.toPx())
        )
        drawRoundRect(
            color = AyanamiSapphire,
            topLeft = Offset(w * 0.16f, h * 0.47f),
            size = Size(w * 0.68f, h * 0.16f),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        drawRoundRect(
            color = AyanamiIce,
            topLeft = Offset(w * 0.44f, h * 0.43f),
            size = Size(w * 0.14f, h * 0.2f),
            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
        )
        drawCircle(
            color = AyanamiDeep,
            radius = w * 0.035f,
            center = Offset(w * 0.51f, h * 0.51f)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.36f),
            topLeft = Offset(w * 0.25f, h * 0.34f),
            size = Size(w * 0.2f, h * 0.05f),
            cornerRadius = CornerRadius(20f, 20f)
        )
    }
}

@Composable
private fun AyanamiBottomBar(
    onOpenExercise: (String) -> Unit,
    onOpenAiMode: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(Color(0xFF131B2F))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomNavIcon(
            icon = Icons.Default.Home,
            active = true,
            contentDescription = "Home"
        )
        BottomNavIcon(
            icon = Icons.Default.Inventory2,
            contentDescription = "Missions",
            onClick = { onOpenExercise("daily") }
        )
        BottomNavIcon(
            icon = Icons.Default.FitnessCenter,
            contentDescription = "Practice",
            onClick = { onOpenExercise("practice") }
        )
        BottomNavIcon(
            icon = Icons.Default.SmartToy,
            contentDescription = "AI",
            onClick = onOpenAiMode
        )
        BottomNavIcon(
            icon = Icons.Default.Shield,
            contentDescription = "Leagues",
            onClick = { onOpenExercise("league") }
        )
        BottomNavIcon(
            icon = Icons.Default.Person,
            contentDescription = "Profile",
            onClick = onOpenProfile
        )
        BottomNavIcon(
            icon = Icons.Default.Settings,
            contentDescription = "Settings",
            onClick = onOpenSettings
        )
    }
}

@Composable
private fun BottomNavIcon(
    icon: ImageVector,
    active: Boolean = false,
    contentDescription: String,
    onClick: (() -> Unit)? = null
) {
    val size = if (active) 58.dp else 48.dp
    val iconSize = if (active) 31.dp else 28.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(18.dp))
            .background(if (active) AyanamiIce else Color.Transparent)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) AyanamiBlue else AyanamiMuted,
            modifier = Modifier.size(iconSize)
        )
    }
}

private enum class LessonState {
    Active,
    Done,
    Locked
}

private enum class Skill(val icon: ImageVector, val route: String) {
    Reading(Icons.Default.MenuBook, "reading"),
    Listening(Icons.Default.Headphones, "listening"),
    Speaking(Icons.Default.Mic, "speaking"),
    Writing(Icons.Default.Edit, "writing")
}

@Immutable
private sealed interface PathItem {
    data class Lesson(
        val skill: Skill,
        val state: LessonState,
        val x: Float,
        val y: Dp
    ) : PathItem

    data class Treasure(
        val x: Float,
        val y: Dp
    ) : PathItem

    data class Mascot(
        val x: Float,
        val y: Dp
    ) : PathItem
}

private val learningPathItems = listOf(
    PathItem.Lesson(Skill.Reading, LessonState.Active, x = 0.69f, y = 56.dp),
    PathItem.Lesson(Skill.Listening, LessonState.Done, x = 0.53f, y = 186.dp),
    PathItem.Lesson(Skill.Speaking, LessonState.Done, x = 0.28f, y = 322.dp),
    PathItem.Treasure(x = 0.50f, y = 278.dp),
    PathItem.Lesson(Skill.Writing, LessonState.Done, x = 0.23f, y = 470.dp),
    PathItem.Mascot(x = 0.62f, y = 430.dp),
    PathItem.Lesson(Skill.Reading, LessonState.Done, x = 0.48f, y = 618.dp),
    PathItem.Lesson(Skill.Speaking, LessonState.Active, x = 0.68f, y = 762.dp),
    PathItem.Lesson(Skill.Listening, LessonState.Locked, x = 0.43f, y = 910.dp),
    PathItem.Lesson(Skill.Writing, LessonState.Locked, x = 0.20f, y = 1_036.dp)
)

private val AyanamiBlue = Color(0xFF5B8CFF)
private val AyanamiDeep = Color(0xFF3B6BE0)
private val AyanamiSapphire = Color(0xFF7AA4FF)
private val AyanamiCanvas = Color(0xFF0B101E)
private val AyanamiIce = Color(0xFF1A233A)
private val AyanamiPath = Color(0xFF1E2943)
private val AyanamiPathShadow = Color(0xFF111827)
private val AyanamiInk = Color(0xFFF8FAFC)
private val AyanamiMuted = Color(0xFF94A3B8)
private val AyanamiLocked = Color(0xFF1E293B)
private val AyanamiLockedShadow = Color(0xFF0F172A)
