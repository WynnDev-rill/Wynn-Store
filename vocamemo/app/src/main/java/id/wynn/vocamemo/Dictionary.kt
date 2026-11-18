package id.wynn.vocamemo

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.text.Normalizer
import java.util.Locale

data class WordResult(
    val word: String,
    val summary: String,
    val senses: Int,
    val key: String,
    val cursor: String,
    val suggestion: String? = null,
)

data class Sense(
    val id: Long,
    val word: String,
    val pos: String,
    val context: String,
    val translations: List<String>,
    val ipa: String,
    val source: String,
)

data class Example(val english: String, val indonesian: String, val credit: String)

class Dictionary(private val context: Context) {
    private var handle: SQLiteDatabase? = null

    @Synchronized
    private fun database(): SQLiteDatabase {
        handle?.let {
            return it
        }
        val target = File(context.filesDir, "dictionary-v2.db")
        if (!target.exists()) {
            val temp = File(context.filesDir, "dictionary-copy.tmp")
            context.assets.open("dictionary.db").use { input ->
                temp.outputStream().use { input.copyTo(it) }
            }
            check(temp.renameTo(target)) { "Gagal menyiapkan kamus. Coba lagi." }
        }
        return SQLiteDatabase.openDatabase(target.path, null, SQLiteDatabase.OPEN_READONLY).also {
            handle = it
        }
    }

    fun browse(after: String?, start: String): List<WordResult> =
        words(
            "SELECT word,summary,senses,search,sort_key,NULL FROM headword WHERE sort_key " +
                (if (after == null) ">= ?" else "> ?") +
                " ORDER BY sort_key LIMIT $PAGE_SIZE",
            arrayOf(after ?: start),
        )

    fun illustrated(): List<WordResult> {
        val keys = wordPhotos.keys.toTypedArray()
        return words(
            "SELECT word,summary,senses,search,sort_key,NULL FROM headword WHERE search IN (" +
                keys.joinToString(",") { "?" } + ") ORDER BY sort_key",
            keys,
        )
    }

    fun random(): List<WordResult> =
        words(
            "SELECT word,summary,senses,search,sort_key,NULL FROM headword ORDER BY random() LIMIT $PAGE_SIZE",
            emptyArray(),
        )

    fun discover(excluded: Set<String>, previous: String?): WordResult? {
        val sample = random()
        sample
            .firstOrNull { it.key !in excluded && it.key != previous }
            ?.let {
                return it
            }
        return database()
            .rawQuery(
                "SELECT word,summary,senses,search,sort_key FROM headword ORDER BY random()",
                null,
            )
            .use { cursor ->
                var fallback: WordResult? = null
                while (cursor.moveToNext()) {
                    val key = cursor.getString(3)
                    if (key in excluded) continue
                    val word =
                        WordResult(
                            cursor.getString(0),
                            cursor.getString(1),
                            cursor.getInt(2),
                            key,
                            cursor.getString(4),
                        )
                    if (key != previous) return@use word
                    fallback = word
                }
                fallback
            }
    }

    fun search(query: String, language: String = "en"): List<WordResult> {
        val q = dictionaryKey(query).take(200)
        if (q.isBlank()) return emptyList()
        val exact = lookup(language, q, false)
        val prefix = lookup(language, q, true)
        val roots =
            if (language == "id" && exact.isEmpty())
                indonesianRoots(q).flatMap { root ->
                    lookup(language, root, false).map {
                        it.copy(suggestion = "Kemungkinan bentuk dasar: $root")
                    }
                }
            else emptyList()
        val direct = (exact + roots + prefix).distinctBy { it.key }.take(80)
        if (exact.isNotEmpty() || roots.isNotEmpty() || q.length !in 3..40) return direct

        val distanceLimit = if (q.length < 6) 1 else 2
        val suggestions =
            database()
                .rawQuery(
                    "SELECT term,max(relevance) FROM lookup WHERE language=? AND inflected=0 AND length BETWEEN ? AND ? GROUP BY term",
                    arrayOf(
                        language,
                        (q.length - distanceLimit).toString(),
                        (q.length + distanceLimit).toString(),
                    ),
                )
                .use { cursor ->
                    buildList {
                        val distance = TypoDistance(q)
                        while (cursor.moveToNext()) {
                            val term = cursor.getString(0)
                            val edits = distance.to(term)
                            if (edits <= distanceLimit)
                                add(Triple(term, edits, cursor.getDouble(1)))
                        }
                    }
                }
                .sortedWith(compareBy({ it.second }, { -it.third }, { it.first }))
                .take(5)
                .flatMap { (term, _) ->
                    lookup(language, term, false).take(6).map {
                        it.copy(suggestion = "Saran ejaan: $term")
                    }
                }
        // Keep completion matches visible while making likely spelling corrections discoverable.
        return (direct.take(3) + suggestions + direct.drop(3)).distinctBy { it.key }.take(80)
    }

    private fun lookup(language: String, term: String, prefix: Boolean): List<WordResult> {
        val condition = if (prefix) "l.term > ? AND l.term < ? AND l.inflected=0" else "l.term=?"
        val args =
            if (prefix) arrayOf(language, term, term + "\uDBFF\uDFFF") else arrayOf(language, term)
        return words(
                """SELECT h.word, CASE WHEN l.gloss='' THEN h.summary ELSE l.gloss END,
                h.senses,h.search,h.sort_key,
                CASE WHEN l.inflected=1 THEN 'Bentuk dasar: ' ||
                    CASE WHEN l.gloss='' THEN h.word ELSE l.gloss END ELSE NULL END
                FROM lookup l JOIN headword h ON h.search=l.headword
                WHERE l.language=? AND $condition
                ORDER BY l.inflected,length(l.term),l.relevance DESC,l.term,h.sort_key,l.gloss LIMIT 240""",
                args,
            )
            .distinctBy { it.key }
            .take(80)
    }

    private fun words(sql: String, args: Array<String>): List<WordResult> =
        database().rawQuery(sql, args).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(
                    WordResult(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getInt(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5),
                    )
                )
            }
        }

    fun senses(word: String): List<Sense> =
        database()
            .rawQuery(
                "SELECT id,word,pos,context,translations,ipa,lexentry FROM sense WHERE search=? ORDER BY id",
                arrayOf(dictionaryKey(word)),
            )
            .use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        Sense(
                            c.getLong(0),
                            c.getString(1),
                            c.getString(2),
                            c.getString(3),
                            c.getString(4).split(" | "),
                            c.getString(5),
                            c.getString(6),
                        )
                    )
                }
            }

    fun examples(word: String): List<Example> {
        // Only token/phrase matches. These illustrate the word, never claim sense alignment.
        val token = word.replace("\"", "").trim()
        if (token.isEmpty()) return emptyList()
        return database()
            .rawQuery(
                "SELECT e.english,e.indonesian,e.credit FROM example_fts f JOIN example e ON e.id=f.docid WHERE example_fts MATCH ? ORDER BY length(e.english) LIMIT 3",
                arrayOf("\"$token\""),
            )
            .use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        Example(c.getString(0), c.getString(1), c.getString(2))
                    )
                }
            }
    }

    fun count(): Int =
        database().rawQuery("SELECT value FROM metadata WHERE key='words'", null).use {
            it.moveToFirst()
            it.getString(0).toInt()
        }
}

internal const val PAGE_SIZE = 64

internal fun dictionaryKey(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFC)
        .replace('’', '\'')
        .lowercase(Locale.ROOT)
        .trim()
        .replace(Regex("\\s+"), " ")

/** Restricted Damerau–Levenshtein, including adjacent transpositions. Reuses rows per query. */
internal class TypoDistance(private val query: String) {
    private var previous = IntArray(query.length + 1)
    private var beforePrevious = IntArray(query.length + 1)
    private var current = IntArray(query.length + 1)

    fun to(term: String): Int {
        for (i in previous.indices) previous[i] = i
        for (i in 1..term.length) {
            current[0] = i
            for (j in 1..query.length) {
                val cost = if (term[i - 1] == query[j - 1]) 0 else 1
                var edit = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
                if (i > 1 && j > 1 && term[i - 1] == query[j - 2] && term[i - 2] == query[j - 1]) {
                    edit = minOf(edit, beforePrevious[j - 2] + 1)
                }
                current[j] = edit
            }
            val reusable = beforePrevious
            beforePrevious = previous
            previous = current
            current = reusable
        }
        return previous[query.length]
    }
}

// Indonesian affixes are ambiguous. These candidates are shown as suggestions only
// when the resulting base already occurs as a source translation.
private fun indonesianRoots(word: String): Set<String> {
    if (word.length < 6 || word.any { it !in 'a'..'z' }) return emptySet()
    val prefixes =
        mapOf(
            "meny" to "s",
            "meng" to "k",
            "mem" to "p",
            "men" to "t",
            "peny" to "s",
            "peng" to "k",
            "pem" to "p",
            "pen" to "t",
            "ber" to "",
            "ter" to "",
            "di" to "",
            "ke" to "",
            "se" to "",
            "me" to "",
        )
    return buildSet {
        for (suffix in listOf("", "kan", "an", "nya", "i")) {
            if (!word.endsWith(suffix)) continue
            val base = word.dropLast(suffix.length)
            if (suffix.isNotEmpty() && base.length >= 4) add(base)
            for ((prefix, restored) in prefixes) {
                if (!base.startsWith(prefix)) continue
                val rest = base.drop(prefix.length)
                if (rest.length >= 4) add(rest)
                if (restored.isNotEmpty() && rest.length >= 3) add(restored + rest)
            }
        }
        remove(word)
    }
}
