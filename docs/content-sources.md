# Ayanami Learn content sources

This document defines how external ESL resources should influence Ayanami Learn.
The product must not copy lessons, transcripts, audio, quizzes, branding, or UI assets from third-party sites unless the license/API agreement explicitly allows it.

## Source policy

- Use public ESL sites as curriculum references: CEFR order, exercise formats, skill coverage, difficulty progression, and topic taxonomy.
- Use licensed APIs/datasets only through their documented terms.
- Store source metadata for imported/generated content: provider, license, CEFR level, skill, topic, attribution requirement, and original URL.
- Prefer original Ayanami-authored exercises generated from our own curriculum model.
- Treat chat and live voice as support layers. The learning path must be driven by real exercise data, validation logic, progress, and session reports.

## Reference sources

| Source | Best use | Integration status |
| --- | --- | --- |
| ELLLO | Listening lesson structure: audio, transcript, vocabulary, quiz, CEFR-style grouping | Reference only unless permission/license is confirmed |
| British Council LearnEnglish | CEFR progression for listening, reading, writing, speaking, grammar, vocabulary | Reference only unless permission/license is confirmed |
| Cambridge English Learning English | Exercise formats, level-test ideas, exam/prep style | Reference only unless permission/license is confirmed |
| Cambridge Dictionary API | Dictionary lookup, definitions, pronunciations, examples, word data | API integration after key/agreement |
| OpenAI Speech to Text | Optional transcription provider for uploaded audio content | Provider adapter only if selected |
| Speech Lab IITM ASR | Optional open ASR provider for English audio transcription | Provider adapter candidate |
| Kaggle English audio dataset | Prototype/test content pipeline if license permits | Dataset import candidate |
| TELUS English UK remote speech dataset | High-quality licensed speech data for ASR/accent testing | Commercial dataset candidate |

## Progress and gamification references

These sources are not lesson-content providers. They are useful for understanding learner profile and progress concepts such as streaks, XP, studied languages, level state, achievements, and league-style progression.

| Source | Best use | Integration status |
| --- | --- | --- |
| igorskh/duolingo-api | Reference for profile/progress fields such as streak, XP, language state and achievements | Reference only; do not depend on unofficial/private APIs |
| igorskh/go-duolingo | Reference for typed client/domain modeling of Duolingo-like progress data | Reference only; model inspiration |

## Content pipeline target

1. User onboarding creates a LearningProfile: native language, self-rated level, daily minutes, goal, interests.
2. Placement test estimates CEFR level.
3. Planner agent creates a DailyStudyPlan from profile, level, time, weak points, and past session reports.
4. Exercise generator creates original Ayanami exercises using source-inspired templates and validated curriculum rules.
5. Exercise engine records attempts, correctness, time spent, hints used, speech transcript quality, and XP.
6. Session reporter summarizes learning behavior after each exercise/chat/voice session.
7. Planner agent updates the next study path using long-term progress, not only the last answer.

## Required data model direction

- ContentSource: provider, url, licenseType, attribution, allowedUse, notes.
- CurriculumUnit: CEFR level, skill, topic, grammarFocus, vocabularySet.
- ExerciseTemplate: type, skill, validator, difficulty, requiredInputs.
- ExerciseAttempt: userId, exerciseId, answer, correctness, timeSpentMs, hintsUsed, createdAt.
- SessionReport: userId, sourceSessionId, observedSkills, recurringMistakes, newVocabulary, confidenceSignal, recommendedFocus.
- DailyStudyPlan: userId, date, targetMinutes, exercises, rationale, generatedFromReportIds.
- LearnerProgress: userId, studiedLanguage, streakDays, totalXp, completedLessonIds, skillLevels, achievements, lastStudyDay.
- LessonCompletionResult: lessonId, skill, earnedXp, correctCount, totalCount, completedAtMillis.

## Implementation rule

No visual-only exercise screen is acceptable. Every lesson node must resolve to real lesson data and every exercise must have a validator, result state, and progress event.
Progress UI must also be data-driven: streak, XP, achievements and skill levels cannot be static display values.
