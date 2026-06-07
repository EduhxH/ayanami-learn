package com.yourname.ayanami.learn.data.repository

import com.yourname.ayanami.learn.data.local.NativeLanguage
import com.yourname.ayanami.learn.data.model.ContentSourceRef
import com.yourname.ayanami.learn.data.model.ExerciseItem
import com.yourname.ayanami.learn.data.model.ExerciseLesson
import com.yourname.ayanami.learn.data.model.ExerciseSkill
import com.yourname.ayanami.learn.data.model.MatchPair
import com.yourname.ayanami.learn.data.model.SourceUsage
import com.yourname.ayanami.learn.data.model.VocabularyEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor() {
    fun getLesson(skillKey: String, nativeLanguage: NativeLanguage = NativeLanguage.Portuguese): ExerciseLesson {
        val skill = ExerciseSkill.fromKey(skillKey)
        val lesson = when (skill) {
            ExerciseSkill.Reading -> readingLesson
            ExerciseSkill.Listening -> listeningLesson
            ExerciseSkill.Speaking -> speakingLesson
            ExerciseSkill.Writing -> writingLesson
            ExerciseSkill.Daily -> dailyLesson
            ExerciseSkill.Practice -> practiceLesson
            ExerciseSkill.League -> leagueLesson
        }
        return lesson.localized(nativeLanguage)
    }

    private val ellloSource = ContentSourceRef(
        provider = "ELLLO",
        url = "https://www.elllo.org/",
        usage = SourceUsage.CurriculumReference,
        notes = "Use listening lesson structure: short natural audio, transcript, vocabulary and quiz."
    )

    private val britishCouncilSource = ContentSourceRef(
        provider = "British Council LearnEnglish",
        url = "https://learnenglish.britishcouncil.org/free-resources/listening",
        usage = SourceUsage.CurriculumReference,
        notes = "Use CEFR progression and skill organization across listening, reading, writing and speaking."
    )

    private val cambridgeEnglishSource = ContentSourceRef(
        provider = "Cambridge English",
        url = "https://www.cambridgeenglish.org/learning-english/",
        usage = SourceUsage.CurriculumReference,
        notes = "Use exam-style activity patterns, level-test thinking and skill-specific task formats."
    )

    private val cambridgeDictionarySource = ContentSourceRef(
        provider = "Cambridge Dictionary API",
        url = "https://dictionary.cambridge.org/pt/develop.html",
        usage = SourceUsage.ApiCandidate,
        notes = "Future API integration for definitions, pronunciations, examples and dictionary-backed vocabulary."
    )

    private val openAiSpeechSource = ContentSourceRef(
        provider = "OpenAI Speech to Text",
        url = "https://developers.openai.com/api/docs/guides/speech-to-text",
        usage = SourceUsage.ApiCandidate,
        notes = "Optional transcription provider for converting licensed/custom audio into exercise data."
    )

    private val speechLabSource = ContentSourceRef(
        provider = "Speech Lab IITM ASR",
        url = "https://speech-lab-iitm.github.io/SpeechLab/api.html",
        usage = SourceUsage.ApiCandidate,
        notes = "Open ASR candidate for English transcription and VTT generation."
    )

    private val kaggleSource = ContentSourceRef(
        provider = "Kaggle English audio dataset",
        url = "https://www.kaggle.com/datasets/siddhartha22i258/english-audio-dataset-with-transcription",
        usage = SourceUsage.DatasetCandidate,
        notes = "Candidate for local pipeline tests after license review."
    )

    private val telusSource = ContentSourceRef(
        provider = "TELUS English UK remote speech dataset",
        url = "https://www.telusdigital.com/solutions/data-for-ai-training/off-the-shelf-data/english-uk-remote-speech-dataset",
        usage = SourceUsage.LicensedDatasetCandidate,
        notes = "Commercial/licensed candidate for high-quality speech and accent evaluation."
    )

    private val dailyRoutineVocabulary = listOf(
        VocabularyEntry(
            word = "routine",
            meaning = "rotina",
            example = "My morning routine is simple.",
            pronunciationHint = "roo-TEEN"
        ),
        VocabularyEntry(
            word = "usually",
            meaning = "geralmente",
            example = "I usually study after school."
        ),
        VocabularyEntry(
            word = "ready",
            meaning = "pronto",
            example = "I am ready for the lesson."
        ),
        VocabularyEntry(
            word = "practice",
            meaning = "praticar",
            example = "I practice English every day."
        )
    )

    private val learningVocabulary = listOf(
        VocabularyEntry(
            word = "repeat",
            meaning = "repetir",
            example = "Please repeat the sentence."
        ),
        VocabularyEntry(
            word = "answer",
            meaning = "resposta",
            example = "Write a short answer."
        ),
        VocabularyEntry(
            word = "listen",
            meaning = "ouvir",
            example = "Listen to the sentence first."
        )
    )

    private val readingLesson = ExerciseLesson(
        id = "unit_1_reading_daily_conversation",
        unitTitle = "Unit 1, Path 1",
        skill = ExerciseSkill.Reading,
        title = "Read everyday routines",
        targetMinutes = 5,
        xpReward = 50,
        cefrLevel = "A1",
        topic = "Daily routine and study habits",
        learningObjectives = listOf(
            "Understand short present-simple sentences about routines.",
            "Identify familiar verbs and time expressions.",
            "Choose natural answers to simple questions."
        ),
        grammarFocus = listOf("present simple", "subject + verb + object", "short answers"),
        vocabulary = dailyRoutineVocabulary,
        sourceInfluences = listOf(britishCouncilSource, cambridgeEnglishSource, cambridgeDictionarySource),
        items = listOf(
            ExerciseItem.MultipleChoice(
                id = "reading_1",
                prompt = "Read the sentence and choose the meaning.",
                instruction = "I usually study English after school.",
                choices = listOf(
                    "Eu geralmente estudo ingles depois da escola.",
                    "Eu compro um livro antes da escola.",
                    "Eu estudo ingles apenas no domingo.",
                    "Eu escuto musica depois da aula."
                ),
                answerIndex = 0
            ),
            ExerciseItem.MultipleChoice(
                id = "reading_2",
                prompt = "Choose the sentence that matches the idea.",
                instruction = "A short daily habit",
                choices = listOf(
                    "I practice English for ten minutes every day.",
                    "Yesterday water quickly book.",
                    "She are practice after school.",
                    "I English every morning am."
                ),
                answerIndex = 0
            ),
            ExerciseItem.MatchingPairs(
                id = "reading_3",
                prompt = "Match the reading words.",
                instruction = "Connect each English word with its meaning.",
                pairs = listOf(
                    MatchPair("routine", "rotina"),
                    MatchPair("usually", "geralmente"),
                    MatchPair("practice", "praticar")
                )
            ),
            ExerciseItem.MultipleChoice(
                id = "reading_4",
                prompt = "Choose the best response.",
                instruction = "Question: When do you study English?",
                choices = listOf(
                    "I study after school.",
                    "It is a notebook.",
                    "She likes blue.",
                    "Water is ready."
                ),
                answerIndex = 0
            )
        )
    )

    private val listeningLesson = ExerciseLesson(
        id = "unit_1_listening_daily_conversation",
        unitTitle = "Unit 1, Path 1",
        skill = ExerciseSkill.Listening,
        title = "Listen to short routines",
        targetMinutes = 6,
        xpReward = 60,
        cefrLevel = "A1",
        topic = "Slow clear speech about daily learning",
        learningObjectives = listOf(
            "Recognize familiar words in slow, clear speech.",
            "Connect short audio to written choices.",
            "Build confidence with transcript-style listening."
        ),
        grammarFocus = listOf("present simple", "be + adjective", "time expressions"),
        vocabulary = dailyRoutineVocabulary + learningVocabulary,
        sourceInfluences = listOf(ellloSource, britishCouncilSource, openAiSpeechSource, speechLabSource),
        items = listOf(
            ExerciseItem.Listening(
                id = "listening_1",
                prompt = "Listen and choose what you hear.",
                instruction = "Tap the speaker. The sentence is short and clear.",
                spokenText = "I am ready for English practice.",
                choices = listOf(
                    "I am ready for English practice.",
                    "I am reading an English story.",
                    "I am tired after English class.",
                    "I write English every morning."
                ),
                answerIndex = 0
            ),
            ExerciseItem.Listening(
                id = "listening_2",
                prompt = "Listen for the routine.",
                instruction = "Choose the sentence with the same audio.",
                spokenText = "I usually study after school.",
                choices = listOf(
                    "I usually sleep after school.",
                    "I usually study after school.",
                    "She usually studies before school.",
                    "I study only at night."
                ),
                answerIndex = 1
            ),
            ExerciseItem.MatchingPairs(
                id = "listening_3",
                prompt = "Review the words you heard.",
                instruction = "Match each listening word with its meaning.",
                pairs = listOf(
                    MatchPair("listen", "ouvir"),
                    MatchPair("ready", "pronto"),
                    MatchPair("repeat", "repetir")
                )
            ),
            ExerciseItem.Listening(
                id = "listening_4",
                prompt = "Listen and choose the best transcript.",
                instruction = "Focus on the final word.",
                spokenText = "Please repeat the answer.",
                choices = listOf(
                    "Please repeat the answer.",
                    "Please read the answer.",
                    "Please repeat the lesson.",
                    "Please write the answer."
                ),
                answerIndex = 0
            )
        )
    )

    private val speakingLesson = ExerciseLesson(
        id = "unit_1_speaking_daily_conversation",
        unitTitle = "Unit 1, Path 1",
        skill = ExerciseSkill.Speaking,
        title = "Speak clear routine sentences",
        targetMinutes = 6,
        xpReward = 60,
        cefrLevel = "A1",
        topic = "Pronouncing daily study sentences",
        learningObjectives = listOf(
            "Repeat short sentences with clear rhythm.",
            "Use first-person routine sentences.",
            "Build speaking confidence with low-pressure checks."
        ),
        grammarFocus = listOf("I am", "I usually", "present simple"),
        vocabulary = dailyRoutineVocabulary,
        sourceInfluences = listOf(britishCouncilSource, cambridgeEnglishSource, speechLabSource, kaggleSource, telusSource),
        items = listOf(
            ExerciseItem.Speaking(
                id = "speaking_1",
                prompt = "Say this sentence aloud.",
                instruction = "Use the microphone. The app compares your speech with the target.",
                targetPhrase = "I am ready for English practice.",
                acceptedPhrases = listOf("I am ready for English practice", "I'm ready for English practice")
            ),
            ExerciseItem.Speaking(
                id = "speaking_2",
                prompt = "Say this routine sentence.",
                instruction = "Speak naturally, then check your result.",
                targetPhrase = "I usually study after school.",
                acceptedPhrases = listOf("I usually study after school")
            ),
            ExerciseItem.MultipleChoice(
                id = "speaking_3",
                prompt = "Choose the sentence with natural word order.",
                instruction = "This supports your speaking rhythm.",
                choices = listOf(
                    "I practice English every day.",
                    "I English every day practice.",
                    "Practice I every English day.",
                    "Every day English practice I."
                ),
                answerIndex = 0
            )
        )
    )

    private val writingLesson = ExerciseLesson(
        id = "unit_1_writing_daily_conversation",
        unitTitle = "Unit 1, Path 1",
        skill = ExerciseSkill.Writing,
        title = "Write about your routine",
        targetMinutes = 6,
        xpReward = 60,
        cefrLevel = "A1",
        topic = "Guided sentence building",
        learningObjectives = listOf(
            "Translate simple routine sentences.",
            "Build correct word order.",
            "Use present simple for daily habits."
        ),
        grammarFocus = listOf("present simple", "adverbs of frequency", "sentence word order"),
        vocabulary = dailyRoutineVocabulary + learningVocabulary,
        sourceInfluences = listOf(britishCouncilSource, cambridgeEnglishSource, cambridgeDictionarySource),
        items = listOf(
            ExerciseItem.Writing(
                id = "writing_1",
                prompt = "Translate this sentence.",
                instruction = "Eu geralmente estudo depois da escola.",
                acceptedAnswers = listOf("I usually study after school")
            ),
            ExerciseItem.Writing(
                id = "writing_2",
                prompt = "Write a sentence with these words.",
                instruction = "I / practice / English / every day",
                acceptedAnswers = listOf("I practice English every day")
            ),
            ExerciseItem.MultipleChoice(
                id = "writing_3",
                prompt = "Choose the correct sentence.",
                instruction = "Daily routine",
                choices = listOf(
                    "I usually study after school.",
                    "I usually studies after school.",
                    "I am usually study after school.",
                    "I study usually after school am."
                ),
                answerIndex = 0
            ),
            ExerciseItem.Writing(
                id = "writing_4",
                prompt = "Answer in English.",
                instruction = "When do you practice English?",
                acceptedAnswers = listOf(
                    "I practice English every day",
                    "I practice after school",
                    "I usually practice after school"
                )
            )
        )
    )

    private val dailyLesson = ExerciseLesson(
        id = "daily_balanced_lesson",
        unitTitle = "Daily Plan",
        skill = ExerciseSkill.Daily,
        title = "Balanced daily practice",
        targetMinutes = 12,
        xpReward = 100,
        cefrLevel = "A1",
        topic = "Daily routine mixed skills",
        learningObjectives = listOf(
            "Review one reading pattern.",
            "Hear one short routine sentence.",
            "Write one correct routine sentence.",
            "Say one sentence clearly."
        ),
        grammarFocus = listOf("present simple", "I am", "I usually"),
        vocabulary = dailyRoutineVocabulary + learningVocabulary,
        sourceInfluences = listOf(ellloSource, britishCouncilSource, cambridgeEnglishSource, cambridgeDictionarySource),
        items = listOf(
            readingLesson.items[0],
            listeningLesson.items[0],
            writingLesson.items[0],
            speakingLesson.items[0]
        )
    )

    private val practiceLesson = ExerciseLesson(
        id = "practice_review_lesson",
        unitTitle = "Practice",
        skill = ExerciseSkill.Practice,
        title = "Review weak points",
        targetMinutes = 9,
        xpReward = 80,
        cefrLevel = "A1",
        topic = "Mixed review from current unit",
        learningObjectives = listOf(
            "Reinforce vocabulary from the unit.",
            "Compare similar listening choices.",
            "Recover from common word-order mistakes."
        ),
        grammarFocus = listOf("word order", "present simple", "vocabulary recall"),
        vocabulary = dailyRoutineVocabulary + learningVocabulary,
        sourceInfluences = listOf(ellloSource, britishCouncilSource, cambridgeEnglishSource),
        items = listOf(
            writingLesson.items[1],
            listeningLesson.items[1],
            readingLesson.items[2],
            speakingLesson.items[1]
        )
    )

    private val leagueLesson = ExerciseLesson(
        id = "league_xp_challenge",
        unitTitle = "League Challenge",
        skill = ExerciseSkill.League,
        title = "Fast XP challenge",
        targetMinutes = 7,
        xpReward = 110,
        cefrLevel = "A1",
        topic = "Fast mixed skill check",
        learningObjectives = listOf(
            "Answer quickly without losing accuracy.",
            "Recognize routine vocabulary across skills.",
            "Strengthen recall under light pressure."
        ),
        grammarFocus = listOf("present simple", "routine vocabulary", "short answers"),
        vocabulary = dailyRoutineVocabulary + learningVocabulary,
        sourceInfluences = listOf(britishCouncilSource, cambridgeEnglishSource, kaggleSource),
        items = listOf(
            readingLesson.items[3],
            writingLesson.items[2],
            listeningLesson.items[3],
            speakingLesson.items[2]
        )
    )

    private fun ExerciseLesson.localized(language: NativeLanguage): ExerciseLesson {
        return copy(
            unitTitle = localizedUnitTitle(id, language),
            title = localizedLessonTitle(id, language),
            topic = localizedTopic(id, language),
            learningObjectives = localizedObjectives(id, language),
            grammarFocus = localizedGrammar(language),
            vocabulary = vocabulary.map { entry ->
                entry.copy(meaning = vocabularyMeaning(entry.word, language))
            },
            items = items.map { item -> item.localized(language) }
        )
    }

    private fun ExerciseItem.localized(language: NativeLanguage): ExerciseItem {
        val prompt = localizedPrompt(id, language)
        val instruction = localizedInstruction(id, instruction, language)
        return when (this) {
            is ExerciseItem.MultipleChoice -> copy(
                prompt = prompt,
                instruction = instruction,
                choices = localizedChoices(id, choices, language)
            )
            is ExerciseItem.MatchingPairs -> copy(
                prompt = prompt,
                instruction = instruction,
                pairs = pairs.map { pair ->
                    pair.copy(right = vocabularyMeaning(pair.left, language))
                }
            )
            is ExerciseItem.Writing -> copy(
                prompt = prompt,
                instruction = instruction
            )
            is ExerciseItem.Listening -> copy(
                prompt = prompt,
                instruction = instruction
            )
            is ExerciseItem.Speaking -> copy(
                prompt = prompt,
                instruction = instruction
            )
        }
    }

    private fun localizedUnitTitle(lessonId: String, language: NativeLanguage): String {
        return when {
            lessonId.startsWith("unit_1") -> nativeText(
                language,
                pt = "Unidade 1, Caminho 1",
                uk = "Розділ 1, шлях 1",
                ru = "Раздел 1, путь 1"
            )
            lessonId == "daily_balanced_lesson" -> nativeText(
                language,
                pt = "Plano diário",
                uk = "Щоденний план",
                ru = "Ежедневный план"
            )
            lessonId == "practice_review_lesson" -> nativeText(
                language,
                pt = "Prática",
                uk = "Практика",
                ru = "Практика"
            )
            else -> nativeText(
                language,
                pt = "Desafio da liga",
                uk = "Виклик ліги",
                ru = "Вызов лиги"
            )
        }
    }

    private fun localizedLessonTitle(lessonId: String, language: NativeLanguage): String {
        return when (lessonId) {
            "unit_1_reading_daily_conversation" -> nativeText(language, "Ler rotinas do dia a dia", "Читання щоденних розпорядків", "Чтение повседневных распорядков")
            "unit_1_listening_daily_conversation" -> nativeText(language, "Ouvir rotinas curtas", "Слухання коротких розпорядків", "Прослушивание коротких распорядков")
            "unit_1_speaking_daily_conversation" -> nativeText(language, "Falar frases claras sobre rotina", "Говорити чіткі речення про розпорядок", "Говорить четкие фразы о распорядке")
            "unit_1_writing_daily_conversation" -> nativeText(language, "Escrever sobre sua rotina", "Писати про свій розпорядок", "Писать о своем распорядке")
            "daily_balanced_lesson" -> nativeText(language, "Prática diária equilibrada", "Збалансована щоденна практика", "Сбалансированная ежедневная практика")
            "practice_review_lesson" -> nativeText(language, "Rever pontos fracos", "Повторити слабкі місця", "Повторить слабые места")
            else -> nativeText(language, "Desafio rápido de XP", "Швидкий виклик XP", "Быстрый вызов XP")
        }
    }

    private fun localizedTopic(lessonId: String, language: NativeLanguage): String {
        return when {
            "listening" in lessonId -> nativeText(language, "Fala lenta e clara sobre aprendizado diário", "Повільне й чітке мовлення про щоденне навчання", "Медленная и четкая речь о ежедневном обучении")
            "speaking" in lessonId -> nativeText(language, "Pronúncia de frases sobre estudo diário", "Вимова речень про щоденне навчання", "Произношение фраз о ежедневной учебе")
            "writing" in lessonId -> nativeText(language, "Construção guiada de frases", "Кероване побудування речень", "Пошаговое построение предложений")
            else -> nativeText(language, "Rotina diária e hábitos de estudo", "Щоденний розпорядок і навчальні звички", "Ежедневный распорядок и учебные привычки")
        }
    }

    private fun localizedObjectives(lessonId: String, language: NativeLanguage): List<String> {
        return when {
            "listening" in lessonId -> listOf(
                nativeText(language, "Reconhecer palavras familiares em fala lenta e clara.", "Розпізнавати знайомі слова в повільному й чіткому мовленні.", "Распознавать знакомые слова в медленной и четкой речи."),
                nativeText(language, "Conectar áudio curto às escolhas escritas.", "Пов'язувати коротке аудіо з письмовими варіантами.", "Соотносить короткое аудио с письменными вариантами."),
                nativeText(language, "Ganhar confiança com escuta no estilo transcript.", "Набувати впевненості завдяки аудіюванню з опорою на текст.", "Развивать уверенность через аудирование с опорой на текст.")
            )
            "speaking" in lessonId -> listOf(
                nativeText(language, "Repetir frases curtas com ritmo claro.", "Повторювати короткі речення з чітким ритмом.", "Повторять короткие фразы с четким ритмом."),
                nativeText(language, "Usar frases de rotina em primeira pessoa.", "Використовувати речення про розпорядок від першої особи.", "Использовать фразы о распорядке от первого лица."),
                nativeText(language, "Treinar fala com verificações simples.", "Тренувати мовлення через прості перевірки.", "Тренировать речь с помощью простых проверок.")
            )
            "writing" in lessonId -> listOf(
                nativeText(language, "Traduzir frases simples sobre rotina.", "Перекладати прості речення про розпорядок.", "Переводить простые фразы о распорядке."),
                nativeText(language, "Construir ordem correta das palavras.", "Будувати правильний порядок слів.", "Строить правильный порядок слов."),
                nativeText(language, "Usar present simple para hábitos diários.", "Використовувати present simple для щоденних звичок.", "Использовать present simple для ежедневных привычек.")
            )
            else -> listOf(
                nativeText(language, "Entender frases curtas sobre rotinas.", "Розуміти короткі речення про розпорядок.", "Понимать короткие предложения о распорядке."),
                nativeText(language, "Identificar verbos familiares e expressões de tempo.", "Визначати знайомі дієслова й часові вирази.", "Определять знакомые глаголы и выражения времени."),
                nativeText(language, "Escolher respostas naturais para perguntas simples.", "Обирати природні відповіді на прості запитання.", "Выбирать естественные ответы на простые вопросы.")
            )
        }
    }

    private fun localizedGrammar(language: NativeLanguage): List<String> {
        return listOf(
            nativeText(language, "present simple", "present simple", "present simple"),
            nativeText(language, "ordem das palavras", "порядок слів", "порядок слов"),
            nativeText(language, "respostas curtas", "короткі відповіді", "краткие ответы")
        )
    }

    private fun localizedPrompt(itemId: String, language: NativeLanguage): String {
        return when (itemId) {
            "reading_1" -> nativeText(language, "Leia a frase e escolha o significado.", "Прочитайте речення й оберіть значення.", "Прочитайте предложение и выберите значение.")
            "reading_2" -> nativeText(language, "Escolha a frase que combina com a ideia.", "Оберіть речення, яке відповідає ідеї.", "Выберите предложение, которое соответствует идее.")
            "reading_3" -> nativeText(language, "Ligue as palavras de leitura.", "З'єднайте слова для читання.", "Соедините слова для чтения.")
            "reading_4" -> nativeText(language, "Escolha a melhor resposta.", "Оберіть найкращу відповідь.", "Выберите лучший ответ.")
            "listening_1" -> nativeText(language, "Ouça e escolha o que ouviu.", "Послухайте й оберіть те, що почули.", "Послушайте и выберите то, что услышали.")
            "listening_2" -> nativeText(language, "Ouça a rotina.", "Послухайте розпорядок.", "Послушайте распорядок.")
            "listening_3" -> nativeText(language, "Revise as palavras que ouviu.", "Повторіть слова, які почули.", "Повторите слова, которые услышали.")
            "listening_4" -> nativeText(language, "Ouça e escolha o melhor transcript.", "Послухайте й оберіть найкращий текст.", "Послушайте и выберите лучший текст.")
            "speaking_1" -> nativeText(language, "Diga esta frase em voz alta.", "Вимовте це речення вголос.", "Произнесите эту фразу вслух.")
            "speaking_2" -> nativeText(language, "Diga esta frase de rotina.", "Вимовте це речення про розпорядок.", "Произнесите эту фразу о распорядке.")
            "speaking_3" -> nativeText(language, "Escolha a frase com ordem natural das palavras.", "Оберіть речення з природним порядком слів.", "Выберите фразу с естественным порядком слов.")
            "writing_1" -> nativeText(language, "Traduza esta frase.", "Перекладіть це речення.", "Переведите это предложение.")
            "writing_2" -> nativeText(language, "Escreva uma frase com estas palavras.", "Напишіть речення з цими словами.", "Напишите предложение с этими словами.")
            "writing_3" -> nativeText(language, "Escolha a frase correta.", "Оберіть правильне речення.", "Выберите правильное предложение.")
            "writing_4" -> nativeText(language, "Responda em inglês.", "Дайте відповідь англійською.", "Ответьте на английском.")
            else -> nativeText(language, "Complete o exercício.", "Виконайте вправу.", "Выполните упражнение.")
        }
    }

    private fun localizedInstruction(itemId: String, current: String, language: NativeLanguage): String {
        return when (itemId) {
            "reading_2" -> nativeText(language, "Um hábito diário curto", "Коротка щоденна звичка", "Короткая ежедневная привычка")
            "reading_3" -> nativeText(language, "Conecte cada palavra em inglês ao seu significado.", "З'єднайте кожне англійське слово з його значенням.", "Соедините каждое английское слово с его значением.")
            "reading_4" -> nativeText(language, "Pergunta: Quando você estuda inglês?", "Запитання: Коли ви вивчаєте англійську?", "Вопрос: Когда вы изучаете английский?")
            "listening_1" -> nativeText(language, "Toque no alto-falante. A frase é curta e clara.", "Натисніть на динамік. Речення коротке й чітке.", "Нажмите на динамик. Фраза короткая и четкая.")
            "listening_2" -> nativeText(language, "Escolha a frase igual ao áudio.", "Оберіть речення, яке збігається з аудіо.", "Выберите фразу, совпадающую с аудио.")
            "listening_3" -> nativeText(language, "Ligue cada palavra de escuta ao seu significado.", "З'єднайте кожне слово з аудіювання з його значенням.", "Соедините каждое слово из аудирования с его значением.")
            "listening_4" -> nativeText(language, "Preste atenção à palavra final.", "Зверніть увагу на останнє слово.", "Обратите внимание на последнее слово.")
            "speaking_1" -> nativeText(language, "Use o microfone. O app compara sua fala com o alvo.", "Використайте мікрофон. Застосунок порівнює вашу вимову з ціллю.", "Используйте микрофон. Приложение сравнит вашу речь с целью.")
            "speaking_2" -> nativeText(language, "Fale naturalmente e depois confira o resultado.", "Говоріть природно, а потім перевірте результат.", "Говорите естественно, затем проверьте результат.")
            "speaking_3" -> nativeText(language, "Isso ajuda o ritmo da sua fala.", "Це допомагає ритму вашого мовлення.", "Это помогает ритму вашей речи.")
            "writing_1" -> nativeText(language, "Eu geralmente estudo depois da escola.", "Я зазвичай навчаюся після школи.", "Я обычно учусь после школы.")
            "writing_3" -> nativeText(language, "Rotina diária", "Щоденний розпорядок", "Ежедневный распорядок")
            "writing_4" -> nativeText(language, "Quando você pratica inglês?", "Коли ви практикуєте англійську?", "Когда вы практикуете английский?")
            else -> current
        }
    }

    private fun localizedChoices(itemId: String, choices: List<String>, language: NativeLanguage): List<String> {
        return when (itemId) {
            "reading_1" -> listOf(
                nativeText(language, "Eu geralmente estudo inglês depois da escola.", "Я зазвичай вивчаю англійську після школи.", "Я обычно изучаю английский после школы."),
                nativeText(language, "Eu compro um livro antes da escola.", "Я купую книжку перед школою.", "Я покупаю книгу перед школой."),
                nativeText(language, "Eu estudo inglês apenas no domingo.", "Я вивчаю англійську лише в неділю.", "Я изучаю английский только в воскресенье."),
                nativeText(language, "Eu escuto música depois da aula.", "Я слухаю музику після уроку.", "Я слушаю музыку после урока.")
            )
            else -> choices
        }
    }

    private fun vocabularyMeaning(word: String, language: NativeLanguage): String {
        return when (word.lowercase()) {
            "routine" -> nativeText(language, "rotina", "розпорядок", "распорядок")
            "usually" -> nativeText(language, "geralmente", "зазвичай", "обычно")
            "ready" -> nativeText(language, "pronto", "готовий", "готов")
            "practice" -> nativeText(language, "praticar", "практикувати", "практиковаться")
            "repeat" -> nativeText(language, "repetir", "повторити", "повторить")
            "answer" -> nativeText(language, "resposta", "відповідь", "ответ")
            "listen" -> nativeText(language, "ouvir", "слухати", "слушать")
            else -> word
        }
    }

    private fun nativeText(
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
}
