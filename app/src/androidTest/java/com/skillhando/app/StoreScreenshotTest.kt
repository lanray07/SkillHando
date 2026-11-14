package com.skillhando.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test fun checkLocalizedVoice() {
        val context = instrumentation.targetContext
        check(context.packageName.endsWith(".screenshots")) { "Use the isolated screenshot build." }
        tag = requireNotNull(InstrumentationRegistry.getArguments().getString("locale"))
        context.getSharedPreferences("settings", 0).edit().putBoolean("welcome", true)
            .putBoolean("speech", true).putString("language", tag).putString("interest", "speaking").commit()
        val locale = Locale.forLanguageTag(tag)
        val recognition = SpeechRecognizer.isRecognitionAvailable(context)
        val engines = listOf<String?>(null) + TextToSpeech(context) { }.let { probe ->
            try { probe.engines.map { it.name } } finally { probe.shutdown() }
        }
        var defaultAvailability = TextToSpeech.LANG_NOT_SUPPORTED
        for (name in engines) {
            val initialized = CountDownLatch(1)
            var result = TextToSpeech.ERROR
            val engine = TextToSpeech(context, { status ->
                result = status
                initialized.countDown()
            }, name)
            try {
                assertTrue("TTS initialization timed out: ${name ?: "default"}", initialized.await(12, TimeUnit.SECONDS))
                val availability = if (result == TextToSpeech.SUCCESS) engine.isLanguageAvailable(locale) else result
                if (name == null) defaultAvailability = availability
                println("Voice QA: $tag engine=${name ?: "default"} init=$result availability=$availability")
            } finally {
                engine.shutdown()
            }
        }
        println("Voice QA: $tag recognition=$recognition")
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            scenario = activity
            assertEquals(tag, Localisation.locale.toLanguageTag())
            click(tr("more"))
            click(tr("open_voice_controls"))
            if (!recognition) {
                ui.onNodeWithText(tr("this_device_has_no_available_speech_recognition_service_type_a_command_below"))
                    .assertExists()
                ui.onNodeWithText(tr("tap_to_speak")).assertDoesNotExist()
            }
            val challenge = Catalogue(context).skills.first { it.id == "speaking" }.challenges.first()
            val phrase = VoiceCommands.examples(locale)[VoiceIntent.READ.ordinal]
            ui.onNodeWithText(tr("command")).performScrollTo().performTextInput(phrase)
            click(tr("run_command"))
            val response = challenge.title + ". " + challenge.steps.joinToString(". ")
            ui.onNodeWithText(response).assertExists()
            if (defaultAvailability < TextToSpeech.LANG_AVAILABLE) {
                ui.onNodeWithText(tr("this_device_has_no_available_voice_for_the_selected_language")).assertExists()
            }
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
