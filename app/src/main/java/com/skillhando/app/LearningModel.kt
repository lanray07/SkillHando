package com.skillhando.app

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class LearningModel(app: Application) : AndroidViewModel(app) {
    private val dao = LearningDatabase.open(app).learning()
    private var catalogueLocale = Localisation.locale
    private var catalogue = Catalogue(app).skills
    val skills: List<Skill> get() {
        if (catalogueLocale != Localisation.locale) {
            catalogueLocale = Localisation.locale
            catalogue = Catalogue(getApplication()).skills
        }
        return catalogue
    }
    val boot get() = Settings.Global.getInt(getApplication<Application>().contentResolver, "boot_count", -1)
    val preferences = app.getSharedPreferences("settings", 0)
    val attempts = dao.attempts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val pathways = dao.pathways().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val goals = dao.goals().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val evidence = dao.evidence().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val draft = dao.draft().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val notice = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val vault get() = File(getApplication<Application>().filesDir, "evidence").apply { mkdirs() }

    private fun write(block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { notice.value = tr("skillhando_couldn_t_save_the_change_please_try_again") }
            finally { busy.value = false }
        }
    }

    fun begin(skill: Skill, challenge: Challenge, duration: Int, baseline: String, change: String, done: () -> Unit) = write {
        if (dao.currentDraft() != null) {
            notice.value = tr("finish_or_discard_your_current_practice_first")
        } else {
            dao.saveDraft(Draft(id = UUID.randomUUID().toString(), skill = skill.id, challenge = challenge.id,
                targetMinutes = duration.coerceIn(1, 120), baseline = baseline.trim(), change = change.trim()))
        }
        done()
    }

    fun timer(action: String, notes: String) = write {
        val current = dao.currentDraft() ?: return@write
        val now = SystemClock.elapsedRealtime()
        val elapsed = current.elapsedAt(now, boot)
        val updated = when (action) {
            "start" -> if (current.phase == "review") current else current.copy(
                phase = "active", elapsed = elapsed, runningSince = now, boot = boot, notes = notes)
            "pause" -> current.copy(elapsed = elapsed, runningSince = null, notes = notes)
            "review" -> current.copy(elapsed = elapsed, runningSince = null, phase = "review", notes = notes)
            "back" -> current.copy(phase = "active", runningSince = null, notes = notes)
            else -> current
        }
        dao.saveDraft(updated)
    }

    fun discard() = write {
        val active = dao.currentDraft() ?: return@write
        dao.removeDraft(active.id)
        withContext(Dispatchers.IO) { vault.listFiles()?.filter { it.name.startsWith(active.id + "_") }?.forEach { it.delete() } }
    }

    fun finish(complete: Boolean, ratings: List<Int>, observation: String, reflection: String,
               next: String, attachments: List<Evidence>, done: () -> Unit) = write {
        val current = dao.currentDraft() ?: return@write
        val skill = skills.first { it.id == current.skill }
        if (complete && (ratings.size != skill.rubric.size || ratings.any { it !in 1..5 } || (observation.isBlank() && reflection.isBlank()))) return@write
        val attempt = Attempt(current.id, current.skill, current.challenge, System.currentTimeMillis(),
            current.elapsedAt(SystemClock.elapsedRealtime(), boot), complete,
            if (complete) ratings.joinToString(",") else "", reflection.trim(), next.trim(), current.notes, observation.trim(),
            current.baseline, current.change)
        if (dao.finish(attempt, attachments)) done()
    }

    fun fileFor(id: String, kind: String) = File(vault, id + "_" + UUID.randomUUID() + if (kind == "audio") ".m4a" else ".image")

    suspend fun importPhoto(id: String, uri: Uri): Evidence = withContext(Dispatchers.IO) {
        val resolver = getApplication<Application>().contentResolver
        require(resolver.getType(uri)?.startsWith("image/") == true)
        val file = fileFor(id, "photo")
        try {
            requireNotNull(resolver.openInputStream(uri)).use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= 20_000_000)
                        output.write(buffer, 0, read)
                    }
                    require(total > 0)
                }
            }
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeFile(file.path, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0)
            Evidence(UUID.randomUUID().toString(), id, tr("practice_photograph"), "photo", file.name)
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }

    fun removeEvidence(item: Evidence) = write {
        dao.removeEvidence(item.id)
        withContext(Dispatchers.IO) { File(vault, item.fileName).delete() }
    }

    fun renameEvidence(item: Evidence, name: String) = write {
        if (name.isNotBlank()) dao.renameEvidence(item.id, name.trim().take(100))
    }

    fun pathway(skill: String, paused: Boolean, premium: Boolean) = write {
        if (!dao.setPathway(skill, paused, if (premium) Int.MAX_VALUE else 2, System.currentTimeMillis()))
            notice.value = tr("two_pathways_are_active_pause_one_to_make_room_for_another")
    }

    fun goal(label: String, target: Int) = write {
        if (label.isNotBlank()) dao.saveGoal(Goal(UUID.randomUUID().toString(), label.trim().take(120), target.coerceIn(1, 50), System.currentTimeMillis()))
    }

    fun toggleGoal(goal: Goal) = write { dao.saveGoal(goal.copy(paused = !goal.paused)) }

    fun erase() = write {
        dao.erase()
        withContext(Dispatchers.IO) { vault.listFiles()?.forEach { it.delete() } }
        preferences.edit().remove("reminders").apply()
        Reminders.cancel(getApplication<Application>())
        notice.value = tr("your_learning_records_and_attachments_were_deleted")
    }
}
