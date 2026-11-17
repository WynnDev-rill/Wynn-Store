package id.wynn.arithmemo.core

import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.serialization.Serializable

@Serializable
enum class Operation(val symbol: String, val label: String) {
    ADD("+", "Penjumlahan"),
    SUBTRACT("−", "Pengurangan"),
    MULTIPLY("×", "Perkalian"),
    DIVIDE("÷", "Pembagian"),
}

@Serializable
enum class FactGroup(val label: String) {
    ALL("Semua"),
    LOWER("Bagian awal"),
    UPPER("Bagian akhir");

    fun contains(f: Fact, digits: Int): Boolean {
        val range = Facts.range(digits)
        val middle = (range.first + range.last) / 2
        return f.a in range &&
            f.b in range &&
            when (this) {
                ALL -> true
                LOWER -> f.a <= middle
                UPPER -> f.a > middle
            }
    }
}

@Serializable
data class PracticeConfig(
    val operations: Set<Operation> = setOf(Operation.MULTIPLY),
    val digits: Int = 1,
    val group: FactGroup = FactGroup.ALL,
) {
    init {
        validate()
    }

    fun validate() {
        require(operations.isNotEmpty())
        require(digits in 1..3)
    }

    fun matches(f: Fact) = f.op in operations && group.contains(f, digits)

    val description
        get() = "${digits} digit · ${group.label}"
}

@Serializable
data class Fact(val a: Int, val op: Operation, val b: Int) {
    init {
        validate()
    }

    fun validate() {
        require(a in 1..999 && b in 1..999)
        if (op == Operation.SUBTRACT) require(a >= b)
        if (op == Operation.DIVIDE) require(a % b == 0)
    }

    val answer: Int
        get() =
            when (op) {
                Operation.ADD -> a + b
                Operation.SUBTRACT -> a - b
                Operation.MULTIPLY -> a * b
                Operation.DIVIDE -> a / b
            }

    val key
        get() = "$a:${op.name}:$b"

    val prompt
        get() = "$a ${op.symbol} $b"

    companion object {
        fun parse(key: String): Fact {
            val parts = key.split(':')
            require(parts.size == 3)
            return Fact(parts[0].toInt(), Operation.valueOf(parts[1]), parts[2].toInt())
        }
    }
}

@Serializable
data class Progress(
    val streak: Int = 0,
    val due: Long = 0,
    val intervalDays: Int = 0,
    val reviews: Int = 0,
    val correct: Int = 0,
) {
    val mastered
        get() = streak >= 3 && intervalDays >= 7

    fun validate() {
        require(
            streak >= 0 &&
                reviews >= streak &&
                correct in 0..reviews &&
                due >= 0 &&
                intervalDays in 0..365
        )
    }
}

@Serializable
enum class Rating(val label: String) {
    AGAIN("Ulangi"),
    HARD("Sulit"),
    GOOD("Ingat"),
    EASY("Mudah"),
}

object Scheduler {
    fun review(p: Progress, rating: Rating, now: Long): Progress {
        require(now >= 0)
        val success = rating != Rating.AGAIN
        val interval =
            when (rating) {
                Rating.AGAIN -> 0
                Rating.HARD -> max(1, (p.intervalDays * 1.2).roundToInt())
                Rating.GOOD ->
                    when (p.streak) {
                        0 -> 1
                        1 -> 3
                        else -> max(7, (p.intervalDays * 2.2).roundToInt())
                    }
                Rating.EASY -> max(4, (p.intervalDays * 3.0).roundToInt())
            }.coerceAtMost(365)
        return Progress(
            if (success) p.streak + 1 else 0,
            now + if (success) interval * 86_400_000L else 60_000L,
            interval,
            p.reviews + 1,
            p.correct + if (success) 1 else 0,
        )
    }
}

object Facts {
    fun range(digits: Int): IntRange =
        when (digits) {
            1 -> 1..9
            2 -> 10..99
            3 -> 100..999
            else -> throw IllegalArgumentException("Digit harus 1–3")
        }

    fun random(config: PracticeConfig, random: Random = Random.Default): Fact {
        config.validate()
        val bounds = range(config.digits)
        val mid = (bounds.first + bounds.last) / 2
        val left =
            when (config.group) {
                FactGroup.ALL -> bounds
                FactGroup.LOWER -> bounds.first..mid
                FactGroup.UPPER -> mid + 1..bounds.last
            }
        val op = config.operations.sortedBy { it.ordinal }.random(random)
        val a = left.random(random)
        val b =
            when (op) {
                Operation.SUBTRACT -> (bounds.first..a).random(random)
                Operation.DIVIDE -> bounds.filter { it <= a && a % it == 0 }.random(random)
                else -> bounds.random(random)
            }
        return Fact(a, op, b)
    }

    /**
     * A stable study selection, not the entire million-combination 3-digit space. Random mode
     * covers the full operand range.
     */
    fun catalog(config: PracticeConfig): List<Fact> {
        if (config.digits == 1)
            return config.operations
                .sortedBy { it.ordinal }
                .flatMap { op ->
                    (1..9).flatMap { a ->
                        (1..9).mapNotNull { b -> runCatching { Fact(a, op, b) }.getOrNull() }
                    }
                }
                .filter { config.matches(it) }
        return config.operations
            .sortedBy { it.ordinal }
            .flatMap { op ->
                val choices = linkedSetOf<Fact>()
                val rng = Random(37 + config.digits * 31 + op.ordinal * 7 + config.group.ordinal)
                val one = config.copy(operations = setOf(op))
                repeat(4096) { if (choices.size < 240) choices.add(random(one, rng)) }
                choices.toList().sortedWith(compareBy<Fact> { it.a }.thenBy { it.b })
            }
    }

    fun session(
        config: PracticeConfig,
        mode: StudyMode,
        progress: Map<String, Progress>,
        now: Long = System.currentTimeMillis(),
    ): StudySession {
        val due =
            progress
                .filter { it.value.due <= now }
                .keys
                .mapNotNull { runCatching { Fact.parse(it) }.getOrNull() }
                .filter { config.matches(it) }
                .sortedBy { progress[it.key]?.due }
        val cards =
            if (mode == StudyMode.ADAPTIVE)
                (due + catalog(config).filter { it.key !in progress }).distinct().take(10)
            else if (mode == StudyMode.RANDOM || mode == StudyMode.SPEED)
                List(if (mode == StudyMode.SPEED) 100 else 10) { random(config) }
            else catalog(config).shuffled().take(10)
        require(cards.isNotEmpty()) {
            "Semua kartu sudah terjadwal. Coba Soal acak atau tunggu jadwal berikutnya."
        }
        return StudySession(mode = mode, cards = cards)
    }
}
