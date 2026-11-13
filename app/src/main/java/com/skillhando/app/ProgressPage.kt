package com.skillhando.app

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Date

enum class Period(private val key: String) { DAY("today"), WEEK("this_week"), MONTH("this_month"), ALL("all_time"); val caption get() = tr(key) }

fun inPeriod(timestamp: Long, period: Period, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Boolean {
    val start = when (period) {
        Period.DAY -> today
        Period.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        Period.MONTH -> today.withDayOfMonth(1)
        Period.ALL -> return true
    }
    return timestamp >= start.atStartOfDay(zone).toInstant().toEpochMilli() &&
        timestamp < today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
}
private fun date(timestamp: Long) = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Localisation.locale).format(Date(timestamp))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressPage(model: LearningModel, attempts: List<Attempt>, goals: List<Goal>, evidence: List<Evidence>, media: PracticeMedia,
                 premium: Boolean, selected: String?, select: (String?) -> Unit, open: (Pair<Skill, Challenge>) -> Unit) {
    val detail = attempts.find { it.id == selected }
    if (detail != null) {
        val skill = model.skills.first { it.id == detail.skill }
        val challenge = skill.challenges.first { it.id == detail.challenge }
        TextButton(onClick = { select(null) }) { Text(tr("back_to_progress")) }
        Heading(challenge.title, date(detail.finishedAt))
        AttemptRecord(detail)
        val previous = if (detail.complete) PracticeEngine.comparableAttempt(detail, attempts) else null
        val ownEvidence = evidence.filter { it.attemptId == detail.id }
        if (detail.complete) Panel {
            Subheading(if (previous == null) tr("your_baseline") else tr("compare_with_your_previous_attempt"))
            if (previous != null) Text(date(previous.finishedAt))
            Text(tr("these_ratings_are_your_own_observations_compare_the_records_under_similar_condit"))
            skill.rubric.forEachIndexed { i, label ->
                val current = detail.ratings.getOrNull(i) ?: return@forEachIndexed
                val earlier = previous?.ratings?.getOrNull(i)
                Text(if (earlier == null) "$label · $current/5" else "$label · $earlier/5 → $current/5")
            }
            if (previous != null) {
                if (previous.experiment.isNotBlank()) Text(trf("previous_experiment", previous.experiment))
                if (previous.observation.isNotBlank()) Text(trf("previous_observation", previous.observation))
                if (previous.reflection.isNotBlank()) Text(trf("previous_reflection", previous.reflection))
                TextButton(onClick = { select(previous.id) }) { Text(tr("open_earlier_attempt")) }
            }
        }
        val previousEvidence = if (previous == null) emptyList() else evidence.filter { it.attemptId == previous.id }
        if (ownEvidence.isNotEmpty() || previousEvidence.isNotEmpty()) {
            Subheading(tr("compare_evidence"))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (previousEvidence.isNotEmpty() && maxWidth >= 650.dp) Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tr("earlier_attempt"))
                        previousEvidence.forEach { Attachment(it, model, media, false) }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tr("this_attempt"))
                        ownEvidence.forEach { Attachment(it, model, media, true) }
                    }
                } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (previousEvidence.isNotEmpty()) Text(tr("earlier_attempt"))
                    previousEvidence.forEach { Attachment(it, model, media, false) }
                    Text(tr("this_attempt"))
                    ownEvidence.forEach { Attachment(it, model, media, true) }
                }
            }
        }
        val recommendation = PracticeEngine.recommend(skill, attempts)
        ActionCard(tr("recommended_next"), recommendationText(skill, recommendation), recommendation.challenge.title) {
            open(skill to recommendation.challenge)
        }
        if (recommendation.challenge.id != challenge.id)
            OutlinedButton(onClick = { open(skill to challenge) }) { Text(tr("repeat_this_exact_challenge")) }
        return
    }

    Heading(tr("your_progress"), tr("look_back_at_what_you_tried_what_changed_and_what_you_ll_practise_next"))
    var period by rememberSaveable { mutableStateOf(Period.ALL) }
    var scopeId by rememberSaveable { mutableStateOf("") }
    var focus by rememberSaveable { mutableStateOf(false) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Period.entries.forEach { FilterChip(period == it, { period = it }, { Text(it.caption) }) }
    }
    val practiced = model.skills.filter { s -> attempts.any { it.skill == s.id } }
    if (practiced.size > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(scopeId.isEmpty(), { scopeId = "" }, { Text(tr("all_skills")) })
        practiced.forEach { FilterChip(scopeId == it.id, { scopeId = it.id }, { Text(it.title) }) }
    }
    val visible = attempts.filter { inPeriod(it.finishedAt, period) && (scopeId.isEmpty() || it.skill == scopeId) }
    val complete = visible.filter { it.complete }
    val portfolio = evidence.filter { item -> visible.any { it.id == item.attemptId } }
    val milestones = model.skills.filter { s -> (scopeId.isEmpty() || scopeId == s.id) && s.challenges.all { c -> complete.any { it.challenge == c.id } } }
    Panel {
        Subheading(tr("practice_at_a_glance"))
        Text(trf("practice_totals", complete.size, visible.sumOf { it.seconds } / 60))
        Text(trf("evidence_totals", visible.map { it.challenge }.distinct().size, portfolio.size))
        Text(trf("practice_days", visible.map { java.time.Instant.ofEpochMilli(it.finishedAt).atZone(ZoneId.systemDefault()).toLocalDate() }.distinct().size))
        Text(tr("time_includes_complete_and_incomplete_attempts_a_completion_records_your_review"))
        if (visible.isNotEmpty()) {
            val max = visible.groupBy { it.skill }.values.maxOf { it.sumOf(Attempt::seconds) }.coerceAtLeast(1)
            practiced.forEach { skill ->
                val rows = visible.filter { it.skill == skill.id }
                if (rows.isNotEmpty()) {
                    val seconds = rows.sumOf { it.seconds }
                    Text(skill.title + " · " + trf("duration", seconds / 60, seconds % 60))
                    LinearProgressIndicator(progress = { seconds.toFloat() / max }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
    if (milestones.isNotEmpty()) Panel {
        Subheading(tr("pathway_milestones"))
        milestones.forEach { Text(trf("milestone_line", it.title)) }
    }
    goals.filter { !it.paused }.forEach { goal ->
        val sessions = attempts.count { it.complete && inPeriod(it.finishedAt, Period.WEEK) }
        Panel {
            Subheading(goal.label)
            Text(trf("weekly_progress", sessions, goal.weeklySessions))
            LinearProgressIndicator(progress = { (sessions.toFloat() / goal.weeklySessions).coerceAtMost(1f) }, modifier = Modifier.fillMaxWidth())
        }
    }
    ExportPanel(model, visible, goals, portfolio, premium)
    Subheading(tr("practice_timeline"))
    FilterChip(focus, { focus = !focus }, { Text(tr("with_reflections_or_attachments")) })
    val timeline = if (focus) visible.filter { a -> a.reflection.isNotBlank() || a.observation.isNotBlank() || portfolio.any { it.attemptId == a.id } } else visible
    if (timeline.isEmpty()) Text(tr("no_practice_records_match_this_view"))
    timeline.forEach { attempt ->
        val skill = model.skills.first { it.id == attempt.skill }
        val title = skill.challenges.first { it.id == attempt.challenge }.title
        ActionCard(title, "${skill.title} · ${date(attempt.finishedAt)}\n" +
            (if (attempt.complete) tr("completed") else tr("incomplete")) + " · " + trf("attachment_count", portfolio.count { it.attemptId == attempt.id }),
            tr("review_attempt")) { select(attempt.id) }
    }
}

@Composable
private fun AttemptRecord(attempt: Attempt) = Panel {
    Subheading(if (attempt.complete) tr("completed_practice") else tr("incomplete_practice"))
    Text(trf("duration", attempt.seconds / 60, attempt.seconds % 60))
    listOf("Before" to attempt.baseline, tr("change_tested") to attempt.experiment, tr("what_you_observed") to attempt.observation,
        tr("reflection") to attempt.reflection, tr("next_change") to attempt.nextChange, tr("notes") to attempt.notes).forEach { (label, value) ->
        if (value.isNotBlank()) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(value)
        }
    }
}

@Composable
private fun Attachment(item: Evidence, model: LearningModel, media: PracticeMedia, editable: Boolean) = Panel {
    var name by rememberSaveable(item.id) { mutableStateOf(item.name) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    val file = File(model.vault, item.fileName)
    Text(item.name, style = MaterialTheme.typography.titleMedium)
    if (item.kind == "photo") {
        val bitmap by produceState<android.graphics.Bitmap?>(null, file) {
            value = withContext(Dispatchers.IO) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                val sample = generateSequence(1) { it * 2 }.first { maxOf(bounds.outWidth, bounds.outHeight) / it <= 1500 }
                BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        }
        bitmap?.let { Image(it.asImageBitmap(), item.name, Modifier.fillMaxWidth().heightIn(max = 320.dp), contentScale = ContentScale.Fit) }
    }
    if (item.kind == "audio") OutlinedButton(onClick = {
        if (playing) { media.stopPlayback(); playing = false }
        else runCatching { media.play(file) { playing = false }; playing = true }
            .onFailure { model.notice.value = tr("this_recording_couldn_t_be_played") }
    }) { Text(if (playing) tr("stop_playback") else tr("play_recording")) }
    DisposableEffect(file) { onDispose { if (playing) media.stopPlayback() } }
    if (editable) {
        if (editing) {
            Entry(name, { name = it }, tr("attachment_name"), limit = 100)
            TextButton(onClick = { model.renameEvidence(item, name); editing = false }, enabled = name.isNotBlank()) { Text(tr("save_name")) }
        } else TextButton(onClick = { name = item.name; editing = true }) { Text(tr("rename")) }
        TextButton(onClick = { deleting = true }) { Text(tr("delete_attachment")) }
    }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text(tr("delete_this_attachment")) },
        confirmButton = { TextButton(onClick = { deleting = false; if (playing) media.stopPlayback(); model.removeEvidence(item) }) { Text(tr("delete")) } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text(tr("keep")) } })
}

@Composable
private fun ExportPanel(model: LearningModel, attempts: List<Attempt>, goals: List<Goal>, evidence: List<Evidence>, premium: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var privateData by rememberSaveable { mutableStateOf(false) }
    var action by rememberSaveable { mutableStateOf("") }
    var include by rememberSaveable { mutableStateOf(false) }
    val report = reportFor(attempts, model.skills, goals, evidence, include)
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            try { withContext(Dispatchers.IO) { ReportWriter.write(context, uri, "csv", report) }; model.notice.value = tr("csv_saved") }
            catch (_: java.io.IOException) { model.notice.value = tr("couldn_t_save_the_report") }
            catch (_: SecurityException) { model.notice.value = tr("the_selected_location_couldn_t_be_opened") }
        }
    }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch {
            try { withContext(Dispatchers.IO) { ReportWriter.write(context, uri, "pdf", report) }; model.notice.value = tr("pdf_saved") }
            catch (_: java.io.IOException) { model.notice.value = tr("couldn_t_save_the_report") }
            catch (_: SecurityException) { model.notice.value = tr("the_selected_location_couldn_t_be_opened") }
        }
    }
    fun export(kind: String) {
        when (kind) {
            "csv" -> csv.launch("SkillHando-practice.csv")
            "pdf" -> pdf.launch("SkillHando-practice.pdf")
            else -> context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, reportFor(attempts, model.skills, goals, evidence, false).summary), tr("share_practice_summary")))
        }
    }
    Panel {
        Subheading(tr("export_this_view"))
        Text(tr("reports_use_the_selected_dates_and_skill_attachment_files_stay_in_skillhando"))
        Row { Checkbox(privateData, { privateData = it }); Text(tr("include_personal_notes_and_reflections"), Modifier.padding(top = 13.dp)) }
        OutlinedButton(onClick = { include = privateData; if (privateData) action = "csv" else export("csv") }) { Text(tr("export_csv")) }
        if (premium) OutlinedButton(onClick = { include = privateData; if (privateData) action = "pdf" else export("pdf") }) { Text(tr("export_pdf")) }
        OutlinedButton(onClick = { export("share") }) { Text(tr("share_summary")) }
    }
    if (action.isNotBlank()) AlertDialog(onDismissRequest = { action = "" }, title = { Text(tr("include_private_reflections")) },
        text = { Text(tr("the_saved_copy_can_be_read_outside_skillhando_choose_a_destination_you_trust")) },
        confirmButton = { TextButton(onClick = { export(action); action = "" }) { Text(tr("continue_export")) } },
        dismissButton = { TextButton(onClick = { action = "" }) { Text(tr("cancel")) } })
}
