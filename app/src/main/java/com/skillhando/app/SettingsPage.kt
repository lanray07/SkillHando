package com.skillhando.app

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import java.util.Locale

@Composable
fun Welcome(model: LearningModel, voice: Voice, speech: Boolean, setSpeech: (Boolean) -> Unit, finish: () -> Unit) {
    Heading(tr("find_your_next_small_win"), tr("choose_a_skill_try_a_short_exercise_and_keep_a_record_of_what_changes"))
    androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.practice_art), null,
        Modifier.fillMaxWidth().height(220.dp), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    var interest by rememberSaveable { mutableStateOf(model.preferences.getString("interest", "speaking")!!) }
    var experience by rememberSaveable { mutableIntStateOf(0) }
    val levels = listOf(tr("beginner"), tr("intermediate"), tr("advanced"))
    var minutes by rememberSaveable { mutableStateOf(5) }
    var weekly by rememberSaveable { mutableStateOf("") }
    LanguagePicker()
    Panel {
        Subheading(tr("what_would_you_like_to_practise"))
        SkillPicker(model.skills, interest) { interest = it }
        Subheading(tr("where_are_you_starting"))
        OptionPicker(levels, levels[experience]) { experience = levels.indexOf(it) }
        Subheading(tr("time_for_a_session"))
        OptionPicker(listOf(5, 10, 15).map { "$it min" }, "$minutes min") { minutes = it.substringBefore(" ").toInt() }
        Entry(weekly, { if (it.all(Char::isDigit)) weekly = it }, tr("optional_weekly_session_target"), limit = 2)
    }
    Panel {
        Subheading(tr("voice_and_privacy"))
        SettingSwitch(tr("read_instructions_aloud"), speech) {
            model.preferences.edit().putBoolean("speech", it).apply()
            setSpeech(it)
        }
        if (speech) Text(if (voice.spokenAvailable) tr("uses_a_voice_installed_on_this_device") else tr("a_voice_service_hasn_t_been_detected"))
        Text(tr("your_practice_history_and_attachments_stay_on_this_device_you_can_change_voice_a"))
    }
    Button(onClick = {
        model.preferences.edit().putString("interest", interest).putInt("duration", minutes)
            .putInt("experience", experience).apply()
        weekly.toIntOrNull()?.takeIf { it in 1..50 }?.let { model.goal(tr("weekly_practice"), it) }
        finish()
    }, enabled = weekly.isBlank() || weekly.toIntOrNull() in 1..50) { Text(tr("start_learning")) }
    TextButton(onClick = finish) { Text(tr("skip_setup")) }
}

@Composable
private fun SkillPicker(skills: List<Skill>, interest: String, select: (String) -> Unit) {
    val current = skills.first { it.id == interest }
    OptionPicker(skills.map { it.title }, current.title) { label -> select(skills.first { it.title == label }.id) }
}

@Composable
private fun OptionPicker(options: List<String>, selected: String, changed: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text(selected) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { open = false; changed(option) }) }
        }
    }
}

@Composable
fun SettingSwitch(label: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f).padding(end = 12.dp))
        Switch(checked, change, modifier = Modifier.semanticsLabel(label))
    }
}

@Composable
fun MorePage(model: LearningModel, voice: Voice, speech: Boolean, setSpeech: (Boolean) -> Unit,
             attempts: List<Attempt>, goals: List<Goal>, skill: Skill, recommendation: Recommendation,
             page: String, setPage: (String) -> Unit, progress: () -> Unit, practice: () -> Unit,
             open: (Pair<Skill, Challenge>) -> Unit) {
    if (page.isNotBlank()) TextButton(onClick = { setPage("") }) { Text(tr("back_to_more")) }
    when (page) {
        "coach" -> CoachPage(model, attempts, skill, open)
        "voice" -> VoicePage(model, voice, speech, setSpeech, skill, recommendation, goals, progress, practice, open)
        "goals" -> GoalsPage(model, goals)
        "privacy" -> PrivacyPage(model)
        "premium" -> PremiumPage()
        else -> {
            Heading(tr("more"), tr("make_skillhando_work_for_the_way_you_practise"))
            LanguagePicker()
            ActionCard(tr("practice_coach"), tr("match_a_learning_request_to_an_exercise_from_your_offline_catalogue"), tr("open_coach")) { setPage("coach") }
            ActionCard(tr("voice_controls"), tr("read_instructions_or_run_a_command_with_your_voice_or_keyboard"), tr("open_voice_controls")) { setPage("voice") }
            ActionCard(tr("goals_and_reminders"), tr("set_a_weekly_target_or_choose_a_time_for_a_reminder"), tr("manage_goals")) { setPage("goals") }
            ActionCard(tr("privacy_and_data"), tr("review_where_your_data_lives_and_delete_your_local_records"), tr("privacy_settings")) { setPage("privacy") }
            ActionCard(tr("skillhando_premium"), tr("check_availability_and_restore_your_amazon_purchase"), tr("view_premium")) { setPage("premium") }
        }
    }
}

@Composable
private fun CoachPage(model: LearningModel, attempts: List<Attempt>, preferred: Skill, open: (Pair<Skill, Challenge>) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var proposal by rememberSaveable { mutableStateOf<String?>(null) }
    Heading(tr("practice_coach"), tr("find_a_next_step_you_can_try_with_the_time_and_materials_you_have"))
    Panel {
        Text(tr("the_offline_coach_matches_your_request_to_bundled_exercises_and_uses_completed_a"))
        Entry(text, { text = it }, tr("what_would_you_like_to_practise"), tr("tell_a_clearer_story_in_a_meeting"), 500)
        Button(onClick = { proposal = PracticeEngine.suggest(model.skills, attempts, text, preferred.id).id }, enabled = text.isNotBlank()) { Text(tr("suggest_practice")) }
    }
    proposal?.let { id ->
        val match = model.skills.first { it.id == id }
        val suggested = PracticeEngine.recommend(match, attempts)
        Panel {
            Subheading(suggested.challenge.title)
            Text(match.title)
            Text(suggested.challenge.objective)
            Text(recommendationText(match, suggested))
            Text(suggested.challenge.experiment)
            val count = attempts.count { it.complete && it.skill == id }
            if (count > 0) Text(trf("completed_skill_attempts", count))
            Button(onClick = { open(match to suggested.challenge) }) { Text(tr("review_suggested_challenge")) }
        }
    }
}

@Composable
private fun VoicePage(model: LearningModel, voice: Voice, speech: Boolean, setSpeech: (Boolean) -> Unit,
                      skill: Skill, recommendation: Recommendation, goals: List<Goal>, progress: () -> Unit,
                      practice: () -> Unit, open: (Pair<Skill, Challenge>) -> Unit) {
    val context = LocalContext.current
    val locale = Localisation.locale
    var denied by rememberSaveable { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf("") }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
        if (granted) voice.listen(locale)
    }
    fun reply(text: String) {
        result = text
        if (speech) voice.speak(text, locale)
    }
    Heading(tr("voice_controls"), tr("speak_a_command_or_type_it_and_review_before_running_it"))
    Panel {
        SettingSwitch(tr("enable_spoken_feedback"), speech) {
            model.preferences.edit().putBoolean("speech", it).apply()
            setSpeech(it)
            if (!it) voice.cancel()
        }
        Text(tr("your_device_s_speech_provider_may_process_audio_online_skillhando_keeps_no_recor"))
        if (voice.recognitionAvailable) OutlinedButton(onClick = {
            if (voice.listening) voice.cancel() else {
                when (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)) {
                    android.content.pm.PackageManager.PERMISSION_GRANTED -> voice.listen(locale)
                    else -> permission.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }) { Text(if (voice.listening) tr("cancel_listening") else tr("tap_to_speak")) }
        else Text(tr("this_device_has_no_available_speech_recognition_service_type_a_command_below"))
        if (denied) Text(tr("microphone_permission_was_declined_you_can_type_a_command"))
        if (voice.listening) Text(tr("listening"))
        if (voice.status.isNotBlank()) Text(voice.status)
        Entry(voice.transcript, { voice.transcript = it }, tr("command"), VoiceCommands.examples(locale).first(), 240)
        Button(onClick = {
            val draft = model.draft.value
            when (VoiceCommands.parse(voice.transcript, locale)) {
                VoiceIntent.PROGRESS -> progress()
                VoiceIntent.NEXT -> open(skill to recommendation.challenge)
                VoiceIntent.READ -> reply(recommendation.challenge.title + ". " + recommendation.challenge.steps.joinToString(". "))
                VoiceIntent.GOALS -> reply(goals.filterNot { it.paused }.joinToString(". ") { it.label + " · " + trf("weekly_target", it.weeklySessions) }.ifBlank { tr("you_have_no_active_goals") })
                VoiceIntent.PAUSE -> if (draft?.phase == "active") { model.timer("pause", draft.notes); practice() } else reply(tr("there_is_no_active_timer"))
                VoiceIntent.RESUME -> if (draft?.phase == "active") { model.timer("start", draft.notes); practice() } else reply(tr("open_a_challenge_first"))
                VoiceIntent.FINISH -> if (draft != null) { model.timer("review", draft.notes); practice() } else reply(tr("there_is_no_attempt_to_review"))
                null -> reply(VoiceCommands.examples(locale).take(3).joinToString(" · "))
            }
        }, enabled = voice.transcript.isNotBlank() && !voice.listening) { Text(tr("run_command")) }
        if (result.isNotBlank()) Text(result)
    }
}

@Composable
private fun GoalsPage(model: LearningModel, goals: List<Goal>) {
    val context = LocalContext.current
    var label by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("3") }
    var hour by rememberSaveable { mutableIntStateOf(model.preferences.getInt("reminderHour", 18)) }
    var enabled by rememberSaveable { mutableStateOf(model.preferences.getBoolean("reminders", false)) }
    var denial by rememberSaveable { mutableStateOf(false) }
    fun schedule() {
        enabled = true
        denial = false
        model.preferences.edit().putBoolean("reminders", true).putInt("reminderHour", hour).apply()
        Reminders.schedule(context, hour)
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) schedule() else denial = true }
    Heading(tr("goals_and_reminders"), tr("choose_a_pace_that_fits_your_week"))
    Panel {
        Subheading(tr("new_weekly_goal"))
        Entry(label, { label = it }, tr("learning_objective"), tr("speak_more_clearly_in_meetings"), 120)
        Entry(target, { if (it.all(Char::isDigit)) target = it }, tr("completed_sessions_per_week"), limit = 2)
        Button(enabled = label.isNotBlank() && target.toIntOrNull() in 1..50, onClick = {
            model.goal(label, target.toInt()); label = ""
        }) { Text(tr("save_weekly_goal")) }
    }
    goals.forEach { goal ->
        ActionCard(goal.label, trf("weekly_target", goal.weeklySessions) + if (goal.paused) " · " + tr("paused") else "",
            if (goal.paused) tr("resume_goal") else tr("pause_goal")) { model.toggleGoal(goal) }
    }
    Panel {
        Subheading(tr("practice_reminder"))
        Text(tr("reminders_are_approximate_quiet_hours_run_from_21_00_to_08_00"))
        OptionPicker((8..20).map { "$it:00" }, "$hour:00") {
            hour = it.substringBefore(":").toInt()
            if (enabled) schedule()
        }
        SettingSwitch(tr("daily_reminder"), enabled) {
            if (!it) {
                enabled = false
                model.preferences.edit().putBoolean("reminders", false).apply()
                Reminders.cancel(context)
            } else if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                ask.launch(Manifest.permission.POST_NOTIFICATIONS)
            else schedule()
        }
        if (denial) Text(tr("notifications_are_disabled_in_your_device_settings"))
    }
}

@Composable
private fun PrivacyPage(model: LearningModel) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    Heading(tr("privacy_and_data"), tr("your_practice_portfolio_is_private_to_this_installation"))
    Panel {
        Text(tr("no_account_is_needed_skillhando_stores_notes_self_ratings_photos_and_audio_in_ap"))
        Text(tr("you_can_export_your_practice_history_from_progress_exported_copies_may_remain_af"))
        Text(tr("speech_recognition_uses_your_device_s_provider_which_may_process_audio_online_th"))
        OutlinedButton(onClick = { confirm = true }) { Text(tr("delete_all_learning_records")) }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false }, title = { Text(tr("delete_your_learning_records")) },
        text = { Text(tr("this_removes_practice_goals_pathways_and_private_attachments_from_this_device")) },
        confirmButton = { TextButton(onClick = { confirm = false; model.erase() }) { Text(tr("delete_learning_data")) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(tr("keep_data")) } }
    )
}

private fun Modifier.semanticsLabel(label: String): Modifier = semantics { contentDescription = label }
