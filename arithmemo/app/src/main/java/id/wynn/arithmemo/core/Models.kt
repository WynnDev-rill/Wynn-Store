package id.wynn.arithmemo.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.serialization.Serializable

@Serializable
enum class StudyMode(val label: String) {
    FLASHCARD("Flashcard"),
    RECALL("Latihan mengingat"),
    SPEED("Tes kecepatan"),
    ADAPTIVE("Ulangi terjadwal"),
    RANDOM("Soal acak"),
}

@Serializable
data class UserSettings(
    val config: PracticeConfig = PracticeConfig(setOf(Operation.MULTIPLY), 1),
    val goal: Int = 20,
    val theme: String = "system",
    val haptics: Boolean = true,
) {
    fun validate() {
        config.validate()
        require(goal in 5..200)
        require(theme in setOf("system", "light", "dark"))
    }
}

@Serializable
data class Attempt(
    val id: String,
    val fact: Fact,
    val correct: Boolean,
    val elapsedMs: Long,
    val rating: Rating,
    val at: Long,
) {
    fun validate() {
        require(id.length in 1..100)
        require(elapsedMs in 0..86_400_000)
        require(at in 0..253402300799000)
        fact.validate()
    }
}

@Serializable
data class StudySession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val mode: StudyMode,
    val cards: List<Fact>,
    val index: Int = 0,
    val correct: Int = 0,
    val answered: Int = 0,
    val remainingMs: Long = 60_000,
    val answerMs: Long = 0,
    val revealed: Boolean = false,
    val feedback: Boolean? = null,
    val typed: String = "",
    val active: Boolean = true,
) {
    val finished
        get() =
            !active ||
                (mode != StudyMode.SPEED && index >= cards.size) ||
                (mode == StudyMode.SPEED && remainingMs <= 0)

    val current
        get() = cards.getOrNull(index)

    fun validate() {
        require(cards.size in 1..200)
        cards.forEach { it.validate() }
        require(index in 0..cards.size)
        require(correct in 0..answered && answered in 0..100000)
        require(remainingMs in 0..60000)
        require(answerMs in 0..86400000)
        require(typed.length <= 7)
        require(id.length in 1..100)
    }
}

@Serializable
data class Backup(
    val schema: Int = 1,
    val settings: UserSettings = UserSettings(),
    val progress: Map<String, Progress> = emptyMap(),
    val attempts: List<Attempt> = emptyList(),
) {
    fun validate() {
        require(schema == 1) { "Versi cadangan tidak didukung" }
        settings.validate()
        require(progress.size <= 200000 && attempts.size <= 200000)
        progress.forEach { (key, p) ->
            Fact.parse(key)
            p.validate()
        }
        attempts.forEach { it.validate() }
        require(attempts.map { it.id }.toSet().size == attempts.size)
    }
}

data class Snapshot(
    val settings: UserSettings = UserSettings(),
    val progress: Map<String, Progress> = emptyMap(),
    val attempts: List<Attempt> = emptyList(),
    val session: StudySession? = null,
)

data class Stats(
    val total: Int,
    val correct: Int,
    val averageMs: Long,
    val byDay: Map<LocalDate, Int>,
) {
    val accuracy
        get() = if (total == 0) 0 else (correct * 100.0 / total).toInt()

    fun today(date: LocalDate = LocalDate.now()) = byDay[date] ?: 0

    fun streak(date: LocalDate = LocalDate.now()): Int {
        var day = if (today(date) > 0) date else date.minusDays(1)
        var count = 0
        while ((byDay[day] ?: 0) > 0) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    companion object {
        fun from(attempts: List<Attempt>, zone: ZoneId = ZoneId.systemDefault()): Stats {
            val typed = attempts.filter { it.elapsedMs > 0 && it.correct }
            return Stats(
                attempts.size,
                attempts.count { it.correct },
                if (typed.isEmpty()) 0 else typed.sumOf { it.elapsedMs } / typed.size,
                attempts
                    .groupingBy { Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() }
                    .eachCount(),
            )
        }
    }
}
