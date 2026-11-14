package com.skillhando.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreScreenshotTest {
    @get:Rule val ui = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var tag: String

    private fun click(label: String) {
        val node = ui.onAllNodes(hasText(label) and hasClickAction()).onFirst()
        runCatching { node.performScrollTo() }
        node.performClick()
    }

    private fun capture(name: String, label: String, scroll: Boolean = false) {
        val node = ui.onAllNodesWithText(label).onFirst().assertExists()
        if (scroll) node.performScrollTo()
        scenario.onActivity {
            it.getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(it.window.decorView.windowToken, 0)
        }
        ui.waitForIdle()
        scenario.onActivity { activity ->
            val decor = activity.window.decorView
            val bmp = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
            try {
                decor.draw(Canvas(bmp))
                val folder = File(activity.getExternalFilesDir(null), "store-shots/$tag").apply { mkdirs() }
                File(folder, "$name.png").outputStream().use { check(bmp.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally { bmp.recycle() }
        }
    }

    @Test fun renderStoreScreens() {
        val context = instrumentation.targetContext
        check(context.packageName.endsWith(".screenshots")) { "Use the isolated screenshot build." }
        tag = requireNotNull(InstrumentationRegistry.getArguments().getString("locale"))
        context.getSharedPreferences("settings", 0).edit().clear().putBoolean("welcome", true)
            .putString("language", tag).putString("interest", "speaking").commit()
        Localisation.load(context)
        assertEquals(tag, Localisation.locale.toLanguageTag())
        val skill = Catalogue(context).skills.first { it.id == "speaking" }
        val challenge = skill.challenges.first()
        val db = LearningDatabase.open(context)
        try {
            runBlocking {
                val dao = db.learning()
                dao.erase()
                val now = System.currentTimeMillis()
                dao.insertAttempt(Attempt("store-before", skill.id, challenge.id, now - 86_400_000, 239,
                    true, "2,3,3", "", "", "", challenge.prompt))
                dao.insertAttempt(Attempt("store-after", skill.id, challenge.id, now, 310,
                    true, "4,4,4", "", "", "", challenge.criterion, experiment = challenge.experiment))
                dao.saveGoal(Goal("store-goal", skill.title, 4, now))
            }
        } finally { db.close() }

        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            scenario = activity
            fun nav(key: String) = click(tr(key))
            capture("today", tr("make_time_to_get_better"))
            nav("skills")
            capture("skills", skill.title)
            click(skill.title)
            nav("practise_again")
            capture("challenge", tr("before_you_start"))
            nav("more")
            nav("open_coach")
            ui.onNodeWithText(tr("what_would_you_like_to_practise")).performScrollTo().performTextInput(skill.title)
            nav("suggest_practice")
            capture("coach", tr("review_suggested_challenge"), scroll = true)
            nav("more")
            nav("open_voice_controls")
            ui.onNodeWithText(tr("command")).performScrollTo()
                .performTextInput(VoiceCommands.examples(Localisation.locale)[VoiceIntent.READ.ordinal])
            nav("run_command")
            capture("voice", tr("run_command"))
            nav("progress")
            capture("portfolio", tr("practice_timeline"), scroll = true)
            nav("review_attempt")
            capture("comparison", tr("compare_with_your_previous_attempt"), scroll = true)
            nav("more")
            nav("manage_goals")
            capture("goals", tr("new_weekly_goal"))
        }
    }
}
