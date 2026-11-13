package com.skillhando.app

import java.util.Locale

data class Challenge(
    val id: String, val title: String, val objective: String, val materials: String,
    val steps: List<String>, val criterion: String, val prompt: String, val experiment: String,
    val minutes: Int, val level: Int
)

data class Skill(val id: String, val title: String, val description: String, val rubric: List<String>, val challenges: List<Challenge>)

data class Recommendation(val challenge: Challenge, val focus: Int?, val reason: Reason) {
    enum class Reason { BASELINE, BUILD, REPEAT, MAINTAIN }
}

object PracticeEngine {
    fun recommend(skill: Skill, history: List<Attempt>, experience: Int = 0): Recommendation {
        val comparable = history.filter { it.skill == skill.id && it.complete }
            .sortedWith(compareByDescending<Attempt> { it.finishedAt }.thenByDescending { it.id })
        val last = comparable.firstOrNull()
        if (last == null) return Recommendation(skill.challenges[experience.coerceIn(skill.challenges.indices)], null, Recommendation.Reason.BASELINE)
        val current = skill.challenges.find { it.id == last.challenge } ?: skill.challenges.first()
        val previous = comparable.drop(1).firstOrNull { it.challenge == last.challenge && it.ratings.size == skill.rubric.size }
        val focus = last.ratings.indices.minWithOrNull(compareBy<Int> { last.ratings[it] }
            .thenBy { (last.ratings[it] - (previous?.ratings?.get(it) ?: last.ratings[it])) })
        if (focus != null && last.ratings[focus] < 3) return Recommendation(current, focus, Recommendation.Reason.REPEAT)
        val next = skill.challenges.firstOrNull { candidate -> comparable.none { it.challenge == candidate.id } }
        return Recommendation(next ?: current, focus, if (next == null) Recommendation.Reason.MAINTAIN else Recommendation.Reason.BUILD)
    }

    fun comparableAttempt(attempt: Attempt, history: List<Attempt>): Attempt? =
        history.filter { it.id != attempt.id && it.complete && it.skill == attempt.skill &&
            it.challenge == attempt.challenge && it.ratings.size == attempt.ratings.size &&
            (it.finishedAt < attempt.finishedAt || it.finishedAt == attempt.finishedAt && it.id < attempt.id) }
            .maxWithOrNull(compareBy<Attempt> { it.finishedAt }.thenBy { it.id })

    fun suggest(skills: List<Skill>, history: List<Attempt>, request: String, preferred: String?): Skill {
        val words = Regex("[\\p{L}\\p{N}]{3,}").findAll(request.lowercase(Locale.ROOT)).map { it.value }.toSet()
        val matches = skills.map { skill ->
            val text = (skill.title + " " + skill.description + " " + skill.challenges.joinToString(" ") { it.title })
                .lowercase(Locale.ROOT)
            skill to words.count { Regex("\\b" + Regex.escape(it) + "\\b").containsMatchIn(text) }
        }
        return matches.maxByOrNull { it.second }?.takeIf { it.second > 0 }?.first
            ?: skills.find { it.id == preferred }
            ?: skills.minBy { skill -> history.count { it.complete && it.skill == skill.id } }
    }
}
