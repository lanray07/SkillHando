package com.skillhando.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LearningDatabaseTest {
    @Test fun commitsAttemptWithEvidenceAndReopensDatabase() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "practice-test-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, LearningDatabase::class.java, name).build()
        try {
            open().closing { db ->
                val dao = db.learning()
                dao.saveDraft(Draft(id = "attempt-1", skill = "speaking", challenge = "speaking-baseline", targetMinutes = 5))
                val attempt = Attempt("attempt-1", "speaking", "speaking-baseline", 100L, 61, true,
                    "3,2,3", "I rushed my example.", "Pause before the example.", "", "Example lasted six seconds.")
                val photo = Evidence("ev-1", attempt.id, "Practice photograph", "photo", "attempt-1_example.image")
                assertTrue(dao.finish(attempt, listOf(photo)))
                assertFalse(dao.finish(attempt, listOf(photo)))
                assertNull(dao.currentDraft())
            }
            open().closing { db ->
                val dao = db.learning()
                assertEquals("Example lasted six seconds.", dao.attempts().first().single().observation)
                assertEquals("ev-1", dao.evidence().first().single().id)
                dao.erase()
                assertTrue(dao.attempts().first().isEmpty())
                assertTrue(dao.evidence().first().isEmpty())
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test fun freePathLimitAllowsPausingAndResumingWithoutLosingRecords() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java).build().closing { db ->
            val dao = db.learning()
            assertTrue(dao.setPathway("a", false, 2, 1L))
            assertTrue(dao.setPathway("b", false, 2, 2L))
            assertFalse(dao.setPathway("c", false, 2, 3L))
            assertTrue(dao.setPathway("a", true, 2, 4L))
            assertTrue(dao.setPathway("c", false, 2, 5L))
            assertTrue(dao.currentPathways().single { it.skill == "a" }.paused)
            assertTrue(dao.setPathway("a", false, 3, 6L))
            assertEquals(1L, dao.currentPathways().single { it.skill == "a" }.startedAt)
        }
    }
}

private suspend fun <T> LearningDatabase.closing(block: suspend (LearningDatabase) -> T): T =
    try { block(this) } finally { close() }
