package com.skillhando.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun GuidedPractice(draft: Draft, skill: Skill, challenge: Challenge, model: LearningModel, voice: Voice,
                   media: PracticeMedia, speech: Boolean, premium: Boolean, storedEvidence: Int, busy: Boolean,
                   saved: (String) -> Unit) {
    var notes by rememberSaveable(draft.id) { mutableStateOf(draft.notes) }
    var observation by rememberSaveable(draft.id) { mutableStateOf("") }
    var reflection by rememberSaveable(draft.id) { mutableStateOf("") }
    var next by rememberSaveable(draft.id) { mutableStateOf("") }
    var ratings by rememberSaveable(draft.id) { mutableStateOf(ArrayList(List(skill.rubric.size) { 0 })) }
    var attachments by rememberSaveable(draft.id) { mutableStateOf(arrayListOf<String>()) }
    val items = attachments.chunked(4).filter { it.size == 4 }.map { Evidence(it[0], draft.id, it[1], it[2], it[3]) }
    var confirm by rememberSaveable { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    var audioPaused by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasRoom = premium || storedEvidence + items.size < 10
    var elapsed by remember { mutableIntStateOf(draft.elapsedAt(SystemClock.elapsedRealtime(), model.boot)) }

    LaunchedEffect(draft) {
        do {
            elapsed = draft.elapsedAt(SystemClock.elapsedRealtime(), model.boot)
            if (draft.runningSince == null || draft.boot != model.boot) break
            delay(300)
        } while (true)
    }

    fun attach(item: Evidence) { attachments = ArrayList(attachments + listOf(item.id, item.name, item.kind, item.fileName)) }

    fun record() {
        runCatching { media.start(model.fileFor(draft.id, "audio")) }
            .onSuccess { recording = true; audioPaused = false }
            .onFailure { model.notice.value = tr("recording_could_not_start_you_can_add_a_written_reflection") }
    }

    val microphone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) record() else model.notice.value = tr("microphone_permission_was_declined_you_can_still_write_a_reflection")
    }
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && hasRoom) {
            importing = true
            scope.launch {
                try { attach(model.importPhoto(draft.id, uri)) }
                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) { model.notice.value = tr("choose_a_readable_photo_under_20_mb") }
                finally { importing = false }
            }
        }
    }

    DisposableEffect(draft.id) {
        onDispose { media.close() }
    }

    Heading(challenge.title, skill.title)
    if (draft.phase != "review") {
        Panel {
            Subheading(tr("practice_timer"))
            Text(String.format(Localisation.locale, "%d:%02d", elapsed / 60, elapsed % 60),
                style = MaterialTheme.typography.displayLarge, fontFamily = FontFamily.Monospace)
            val target = draft.targetMinutes * 60
            LinearProgressIndicator(progress = { (elapsed.toFloat() / target).coerceAtMost(1f) }, modifier = Modifier.fillMaxWidth())
            Text(if (elapsed >= target) trf("target_reached", draft.targetMinutes) else trf("timer_target", draft.targetMinutes))
            val running = draft.runningSince != null && draft.boot == model.boot
            Button(onClick = { model.timer(if (running) "pause" else "start", notes) }, enabled = !busy) {
                Text(if (running) tr("pause_timer") else if (draft.phase == "ready") tr("start_timer") else tr("resume_timer"))
            }
        }
        Panel {
            Subheading(tr("keep_this_in_view"))
            Text(draft.baseline.ifBlank { challenge.criterion })
            if (draft.change.isNotBlank()) Text(trf("change_to_try", draft.change))
            challenge.steps.forEachIndexed { index, step -> Text("${index + 1}. $step") }
            if (speech) OutlinedButton(onClick = { voice.speak(challenge.steps.joinToString(". "), Localisation.locale) }) { Text(tr("read_instructions_aloud")) }
            if (speech && voice.status.isNotBlank()) Text(voice.status)
            Entry(notes, { notes = it }, tr("practice_notes"))
        }
        Button(onClick = { model.timer("review", notes) }, enabled = !busy) { Text(tr("review_this_attempt")) }
    } else {
        Panel {
            Subheading(tr("what_did_you_notice"))
            Text(challenge.criterion)
            Entry(observation, { observation = it }, tr("observable_result"), tr("what_did_you_hear_see_or_measure"))
            Text(challenge.prompt)
            Entry(reflection, { reflection = it }, tr("reflection"))
        }
        Panel {
            Subheading(tr("rate_this_attempt"))
            Text(tr("self_assessment_1_needs_work_3_partly_meets_the_criterion_5_meets_it_consistentl"))
            skill.rubric.forEachIndexed { index, label ->
                Text(label, style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth()) {
                    (1..5).forEach { score ->
                        val selected = ratings[index] == score
                        Surface(modifier = Modifier.weight(1f).heightIn(min = 48.dp).selectable(selected = selected, role = Role.RadioButton,
                            onClick = { ratings = ArrayList(ratings).apply { set(index, score) } }),
                            color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface) {
                            Box(contentAlignment = androidx.compose.ui.Alignment.Center) { Text(score.toString()) }
                        }
                    }
                }
            }
        }
        Panel {
            Subheading(tr("evidence_for_your_portfolio"))
            Text(tr("add_a_photo_or_audio_clip_you_can_revisit_when_you_try_again"))
            if (!hasRoom) Text(tr("your_free_portfolio_holds_up_to_10_attachments"))
            if (!recording) {
                OutlinedButton(onClick = { photo.launch("image/*") }, enabled = hasRoom && !importing) { Text(tr("attach_a_photo")) }
                OutlinedButton(onClick = {
                    if (ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) record()
                    else microphone.launch(Manifest.permission.RECORD_AUDIO)
                }, enabled = hasRoom && !importing) { Text(tr("record_audio_evidence")) }
            } else {
                Text(if (audioPaused) tr("recording_paused") else tr("recording_audio"))
                TextButton(onClick = {
                    runCatching { if (audioPaused) media.resume() else media.pause() }
                        .onSuccess { audioPaused = !audioPaused }
                        .onFailure { model.notice.value = tr("couldn_t_change_the_recording_state") }
                }) { Text(if (audioPaused) tr("resume_recording") else tr("pause_recording")) }
                Button(onClick = {
                    recording = false
                    media.stop()?.let { attach(Evidence(java.util.UUID.randomUUID().toString(), draft.id, tr("practice_recording"), "audio", it.name)) }
                        ?: run { model.notice.value = tr("the_recording_could_not_be_saved_try_recording_a_little_longer") }
                }) { Text(tr("stop_and_attach_recording")) }
            }
            items.forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(item.name, Modifier.weight(1f))
                    TextButton(onClick = {
                        File(model.vault, item.fileName).delete()
                        attachments = ArrayList(items.filter { it.id != item.id }.flatMap { listOf(it.id, it.name, it.kind, it.fileName) })
                    }) { Text(tr("remove")) }
                }
            }
        }
        Panel {
            Subheading(tr("plan_the_next_attempt"))
            Text(challenge.experiment)
            Entry(next, { next = it }, tr("what_will_you_change_next_time"))
            val rated = ratings.all { it > 0 }
            val ready = rated && (observation.isNotBlank() || reflection.isNotBlank())
            if (!rated) Text(tr("rate_each_criterion_to_save_a_completed_attempt"))
            if (rated && !ready) Text(tr("observation_required"))
            Button(enabled = !busy && ready && !recording && !importing, onClick = {
                model.finish(true, ratings, observation, reflection, next, items) { saved(draft.id) }
            }) { Text(tr("save_completed_attempt")) }
            OutlinedButton(enabled = !busy && !recording && !importing, onClick = {
                model.finish(false, emptyList(), observation, reflection, next, items) { saved(draft.id) }
            }) { Text(tr("save_as_incomplete")) }
            TextButton(enabled = !busy && !recording && !importing, onClick = { model.timer("back", notes) }) { Text(tr("return_to_timer")) }
        }
    }
    TextButton(onClick = { confirm = "discard" }, enabled = !busy && !recording && !importing) { Text(tr("discard_practice")) }
    if (confirm.isNotBlank()) AlertDialog(onDismissRequest = { confirm = "" }, title = { Text(tr("discard_this_practice")) },
        text = { Text(tr("the_timer_notes_and_attachments_from_this_attempt_will_be_deleted")) },
        confirmButton = { TextButton(onClick = { confirm = ""; model.discard() }) { Text(tr("discard")) } },
        dismissButton = { TextButton(onClick = { confirm = "" }) { Text(tr("keep_practising")) } })
}
