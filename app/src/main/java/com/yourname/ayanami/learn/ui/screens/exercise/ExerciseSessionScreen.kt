package com.yourname.ayanami.learn.ui.screens.exercise

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.ayanami.learn.data.local.NativeLanguage
import com.yourname.ayanami.learn.data.model.ExerciseItem
import com.yourname.ayanami.learn.data.model.ExerciseLesson
import com.yourname.ayanami.learn.data.model.VocabularyEntry
import com.yourname.ayanami.learn.ui.viewmodel.ExerciseUiState
import com.yourname.ayanami.learn.ui.viewmodel.ExerciseViewModel
import java.util.Locale

@Composable
fun ExerciseSessionScreen(
    skillKey: String,
    onBack: () -> Unit,
    onOpenChatSupport: () -> Unit,
    onOpenVoiceSupport: () -> Unit,
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    LaunchedEffect(skillKey) {
        viewModel.load(skillKey)
    }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var speechError by remember { mutableStateOf<String?>(null) }
    var isListeningForSpeech by remember { mutableStateOf(false) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                speechError = exerciseText(
                    uiState.lessonLanguage,
                    pt = "A permissão do microfone é necessária para exercícios de fala.",
                    uk = "Для вправ на мовлення потрібен дозвіл на мікрофон.",
                    ru = "Для упражнений на речь нужно разрешение микрофона."
                )
            }
        }
    )

    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.US
                engine?.setSpeechRate(0.88f)
            }
        }
        ttsEngine = engine

        val recognizer = if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
        recognizer?.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    speechError = null
                    isListeningForSpeech = true
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    isListeningForSpeech = false
                }

                override fun onError(error: Int) {
                    isListeningForSpeech = false
                    speechError = exerciseText(
                        uiState.lessonLanguage,
                        pt = "Não consegui ouvir uma frase clara. Tente novamente.",
                        uk = "Не вдалося почути чітке речення. Спробуйте ще раз.",
                        ru = "Не удалось услышать четкую фразу. Попробуйте снова."
                    )
                }

                override fun onResults(results: Bundle?) {
                    isListeningForSpeech = false
                    val transcript = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    viewModel.updateSpeechTranscript(transcript)
                    speechError = if (transcript.isBlank()) {
                        "Could not hear a clear sentence. Try again."
                    } else {
                        null
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            }
        )
        speechRecognizer = recognizer

        onDispose {
            engine?.stop()
            engine?.shutdown()
            recognizer?.destroy()
        }
    }

    fun playListeningText(text: String) {
        ttsEngine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ayanami_exercise_listening")
    }

    fun startSpeechRecognition() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            speechError = "Speech recognition is not available on this device."
            return
        }

        speechError = null
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    Scaffold(
        containerColor = AyanamiCanvas,
        bottomBar = {
            ExerciseBottomAction(
                uiState = uiState,
                onCheck = viewModel::checkCurrent,
                onRetry = viewModel::retryCurrent,
                onContinue = viewModel::continueLesson,
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AyanamiCanvas)
                .padding(paddingValues)
        ) {
            ExerciseTopBar(
                uiState = uiState,
                onBack = onBack
            )

            if (uiState.completed) {
                LessonCompleteContent(
                    uiState = uiState,
                    onBack = onBack,
                    onOpenChatSupport = onOpenChatSupport,
                    onOpenVoiceSupport = onOpenVoiceSupport
                )
            } else {
                ExerciseContent(
                    uiState = uiState,
                    isListeningForSpeech = isListeningForSpeech,
                    speechError = speechError,
                    onSelectChoice = viewModel::selectChoice,
                    onSelectMatchLeft = viewModel::selectMatchLeft,
                    onSelectMatchRight = viewModel::selectMatchRight,
                    onWritingChanged = viewModel::updateWritingAnswer,
                    onPlayListeningText = ::playListeningText,
                    onStartSpeechRecognition = ::startSpeechRecognition
                )
            }
        }
    }
}

@Composable
private fun ExerciseTopBar(
    uiState: ExerciseUiState,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close lesson",
                    tint = AyanamiMuted,
                    modifier = Modifier.size(32.dp)
                )
            }
            LinearProgressIndicator(
                progress = { if (uiState.completed) 1f else uiState.progress },
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(20.dp)),
                color = AyanamiBlue,
                trackColor = AyanamiLocked
            )
            Spacer(modifier = Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = AyanamiAccent)
                Text(
                    text = "3",
                    color = AyanamiAccent,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
        }

        uiState.lesson?.let { lesson ->
            Text(
                text = "${lesson.unitTitle} - ${lesson.cefrLevel} - ${lesson.skill.localizedTitle(uiState.lessonLanguage)} - ${lesson.targetMinutes} min",
                color = AyanamiMuted,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp
            )
            Text(
                text = lesson.title,
                color = AyanamiInk,
                fontWeight = FontWeight.Black,
                fontSize = 27.sp,
                lineHeight = 31.sp
            )
        }
    }
}

@Composable
private fun ExerciseContent(
    uiState: ExerciseUiState,
    isListeningForSpeech: Boolean,
    speechError: String?,
    onSelectChoice: (Int) -> Unit,
    onSelectMatchLeft: (String) -> Unit,
    onSelectMatchRight: (String) -> Unit,
    onWritingChanged: (String) -> Unit,
    onPlayListeningText: (String) -> Unit,
    onStartSpeechRecognition: () -> Unit
) {
    val item = uiState.currentItem ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        uiState.lesson?.let { lesson ->
            LessonContextPanel(lesson = lesson)
        }

        Text(
            text = item.prompt,
            color = AyanamiInk,
            fontWeight = FontWeight.Black,
            fontSize = 29.sp,
            lineHeight = 35.sp
        )
        Text(
            text = item.instruction,
            color = AyanamiMuted,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 23.sp
        )

        when (item) {
            is ExerciseItem.MultipleChoice -> ChoiceExercise(
                choices = item.choices,
                selectedIndex = uiState.selectedChoiceIndex,
                checked = uiState.checked,
                correctIndex = item.answerIndex,
                onSelectChoice = onSelectChoice
            )
            is ExerciseItem.Listening -> ListeningExercise(
                item = item,
                selectedIndex = uiState.selectedChoiceIndex,
                checked = uiState.checked,
                onPlayListeningText = onPlayListeningText,
                onSelectChoice = onSelectChoice
            )
            is ExerciseItem.MatchingPairs -> MatchingExercise(
                item = item,
                selectedLeft = uiState.selectedMatchLeft,
                matchedPairs = uiState.matchedPairs,
                onSelectLeft = onSelectMatchLeft,
                onSelectRight = onSelectMatchRight
            )
            is ExerciseItem.Writing -> WritingExercise(
                answer = uiState.writingAnswer,
                onWritingChanged = onWritingChanged,
                language = uiState.lessonLanguage
            )
            is ExerciseItem.Speaking -> SpeakingExercise(
                item = item,
                transcript = uiState.speechTranscript,
                isListeningForSpeech = isListeningForSpeech,
                speechError = speechError,
                language = uiState.lessonLanguage,
                onStartSpeechRecognition = onStartSpeechRecognition
            )
        }
    }
}

@Composable
private fun LessonContextPanel(lesson: ExerciseLesson) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AyanamiCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = lesson.topic,
            color = AyanamiInk,
            fontWeight = FontWeight.Black,
            fontSize = 17.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            lesson.grammarFocus.take(2).forEach { focus ->
                LearningChip(text = focus)
            }
        }
        lesson.vocabulary.take(2).forEach { entry ->
            VocabularyLine(entry = entry)
        }
    }
}

@Composable
private fun LearningChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AyanamiIce)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = AyanamiDeep,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun VocabularyLine(entry: VocabularyEntry) {
    Text(
        text = "${entry.word} - ${entry.meaning}: ${entry.example}",
        color = AyanamiMuted,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp
    )
}

@Composable
private fun ChoiceExercise(
    choices: List<String>,
    selectedIndex: Int?,
    checked: Boolean,
    correctIndex: Int,
    onSelectChoice: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        choices.forEachIndexed { index, choice ->
            val status = when {
                checked && index == correctIndex -> ButtonStatus.Correct
                checked && index == selectedIndex && index != correctIndex -> ButtonStatus.Wrong
                index == selectedIndex -> ButtonStatus.Selected
                else -> ButtonStatus.Neutral
            }
            GummyOptionButton(
                text = choice,
                status = status,
                onClick = { onSelectChoice(index) }
            )
        }
    }
}

@Composable
private fun ListeningExercise(
    item: ExerciseItem.Listening,
    selectedIndex: Int?,
    checked: Boolean,
    onPlayListeningText: (String) -> Unit,
    onSelectChoice: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(AyanamiBlue)
                    .clickable { onPlayListeningText(item.spokenText) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Play audio",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        ChoiceExercise(
            choices = item.choices,
            selectedIndex = selectedIndex,
            checked = checked,
            correctIndex = item.answerIndex,
            onSelectChoice = onSelectChoice
        )
    }
}

@Composable
private fun MatchingExercise(
    item: ExerciseItem.MatchingPairs,
    selectedLeft: String?,
    matchedPairs: Map<String, String>,
    onSelectLeft: (String) -> Unit,
    onSelectRight: (String) -> Unit
) {
    val rightOptions = item.pairs.map { it.right }.reversed()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item.pairs.forEach { pair ->
                val matched = matchedPairs.containsKey(pair.left)
                GummyOptionButton(
                    text = pair.left,
                    status = when {
                        matched -> ButtonStatus.Correct
                        selectedLeft == pair.left -> ButtonStatus.Selected
                        else -> ButtonStatus.Neutral
                    },
                    enabled = !matched,
                    onClick = { onSelectLeft(pair.left) }
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rightOptions.forEach { right ->
                val matched = matchedPairs.containsValue(right)
                GummyOptionButton(
                    text = right,
                    status = if (matched) ButtonStatus.Correct else ButtonStatus.Neutral,
                    enabled = !matched,
                    onClick = { onSelectRight(right) }
                )
            }
        }
    }
}

@Composable
private fun WritingExercise(
    answer: String,
    onWritingChanged: (String) -> Unit,
    language: NativeLanguage
) {
    OutlinedTextField(
        value = answer,
        onValueChange = onWritingChanged,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        placeholder = {
            Text(
                exerciseText(
                    language,
                    pt = "Digite sua resposta",
                    uk = "Введіть відповідь",
                    ru = "Введите ответ"
                )
            )
        },
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AyanamiBlue,
            unfocusedBorderColor = AyanamiLockedShadow,
            cursorColor = AyanamiBlue,
            focusedContainerColor = AyanamiCard,
            unfocusedContainerColor = AyanamiCard,
            focusedTextColor = AyanamiInk,
            unfocusedTextColor = AyanamiInk
        )
    )
}

@Composable
private fun SpeakingExercise(
    item: ExerciseItem.Speaking,
    transcript: String,
    isListeningForSpeech: Boolean,
    speechError: String?,
    language: NativeLanguage,
    onStartSpeechRecognition: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(AyanamiCard)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.targetPhrase,
                color = AyanamiInk,
                fontWeight = FontWeight.Black,
                fontSize = 25.sp,
                textAlign = TextAlign.Center
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(if (isListeningForSpeech) AyanamiAccent else AyanamiBlue)
                    .clickable(onClick = onStartSpeechRecognition),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Record speech",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        Text(
            text = when {
                isListeningForSpeech -> exerciseText(language, "Escutando...", "Слухаю...", "Слушаю...")
                transcript.isNotBlank() -> exerciseText(language, "Ouvi: $transcript", "Почуто: $transcript", "Распознано: $transcript")
                else -> exerciseText(
                    language,
                    pt = "Toque no microfone e repita a frase.",
                    uk = "Натисніть на мікрофон і повторіть речення.",
                    ru = "Нажмите на микрофон и повторите фразу."
                )
            },
            color = AyanamiMuted,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        speechError?.let {
            Text(
                text = it,
                color = AyanamiAccent,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ExerciseBottomAction(
    uiState: ExerciseUiState,
    onCheck: () -> Unit,
    onRetry: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    if (uiState.completed) return

    val checked = uiState.checked
    val isCorrect = uiState.isCorrect
    val enabled = uiState.canCheck()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    checked && isCorrect == true -> AyanamiCorrectBg
                    checked && isCorrect == false -> AyanamiWrongBg
                    else -> AyanamiCanvas
                }
            )
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (checked) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isCorrect == true) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (isCorrect == true) AyanamiCorrect else AyanamiAccent,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isCorrect == true) {
                        exerciseText(uiState.lessonLanguage, "Correto!", "Правильно!", "Правильно!")
                    } else {
                        exerciseText(uiState.lessonLanguage, "Tente novamente", "Спробуйте ще раз", "Попробуйте снова")
                    },
                    color = if (isCorrect == true) AyanamiCorrect else AyanamiAccent,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                )
            }
        }

        GummyPrimaryButton(
            text = when {
                checked && isCorrect == true -> exerciseText(uiState.lessonLanguage, "CONTINUAR", "ПРОДОВЖИТИ", "ПРОДОЛЖИТЬ")
                checked -> exerciseText(uiState.lessonLanguage, "TENTAR DE NOVO", "СПРОБУВАТИ ЩЕ", "ПОПРОБОВАТЬ СНОВА")
                else -> exerciseText(uiState.lessonLanguage, "VERIFICAR", "ПЕРЕВІРИТИ", "ПРОВЕРИТЬ")
            },
            enabled = enabled || checked,
            onClick = {
                when {
                    checked && isCorrect == true -> onContinue()
                    checked -> onRetry()
                    else -> onCheck()
                }
            }
        )
    }
}

private fun ExerciseUiState.canCheck(): Boolean {
    return when (val item = currentItem) {
        is ExerciseItem.MultipleChoice -> selectedChoiceIndex != null
        is ExerciseItem.Listening -> selectedChoiceIndex != null
        is ExerciseItem.Writing -> writingAnswer.isNotBlank()
        is ExerciseItem.Speaking -> speechTranscript.isNotBlank()
        is ExerciseItem.MatchingPairs -> matchedPairs.size == item.pairs.size
        null -> false
    }
}

@Composable
private fun LessonCompleteContent(
    uiState: ExerciseUiState,
    onBack: () -> Unit,
    onOpenChatSupport: () -> Unit,
    onOpenVoiceSupport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = AyanamiBlue,
            modifier = Modifier.size(82.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = exerciseText(
                uiState.lessonLanguage,
                pt = "Lição concluída",
                uk = "Урок завершено",
                ru = "Урок завершен"
            ),
            color = AyanamiInk,
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = exerciseText(
                uiState.lessonLanguage,
                pt = "${uiState.correctExerciseIds.size}/${uiState.totalItems} corretas - ${uiState.xpEarned} XP",
                uk = "${uiState.correctExerciseIds.size}/${uiState.totalItems} правильних - ${uiState.xpEarned} XP",
                ru = "${uiState.correctExerciseIds.size}/${uiState.totalItems} правильных - ${uiState.xpEarned} XP"
            ),
            color = AyanamiMuted,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(28.dp))
        GummyPrimaryButton(
            text = exerciseText(uiState.lessonLanguage, "VOLTAR À TRILHA", "НАЗАД ДО ШЛЯХУ", "НАЗАД К ПУТИ"),
            onClick = onBack
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SupportButton(
                text = exerciseText(uiState.lessonLanguage, "Ajuda por texto", "Допомога текстом", "Помощь текстом"),
                icon = Icons.Default.Chat,
                modifier = Modifier.weight(1f),
                onClick = onOpenChatSupport
            )
            SupportButton(
                text = exerciseText(uiState.lessonLanguage, "Ajuda por voz", "Допомога голосом", "Помощь голосом"),
                icon = Icons.Default.Headphones,
                modifier = Modifier.weight(1f),
                onClick = onOpenVoiceSupport
            )
        }
    }
}

@Composable
private fun GummyOptionButton(
    text: String,
    status: ButtonStatus,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = status.colors()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.shadow)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 6.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.top),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = colors.content,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun GummyPrimaryButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val top = if (enabled) AyanamiBlue else AyanamiLocked
    val shadow = if (enabled) AyanamiDeep else AyanamiLockedShadow
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 7.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(shadow)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 7.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(top),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun SupportButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AyanamiCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = AyanamiBlue, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = AyanamiInk, fontWeight = FontWeight.Black)
    }
}

private enum class ButtonStatus {
    Neutral,
    Selected,
    Correct,
    Wrong
}

private data class ButtonColors(
    val top: Color,
    val shadow: Color,
    val content: Color
)

private fun ButtonStatus.colors(): ButtonColors {
    return when (this) {
        ButtonStatus.Neutral -> ButtonColors(AyanamiCard, AyanamiLockedShadow, AyanamiInk)
        ButtonStatus.Selected -> ButtonColors(AyanamiIce, AyanamiPath, AyanamiDeep)
        ButtonStatus.Correct -> ButtonColors(AyanamiCorrectBg, AyanamiCorrect.copy(alpha = 0.42f), AyanamiCorrect)
        ButtonStatus.Wrong -> ButtonColors(AyanamiWrongBg, AyanamiAccent.copy(alpha = 0.42f), AyanamiAccent)
    }
}

private fun exerciseText(
    language: NativeLanguage,
    pt: String,
    uk: String,
    ru: String
): String {
    return when (language) {
        NativeLanguage.Portuguese -> pt
        NativeLanguage.Ukrainian -> uk
        NativeLanguage.Russian -> ru
    }
}

private val AyanamiBlue = Color(0xFF5B8CFF)
private val AyanamiDeep = Color(0xFF3F63CC)
private val AyanamiCanvas = Color(0xFF0B101E)
private val AyanamiCard = Color(0xFF151D30)
private val AyanamiIce = Color(0xFF1A233A)
private val AyanamiPath = Color(0xFF263454)
private val AyanamiInk = Color(0xFFF8FAFC)
private val AyanamiMuted = Color(0xFF94A3B8)
private val AyanamiLocked = Color(0xFF1E293B)
private val AyanamiLockedShadow = Color(0xFF0F172A)
private val AyanamiAccent = Color(0xFFFF6B7A)
private val AyanamiCorrect = Color(0xFF5B8CFF)
private val AyanamiCorrectBg = Color(0xFF10233F)
private val AyanamiWrongBg = Color(0xFF351925)
