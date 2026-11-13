package com.skillhando.app

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "attempts", indices = [Index("skill"), Index("finishedAt")])
data class Attempt(
    @PrimaryKey val id: String, val skill: String, val challenge: String, val finishedAt: Long,
    val seconds: Int, val complete: Boolean, val ratingValues: String, val reflection: String,
    val nextChange: String, val notes: String, val observation: String = "", val baseline: String = "", val experiment: String = ""
) {
    @get:Ignore val ratings get() = ratingValues.split(",").mapNotNull(String::toIntOrNull)
}

@Entity(tableName = "pathways")
data class Pathway(@PrimaryKey val skill: String, val paused: Boolean, val startedAt: Long)

@Entity(tableName = "goals")
data class Goal(@PrimaryKey val id: String, val label: String, val weeklySessions: Int, val createdAt: Long, val paused: Boolean = false)

@Entity(tableName = "evidence", indices = [Index("attemptId")], foreignKeys = [
    ForeignKey(entity = Attempt::class, parentColumns = ["id"], childColumns = ["attemptId"], onDelete = ForeignKey.CASCADE)
])
data class Evidence(@PrimaryKey val id: String, val attemptId: String, val name: String, val kind: String, val fileName: String)

@Entity(tableName = "drafts")
data class Draft(
    @PrimaryKey val slot: Int = 1, val id: String, val skill: String, val challenge: String, val targetMinutes: Int,
    val elapsed: Int = 0, val runningSince: Long? = null, val boot: Int = -1, val notes: String = "",
    val phase: String = "ready", val baseline: String = "", val change: String = ""
) {
    fun elapsedAt(now: Long, currentBoot: Int): Int {
        val active = if (currentBoot == boot) runningSince?.let { ((now - it) / 1000).coerceAtLeast(0) } else null
        return (elapsed.toLong() + (active ?: 0)).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    }
}

@Dao
abstract class LearningDao {
    @Query("SELECT * FROM attempts ORDER BY finishedAt DESC, id DESC") abstract fun attempts(): Flow<List<Attempt>>
    @Query("SELECT * FROM pathways ORDER BY startedAt") abstract fun pathways(): Flow<List<Pathway>>
    @Query("SELECT * FROM goals ORDER BY createdAt DESC, id DESC") abstract fun goals(): Flow<List<Goal>>
    @Query("SELECT * FROM evidence") abstract fun evidence(): Flow<List<Evidence>>
    @Query("SELECT * FROM drafts WHERE slot = 1") abstract fun draft(): Flow<Draft?>
    @Query("SELECT * FROM drafts WHERE slot = 1") abstract suspend fun currentDraft(): Draft?
    @Query("SELECT * FROM pathways") abstract suspend fun currentPathways(): List<Pathway>
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun saveDraft(draft: Draft)
    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun insertAttempt(attempt: Attempt): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun savePathway(pathway: Pathway)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun saveGoal(goal: Goal)
    @Insert abstract suspend fun addEvidence(evidence: Evidence)
    @Query("UPDATE evidence SET name = :name WHERE id = :id") abstract suspend fun renameEvidence(id: String, name: String)
    @Query("DELETE FROM evidence WHERE id = :id") abstract suspend fun removeEvidence(id: String)
    @Query("DELETE FROM drafts WHERE id = :id") abstract suspend fun removeDraft(id: String)
    @Query("DELETE FROM attempts") abstract suspend fun clearAttempts()
    @Query("DELETE FROM pathways") abstract suspend fun clearPathways()
    @Query("DELETE FROM goals") abstract suspend fun clearGoals()
    @Query("DELETE FROM drafts") abstract suspend fun clearDrafts()

    @Transaction
    open suspend fun setPathway(skill: String, paused: Boolean, limit: Int, now: Long): Boolean {
        val pathways = currentPathways()
        val existing = pathways.find { it.skill == skill }
        if (!paused && existing?.paused != false && pathways.count { !it.paused } >= limit) return false
        savePathway(Pathway(skill, paused, existing?.startedAt ?: now))
        return true
    }

    @Transaction
    open suspend fun finish(attempt: Attempt, evidence: List<Evidence>): Boolean {
        if (currentDraft()?.id != attempt.id || insertAttempt(attempt) == -1L) return false
        evidence.forEach { addEvidence(it) }
        removeDraft(attempt.id)
        return true
    }

    @Transaction
    open suspend fun erase() {
        clearAttempts()
        clearPathways()
        clearGoals()
        clearDrafts()
    }
}

@Database(entities = [Attempt::class, Draft::class, Pathway::class, Goal::class, Evidence::class], version = 1, exportSchema = true)
abstract class LearningDatabase : RoomDatabase() {
    abstract fun learning(): LearningDao
    companion object {
        fun open(context: Context) = Room.databaseBuilder(context.applicationContext, LearningDatabase::class.java, "skillhando.db").build()
    }
}
