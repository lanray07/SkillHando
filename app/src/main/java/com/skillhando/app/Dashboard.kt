package com.skillhando.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

enum class Section(private val key: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TODAY("today", Icons.Default.Home), SKILLS("skills", Icons.AutoMirrored.Filled.List),
    PRACTICE("practice", Icons.Default.PlayArrow), PROGRESS("progress", Icons.Default.CheckCircle),
    MORE("more", Icons.Default.MoreVert);
    val title get() = tr(key)
}

@Composable
fun SkillHando(model: LearningModel, voice: Voice, media: PracticeMedia) {
    var section by rememberSaveable { mutableStateOf(Section.TODAY) }
    var skillId by rememberSaveable { mutableStateOf<String?>(null) }
    var challengeId by rememberSaveable { mutableStateOf<String?>(null) }
    var morePage by rememberSaveable { mutableStateOf("") }
    var progressAttempt by rememberSaveable { mutableStateOf<String?>(null) }
    var onboarding by rememberSaveable { mutableStateOf(!model.preferences.getBoolean("welcome", false)) }
    var speech by rememberSaveable { mutableStateOf(model.preferences.getBoolean("speech", false)) }
    val premium = Billing.state.collectAsStateWithLifecycle().value.active
    val attempts by model.attempts.collectAsStateWithLifecycle()
    val pathways by model.pathways.collectAsStateWithLifecycle()
    val goals by model.goals.collectAsStateWithLifecycle()
    val evidence by model.evidence.collectAsStateWithLifecycle()
    val draft by model.draft.collectAsStateWithLifecycle()
    val notice by model.notice.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val screenState = rememberSaveableStateHolder()
    val snackbar = remember { SnackbarHostState() }
    val favourite = model.preferences.getString("interest", "speaking")
    val suggested = model.skills.find { it.id == pathways.firstOrNull { pathway -> !pathway.paused }?.skill }
        ?: model.skills.find { it.id == favourite } ?: model.skills.first()
    val recommendation = PracticeEngine.recommend(suggested, attempts, model.preferences.getInt("experience", 0))
    val selected = model.skills.find { it.id == skillId }
    val selectedChallenge = selected?.challenges?.find { it.id == challengeId }

    fun openChallenge(skill: Skill, challenge: Challenge) {
        skillId = skill.id
        challengeId = challenge.id
        section = Section.SKILLS
    }

    var lastDraft by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(draft?.id) {
        if (lastDraft != draft?.id && lastDraft != null) screenState.removeState("PRACTICE/null/null//null")
        lastDraft = draft?.id
    }

    LaunchedEffect(notice) {
        notice?.let { snackbar.showSnackbar(it); model.notice.value = null }
    }

    androidx.activity.compose.BackHandler(onboarding || skillId != null || morePage.isNotEmpty() || progressAttempt != null || section != Section.TODAY) {
        when {
            onboarding -> { model.preferences.edit().putBoolean("welcome", true).apply(); onboarding = false }
            section == Section.SKILLS && challengeId != null -> challengeId = null
            section == Section.SKILLS && skillId != null -> skillId = null
            section == Section.MORE && morePage.isNotBlank() -> morePage = ""
            section == Section.PROGRESS && progressAttempt != null -> progressAttempt = null
            else -> { section = Section.TODAY; skillId = null; challengeId = null; morePage = ""; progressAttempt = null }
        }
    }

    fun navigate(destination: Section) {
        section = destination
        skillId = null
        challengeId = null
        morePage = ""
        progressAttempt = null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {},
        containerColor = MaterialTheme.colorScheme.background
    ) { insets ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(insets)) {
            val wide = maxWidth >= 720.dp
            if (onboarding) {
                SkillPage(Modifier.fillMaxSize()) {
                    Welcome(model, voice, speech, { speech = it }) {
                        model.preferences.edit().putBoolean("welcome", true).apply()
                        onboarding = false
                    }
                }
            } else {
                Row(Modifier.fillMaxSize()) {
                    if (wide) NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        Image(painterResource(R.drawable.ic_hando), null, Modifier.padding(12.dp).size(54.dp).background(Color(0xFF303A72), RoundedCornerShape(16.dp)))
                        Spacer(Modifier.height(18.dp))
                        Section.entries.forEach { item ->
                            NavigationRailItem(section == item, { navigate(item) }, { Icon(item.icon, null) }, label = { Text(item.title) })
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        val route = listOf(section.name, skillId, challengeId, morePage, progressAttempt).joinToString("/")
                        key(route) {
                            screenState.SaveableStateProvider(route) {
                                SkillPage(Modifier.weight(1f).background(MaterialTheme.colorScheme.background)) {
                                    when (section) {
                                        Section.TODAY -> Today(suggested, recommendation, attempts, goals, draft != null,
                                            { openChallenge(suggested, recommendation.challenge) }, { navigate(Section.PRACTICE) },
                                            { navigate(Section.SKILLS) }, { navigate(Section.PROGRESS) })
                                        Section.SKILLS -> when {
                                            selected == null -> Skills(model.skills, pathways, attempts, wide) { skillId = it.id }
                                            selectedChallenge == null -> PathwayPage(selected, pathways.find { it.skill == selected.id }, attempts,
                                                { skillId = null }, { model.pathway(selected.id, it, premium) }, { openChallenge(selected, it) })
                                            else -> ChallengePage(selected, selectedChallenge, attempts, model, voice, speech, busy,
                                                { challengeId = null }, { navigate(Section.PRACTICE) })
                                        }
                                        Section.PRACTICE -> if (draft == null) {
                                            Heading(tr("practice"), tr("turn_one_small_exercise_into_something_you_can_learn_from"))
                                            ActionCard(tr("pick_a_challenge"), tr("choose_the_point_you_want_to_practise_and_what_you_will_look_for"), tr("browse_skills")) { navigate(Section.SKILLS) }
                                        } else {
                                            val active = model.skills.first { it.id == draft!!.skill }
                                            GuidedPractice(draft!!, active, active.challenges.first { it.id == draft!!.challenge },
                                                model, voice, media, speech, premium, evidence.size, busy) { id ->
                                                navigate(Section.PROGRESS)
                                                progressAttempt = id
                                            }
                                        }
                                        Section.PROGRESS -> ProgressPage(model, attempts, goals, evidence, media, premium,
                                            progressAttempt, { progressAttempt = it }, { openChallenge(it.first, it.second) })
                                        Section.MORE -> MorePage(model, voice, speech, { speech = it }, attempts, goals, suggested, recommendation,
                                            morePage, { morePage = it }, { navigate(Section.PROGRESS) }, { navigate(Section.PRACTICE) },
                                            { openChallenge(it.first, it.second) })
                                    }
                                }
                            }
                        }
                        if (!wide) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                            Section.entries.forEach { item ->
                                NavigationBarItem(section == item, { navigate(item) }, { Icon(item.icon, null) }, label = { Text(item.title) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SkillPage(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.widthIn(max = 1110.dp).fillMaxWidth().padding(horizontal = 22.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(17.dp), content = content)
    }
}

@Composable
fun Heading(title: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.displaySmall, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium)
        if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Panel(content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(19.dp), verticalArrangement = Arrangement.spacedBy(9.dp), content = content)
    }
}

@Composable
fun Subheading(title: String) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }

@Composable
fun ActionCard(title: String, detail: String, label: String, click: () -> Unit) = Panel {
    Subheading(title)
    Text(detail)
    FilledTonalButton(onClick = click) { Text(label) }
}

@Composable
fun Today(skill: Skill, suggestion: Recommendation, attempts: List<Attempt>, goals: List<Goal>, draft: Boolean,
          open: () -> Unit, resume: () -> Unit, browse: () -> Unit, progress: () -> Unit) {
    Text("SkillHando", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    Heading(tr("make_time_to_get_better"), tr("practise_something_small_notice_what_changes"))
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF303A72), contentColor = Color.White)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide = maxWidth >= 620.dp
            if (wide) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(26.dp)) { HeroCopy(skill, suggestion, draft, open, resume) }
                    Image(painterResource(R.drawable.practice_art), null, Modifier.weight(1f).height(260.dp), contentScale = ContentScale.Crop)
                }
            } else {
                Column {
                    Image(painterResource(R.drawable.practice_art), null, Modifier.fillMaxWidth().height(148.dp), contentScale = ContentScale.Crop)
                    Column(Modifier.padding(21.dp)) { HeroCopy(skill, suggestion, draft, open, resume) }
                }
            }
        }
    }
    val recent = attempts.firstOrNull { it.complete }
    val activeGoal = goals.firstOrNull { !it.paused }
    if (recent != null) ActionCard(tr("take_a_look_back"), recent.nextChange.ifBlank { tr("compare_your_recent_evidence_and_self_ratings") }, tr("review_progress"), progress)
    if (activeGoal != null) Panel {
        Subheading(tr("weekly_practice_goal"))
        Text(activeGoal.label)
        val count = attempts.count { it.complete && inPeriod(it.finishedAt, Period.WEEK) }
        LinearProgressIndicator(progress = { (count.toFloat() / activeGoal.weeklySessions).coerceAtMost(1f) }, modifier = Modifier.fillMaxWidth())
        Text(trf("weekly_progress", count, activeGoal.weeklySessions))
    }
    ActionCard(tr("explore_practical_skills"), tr("photography_communication_digital_literacy_and_more_challenges_are_available_off"), tr("find_a_skill"), browse)
}

@Composable
private fun HeroCopy(skill: Skill, suggestion: Recommendation, draft: Boolean, open: () -> Unit, resume: () -> Unit) {
    Text(skill.title, color = Color(0xFFD0EDEA), style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(6.dp))
    Text(suggestion.challenge.title, style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif)
    Spacer(Modifier.height(8.dp))
    Text(recommendationText(skill, suggestion), color = Color(0xFFE5E6F3))
    Spacer(Modifier.height(14.dp))
    Button(onClick = if (draft) resume else open, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDAEFEA), contentColor = Color(0xFF1B4B46))) {
        Text(if (draft) tr("continue_practice") else tr("see_today_s_challenge"))
    }
}

fun recommendationText(skill: Skill, suggestion: Recommendation): String = when (suggestion.reason) {
    Recommendation.Reason.BASELINE -> tr("create_a_baseline_you_can_compare_with_your_next_attempt")
    Recommendation.Reason.REPEAT -> trf("repeat_for_criterion", skill.rubric[suggestion.focus!!])
    Recommendation.Reason.BUILD -> tr("build_on_your_last_completed_attempt_with_a_different_challenge")
    Recommendation.Reason.MAINTAIN -> tr("try_the_same_exercise_again_and_compare_the_evidence_under_similar_conditions")
}

@Composable
fun Skills(skills: List<Skill>, paths: List<Pathway>, attempts: List<Attempt>, wide: Boolean, select: (Skill) -> Unit) {
    Heading(tr("skills"), tr("choose_a_subject_and_work_through_short_practice_challenges"))
    skills.chunked(if (wide) 2 else 1).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            row.forEach { skill ->
                val count = skill.challenges.count { c -> attempts.any { it.complete && it.challenge == c.id } }
                val path = paths.find { it.skill == skill.id }
                OutlinedCard(onClick = { select(skill) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Subheading(skill.title)
                        Text(skill.description)
                        Text(if (path?.paused == true) trf("path_paused", count, skill.challenges.size) else trf("path_count", count, skill.challenges.size),
                            color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            if (wide && row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun PathwayPage(skill: Skill, path: Pathway?, attempts: List<Attempt>, back: () -> Unit, toggle: (Boolean) -> Unit,
                open: (Challenge) -> Unit) {
    TextButton(onClick = back) { Text(tr("all_skills")) }
    Heading(skill.title, skill.description)
    val next = PracticeEngine.recommend(skill, attempts)
    Panel {
        Subheading(if (skill.challenges.all { c -> attempts.any { it.complete && it.challenge == c.id } }) tr("pathway_milestone_reached") else tr("recommended_next"))
        Text(next.challenge.title)
        Text(recommendationText(skill, next))
        Button(onClick = { open(next.challenge) }) { Text(tr("review_challenge")) }
    }
    OutlinedButton(onClick = { toggle(path?.paused == false) }) {
        Text(if (path == null) tr("start_pathway") else if (path.paused) tr("resume_pathway") else tr("pause_pathway"))
    }
    skill.challenges.forEach { challenge ->
        val level = listOf(tr("beginner"), tr("intermediate"), tr("advanced"))[challenge.level]
        val count = attempts.count { it.complete && it.challenge == challenge.id }
        ActionCard(challenge.title, trf("challenge_duration", level, challenge.minutes) + "\n" + challenge.objective,
            if (count > 0) tr("practise_again") else tr("view_exercise")) { open(challenge) }
    }
}

@Composable
fun ChallengePage(skill: Skill, challenge: Challenge, history: List<Attempt>, model: LearningModel, voice: Voice,
                  speech: Boolean, busy: Boolean, back: () -> Unit, open: () -> Unit) {
    var baseline by rememberSaveable { mutableStateOf("") }
    val previous = history.firstOrNull { it.complete && it.challenge == challenge.id }
    var change by rememberSaveable { mutableStateOf(previous?.nextChange.orEmpty()) }
    var duration by rememberSaveable { mutableStateOf(model.preferences.getInt("duration", challenge.minutes).toString()) }
    TextButton(onClick = back) { Text(skill.title) }
    Heading(challenge.title, challenge.objective)
    Panel {
        Subheading(tr("before_you_start"))
        Text(challenge.materials)
        challenge.steps.forEachIndexed { i, step -> Text("${i + 1}. $step") }
        Subheading(tr("what_to_look_for"))
        Text(challenge.criterion)
        if (speech) OutlinedButton(onClick = { voice.speak(challenge.steps.joinToString(". "), Localisation.locale) }) { Text(tr("read_instructions_aloud")) }
        if (speech && voice.status.isNotBlank()) Text(voice.status)
    }
    Panel {
        Subheading(tr("plan_your_attempt"))
        if (previous != null) Text(tr("last_observation") + previous.observation.ifBlank { previous.reflection.ifBlank { tr("no_observation_saved") } })
        Text(tr("if_you_re_repeating_this_challenge_keep_the_conditions_similar_and_change_one_th"))
        Entry(baseline, { baseline = it }, tr("what_will_you_look_for"))
        Entry(change, { change = it }, tr("one_change_to_try"), placeholder = challenge.experiment)
        OutlinedTextField(duration, { if (it.length <= 3 && it.all(Char::isDigit)) duration = it }, label = { Text(tr("target_minutes_1_120")) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), singleLine = true)
        Button(onClick = { model.begin(skill, challenge, duration.toInt(), baseline, change, open) }, enabled = !busy && duration.toIntOrNull() in 1..120) {
            Text(tr("open_guided_practice"))
        }
    }
}

@Composable
fun Entry(value: String, set: (String) -> Unit, label: String, placeholder: String = "", limit: Int = 2000) {
    OutlinedTextField(value, { set(it.take(limit)) }, Modifier.fillMaxWidth(), label = { Text(label) },
        placeholder = { if (placeholder.isNotBlank()) Text(placeholder) }, maxLines = 5)
}
