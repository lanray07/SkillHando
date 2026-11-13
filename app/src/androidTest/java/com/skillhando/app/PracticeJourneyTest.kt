package com.skillhando.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PracticeJourneyTest {
    @get:Rule val app = createAndroidComposeRule<MainActivity>()

    private fun click(text: String) {
        val node = app.onAllNodes(hasText(text) and hasClickAction()).onFirst()
        runCatching { node.performScrollTo() }
        node.performClick()
    }

    private fun capture(name: String) {
        app.waitForIdle()
        android.os.SystemClock.sleep(850)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = app.runOnUiThread {
            val view = app.activity.window.decorView
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also {
                view.draw(android.graphics.Canvas(it))
            }
        }
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "store-captures").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun completesTwoAttemptsAndComparesSelfReportedEvidence() {
        app.waitUntil(12_000) { app.onAllNodesWithText("Skip setup").fetchSemanticsNodes().isNotEmpty() || app.onAllNodesWithText("Make time to get better.").fetchSemanticsNodes().isNotEmpty() }
        if (app.onAllNodesWithText("Skip setup").fetchSemanticsNodes().isNotEmpty()) click("Skip setup")
        app.onNodeWithText("Make time to get better.").assertExists()
        capture("today")
        click("Skills")
        capture("skills")
        click("Public speaking")
        val baseline = app.onNodeWithText("A one-minute introduction")
        baseline.performScrollTo()
        app.onAllNodes(hasText("Practise again") or hasText("View exercise")).onFirst().performScrollTo().performClick()
        capture("challenge")
        val observations = listOf("I rushed past my example.", "I paused before the example and stayed within a minute.")
        repeat(2) { index ->
            click("Open guided practice")
            click("Start timer")
            click("Review this attempt")
            app.onNodeWithText("Observable result").performScrollTo().performTextInput(observations[index])
            app.onNodeWithText("Reflection").performScrollTo().performTextInput(
                if (index == 0) "Next time, pause after I introduce myself." else "The pause gave me time to breathe.")
            repeat(3) { criterion ->
                app.onAllNodesWithText(if (index == 0) "2" else "4")[criterion].performScrollTo().performClick()
            }
            click("Save completed attempt")
            app.waitUntil(8_000) { app.onAllNodesWithText(observations[index]).fetchSemanticsNodes().isNotEmpty() }
            if (index == 0) click("A one-minute introduction")
        }
        app.onNodeWithText("Compare with your previous attempt").assertExists().performScrollTo()
        app.onNodeWithText("Clarity · 2/5 → 4/5").assertExists()
        app.onNodeWithText("Previous observation: ${observations[0]}").assertExists()
        app.onNodeWithText("Explain it to a beginner").assertExists()
        capture("comparison")
        click("Back to progress")
        app.onNodeWithText("Practice timeline").performScrollTo()
        capture("portfolio")

        click("More")
        click("Open coach")
        app.onNodeWithText("What would you like to practise?").performTextInput("I want to improve my speaking in a meeting")
        click("Suggest practice")
        app.onNodeWithText("Explain it to a beginner").assertExists()
        app.runOnUiThread { app.activity.getSystemService(android.view.inputmethod.InputMethodManager::class.java).hideSoftInputFromWindow(app.activity.window.decorView.windowToken, 0) }
        capture("coach")

        click("More")
        click("Open voice controls")
        app.onNodeWithText("Command").performScrollTo().performTextInput("Read today's challenge")
        click("Run command")
        app.onNodeWithText("Explain it to a beginner.", substring = true).assertExists()
        app.runOnUiThread { app.activity.getSystemService(android.view.inputmethod.InputMethodManager::class.java).hideSoftInputFromWindow(app.activity.window.decorView.windowToken, 0) }
        capture("voice")

        click("More")
        click("Manage goals")
        app.onNodeWithText("Learning objective").performScrollTo().performTextInput("Give a clearer update in meetings")
        click("Save weekly goal")
        app.waitUntil(7_000) { app.onAllNodes(hasText("Give a clearer update in meetings") and !hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        app.runOnUiThread { app.activity.getSystemService(android.view.inputmethod.InputMethodManager::class.java).hideSoftInputFromWindow(app.activity.window.decorView.windowToken, 0) }
        capture("goals")
    }
}
