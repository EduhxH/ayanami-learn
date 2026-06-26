package com.yourname.ayanami.learn.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.ayanami.learn.R
import com.yourname.ayanami.learn.data.model.CurriculumLesson
import com.yourname.ayanami.learn.data.model.CurriculumUnit
import com.yourname.ayanami.learn.data.model.ExerciseSkill
import com.yourname.ayanami.learn.data.model.LearnerProgress
import com.yourname.ayanami.learn.ui.components.ReiAssetImage
import com.yourname.ayanami.learn.ui.feedback.rememberClickFeedback
import com.yourname.ayanami.learn.ui.localization.LocalAppStrings
import com.yourname.ayanami.learn.ui.viewmodel.HomeViewModel
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

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
    val curriculum by viewModel.curriculum.collectAsState()

    val completedLessonIds = progress.completedLessonIds
    val activeLessonId = remember(curriculum, completedLessonIds) {
        curriculum.flatMap { it.lessons }.firstOrNull { it.lessonId !in completedLessonIds }?.lessonId
    }
    val activeUnit = remember(curriculum, activeLessonId) {
        curriculum.firstOrNull { unit -> unit.lessons.any { it.lessonId == activeLessonId } }
            ?: curriculum.lastOrNull()
    }

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
                activeUnit = activeUnit,
                onOpenSettings = onOpenSettings,
                onLogout = onLogout
            )
            LearningPath(
                curriculum = curriculum,
                completedLessonIds = completedLessonIds,
                activeLessonId = activeLessonId,
                onOpenExercise = onOpenExercise
            )
        }
    }
}

@Composable
private fun LearningPathHeader(
    progress: LearnerProgress,
    activeUnit: CurriculumUnit?,
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

        GummyUnitBanner(
            title = activeUnit?.title?.uppercase() ?: "UNIT 1",
            subtitle = activeUnit?.subtitle ?: "Daily conversation"
        )
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
private fun GummyUnitBanner(
    title: String,
    subtitle: String
) {
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
                    text = title,
                    color = Color.White.copy(alpha = 0.86f),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                Text(
                    text = subtitle,
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
    curriculum: List<CurriculumUnit>,
    completedLessonIds: Set<String>,
    activeLessonId: String?,
    onOpenExercise: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val strings = LocalAppStrings.current
    val clickFeedback = rememberClickFeedback()
    val configuration = LocalConfiguration.current

    val entries = remember(curriculum, completedLessonIds, activeLessonId) {
        buildPathEntries(curriculum, completedLessonIds, activeLessonId)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        val density = LocalDensity.current
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val nodeSize = 76.dp
        val treasureSize = 92.dp
        val spacing = 132.dp
        val topPadding = 36.dp
        val nodePx = with(density) { nodeSize.toPx() }
        val viewportCenterPx = with(density) { configuration.screenHeightDp.dp.toPx() } / 2f

        val centers = remember(entries.size, containerWidthPx, density) {
            List(entries.size) { index ->
                val fraction = (0.5f + WAVE_AMPLITUDE * sin(index * WAVE_FREQUENCY + WAVE_PHASE))
                    .coerceIn(0.18f, 0.82f)
                val centerX = containerWidthPx * fraction
                val centerY = with(density) { (topPadding + spacing * index).toPx() } + nodePx / 2f
                Offset(centerX, centerY)
            }
        }

        val totalHeight = topPadding + spacing * (entries.size - 1).coerceAtLeast(0) + nodeSize + 130.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawLearningPath(centers)
            }

            ReiAssetImage(
                resId = R.drawable.reiayanamiplush,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 22.dp, y = 0.dp)
                    .size(150.dp),
                contentDescription = "Rei Ayanami"
            )

            entries.forEachIndexed { index, entry ->
                val center = centers[index]
                when (entry) {
                    is PathEntry.LessonEntry -> {
                        val offsetX = (center.x - nodePx / 2f).roundToInt()
                        val offsetY = (center.y - nodePx / 2f).roundToInt()
                        LessonNode(
                            icon = entry.lesson.skill.pathIcon(),
                            state = entry.state,
                            startLabel = if (entry.state == LessonState.Active) strings.startLesson else null,
                            modifier = Modifier
                                .offset { IntOffset(offsetX, offsetY) }
                                .size(nodeSize)
                                .scrollReveal(scrollState, center.y, viewportCenterPx),
                            onClick = {
                                if (entry.state != LessonState.Locked) {
                                    clickFeedback()
                                    onOpenExercise(entry.lesson.route)
                                }
                            }
                        )
                    }
                    is PathEntry.TreasureEntry -> {
                        val treasurePx = with(density) { treasureSize.toPx() }
                        val offsetX = (center.x - treasurePx / 2f).roundToInt()
                        val offsetY = (center.y - treasurePx / 2f).roundToInt()
                        TreasureChest(
                            modifier = Modifier
                                .offset { IntOffset(offsetX, offsetY) }
                                .size(treasureSize)
                                .scrollReveal(scrollState, center.y, viewportCenterPx)
                        )
                    }
                }
            }
        }
    }
}

private fun buildPathEntries(
    curriculum: List<CurriculumUnit>,
    completedLessonIds: Set<String>,
    activeLessonId: String?
): List<PathEntry> {
    return buildList {
        curriculum.forEachIndexed { unitIndex, unit ->
            unit.lessons.forEach { lesson ->
                val state = when {
                    lesson.lessonId in completedLessonIds -> LessonState.Done
                    lesson.lessonId == activeLessonId -> LessonState.Active
                    else -> LessonState.Locked
                }
                add(PathEntry.LessonEntry(lesson, state))
            }
            if (unitIndex < curriculum.lastIndex) {
                val unitCleared = unit.lessons.all { it.lessonId in completedLessonIds }
                add(PathEntry.TreasureEntry(unlocked = unitCleared))
            }
        }
    }
}

private fun DrawScope.drawLearningPath(centers: List<Offset>) {
    if (centers.size < 2) return

    val path = Path().apply {
        moveTo(centers.first().x, centers.first().y)
        centers.zipWithNext().forEach { (start, end) ->
            val controlY = (start.y + end.y) / 2f
            cubicTo(start.x, controlY, end.x, controlY, end.x, end.y)
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
    startLabel: String?,
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

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "node-press"
    )

    Box(
        modifier = modifier.clickable(
            interactionSource = interaction,
            indication = null,
            enabled = state != LessonState.Locked,
            onClick = onClick
        ),
        contentAlignment = Alignment.TopCenter
    ) {
        if (state == LessonState.Active) {
            ActivePulse(modifier = Modifier.matchParentSize())
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                .offset(y = 8.dp)
                .clip(CircleShape)
                .background(shadowColor)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
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
        if (startLabel != null) {
            StartBubble(
                label = startLabel,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-32).dp)
            )
        }
    }
}

@Composable
private fun ActivePulse(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "node-pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse-scale"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse-alpha"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(CircleShape)
            .background(AyanamiBlue)
    )
}

@Composable
private fun StartBubble(label: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "start-bubble")
    val lift by transition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(820, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bubble-lift"
    )
    Box(
        modifier = modifier
            .graphicsLayer { translationY = lift }
            .clip(RoundedCornerShape(11.dp))
            .background(Color.White)
            .padding(horizontal = 13.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = AyanamiDeep,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp
        )
    }
}

private fun Modifier.scrollReveal(
    scrollState: ScrollState,
    centerY: Float,
    viewportCenterPx: Float
): Modifier = graphicsLayer {
    if (viewportCenterPx <= 0f) return@graphicsLayer
    val onScreenY = centerY - scrollState.value
    val distance = (abs(onScreenY - viewportCenterPx) / viewportCenterPx).coerceIn(0f, 1f)
    val nodeScale = 1f - 0.12f * distance
    scaleX = nodeScale
    scaleY = nodeScale
    alpha = 1f - 0.32f * distance
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
    val clickFeedback = rememberClickFeedback()
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(18.dp))
            .background(if (active) AyanamiIce else Color.Transparent)
            .clickable(enabled = onClick != null) {
                clickFeedback()
                onClick?.invoke()
            },
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

private fun ExerciseSkill.pathIcon(): ImageVector {
    return when (this) {
        ExerciseSkill.Reading -> Icons.Default.MenuBook
        ExerciseSkill.Listening -> Icons.Default.Headphones
        ExerciseSkill.Speaking -> Icons.Default.Mic
        ExerciseSkill.Writing -> Icons.Default.Edit
        ExerciseSkill.Daily -> Icons.Default.Inventory2
        ExerciseSkill.Practice -> Icons.Default.FitnessCenter
        ExerciseSkill.League -> Icons.Default.Shield
    }
}

private enum class LessonState {
    Active,
    Done,
    Locked
}

private sealed interface PathEntry {
    data class LessonEntry(val lesson: CurriculumLesson, val state: LessonState) : PathEntry
    data class TreasureEntry(val unlocked: Boolean) : PathEntry
}

private const val WAVE_AMPLITUDE = 0.30f
private const val WAVE_FREQUENCY = 0.95f
private const val WAVE_PHASE = 0.6f

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
