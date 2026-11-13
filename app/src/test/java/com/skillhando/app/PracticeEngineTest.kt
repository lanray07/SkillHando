package com.skillhando.app

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class PracticeEngineTest {
    private val baseline = Challenge("a", "A", "", "", emptyList(), "", "", "", 5, 0)
    private val next = baseline.copy(id = "b", title = "B", level = 1)
    private val skill = Skill("s", "Speaking", "Deliver a meeting update", listOf("Clarity", "Pace"), listOf(baseline, next))

    private fun attempt(id: String, challenge: String = "a", time: Long = 1, done: Boolean = true, ratings: String = "3,2") =
        Attempt(id, "s", challenge, time, 30, done, ratings, "", "", "")

    @Test fun incompleteAttemptDoesNotReplaceBaseline() {
        val suggestion = PracticeEngine.recommend(skill, listOf(attempt("x", done = false)))
        assertEquals(Recommendation.Reason.BASELINE, suggestion.reason)
        assertEquals(baseline, suggestion.challenge)
    }

    @Test fun weakCriterionRecommendsSameChallenge() {
        val suggestion = PracticeEngine.recommend(skill, listOf(attempt("x", ratings = "4,2")))
        assertEquals(Recommendation.Reason.REPEAT, suggestion.reason)
        assertEquals(1, suggestion.focus)
        assertEquals(baseline, suggestion.challenge)
    }

    @Test fun advancesWhenAllCriteriaReachThreshold() {
        val suggestion = PracticeEngine.recommend(skill, listOf(attempt("x", ratings = "3,3")))
        assertEquals(Recommendation.Reason.BUILD, suggestion.reason)
        assertEquals(next, suggestion.challenge)
    }

    @Test fun comparisonOnlyUsesEarlierCompleteAttemptAtSameChallenge() {
        val latest = attempt("recent", time = 8)
        val baseline = attempt("baseline", time = 1)
        val history = listOf(latest, attempt("newer", time = 9), attempt("other", challenge = "b", time = 7),
            attempt("incomplete", time = 6, done = false), baseline)
        assertEquals(baseline, PracticeEngine.comparableAttempt(latest, history))
        assertNull(PracticeEngine.comparableAttempt(baseline, history))
    }

    @Test fun tiedWeakCriteriaPreferTheOneThatDeclined() {
        val result = PracticeEngine.recommend(skill, listOf(attempt("x", time = 5, ratings = "2,2"), attempt("y", time = 4, ratings = "2,5")))
        assertEquals(1, result.focus)
    }

    @Test fun runningTimerIsMonotonicAndDoesNotInventPostRebootTime() {
        val draft = Draft(id = "d", skill = "s", challenge = "a", targetMinutes = 5, elapsed = 20, runningSince = 10000, boot = 2)
        assertEquals(25, draft.elapsedAt(15000, 2))
        assertEquals(20, draft.elapsedAt(50000, 3))
        assertEquals(20, draft.elapsedAt(5000, 2))
    }

    @Test fun typedCommandsNormalisePunctuation() {
        assertEquals(VoiceIntent.READ, VoiceCommands.parse("Read today’s challenge!", Locale.UK))
        assertEquals(VoiceIntent.NEXT, VoiceCommands.parse("¿Qué debo practicar ahora?", Locale.forLanguageTag("es-ES")))
        assertNull(VoiceCommands.parse("erase my data", Locale.UK))
    }

    @Test fun csvCellsQuoteUserDataAndNeutraliseSpreadsheetFormulas() {
        assertEquals("\"'=1+2\"", ReportWriter.csv("=1+2"))
        assertEquals("\"She said \"\"hello\"\", then left\"", ReportWriter.csv("She said \"hello\", then left"))
    }
}
