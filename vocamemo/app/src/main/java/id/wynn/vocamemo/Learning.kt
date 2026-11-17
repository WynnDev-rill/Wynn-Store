package id.wynn.vocamemo

import java.text.Normalizer
import java.util.Locale
import kotlin.math.roundToInt

object Answers {
    fun normalize(s: String) =
        Normalizer.normalize(s, Normalizer.Form.NFC)
            .trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")

    fun matches(input: String, answer: String) =
        normalize(input).isNotEmpty() && normalize(input) == normalize(answer)
}

/** SM-2 inspired, explicit self-ratings 1=Again, 2=Hard, 3=Good, 4=Easy. */
object Scheduler {
    fun grade(m: Memo, rating: Int, now: Long): Memo {
        require(rating in 1..4)
        val success = rating >= 2
        val ease =
            (m.ease +
                    when (rating) {
                        1 -> -0.2
                        2 -> -0.15
                        4 -> 0.15
                        else -> 0.0
                    })
                .coerceIn(1.3, 3.0)
        val interval =
            if (!success) 0
            else
                when {
                    m.repetitions == 0 -> 1
                    m.repetitions == 1 -> if (rating == 2) 3 else 6
                    else ->
                        (m.interval *
                                ease *
                                (if (rating == 2) 0.65 else if (rating == 4) 1.3 else 1.0))
                            .roundToInt()
                            .coerceAtLeast(m.interval + 1)
                }.coerceAtMost(3650)
        return m.copy(
            ease = ease,
            interval = interval,
            repetitions = if (success) m.repetitions + 1 else 0,
            due = now + if (success) interval * 86_400_000L else 60_000L,
            lapses = m.lapses + if (success) 0 else 1,
        )
    }
}
