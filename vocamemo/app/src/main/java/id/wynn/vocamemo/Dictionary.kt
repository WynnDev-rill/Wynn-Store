package id.wynn.vocamemo

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.util.Locale

data class WordResult(val word: String, val summary: String, val senses: Int)

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
        val target = File(context.filesDir, "dictionary-v1.db")
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

    fun search(query: String): List<WordResult> {
        val q = query.trim().lowercase(Locale.ROOT).take(200)
        if (q.isEmpty()) return emptyList()
        val escaped = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return database()
            .rawQuery(
                "SELECT word,translations,count(*) FROM sense WHERE search LIKE ? ESCAPE '\\' GROUP BY search ORDER BY CASE WHEN search=? THEN 0 ELSE 1 END,length(search),search LIMIT 80",
                arrayOf("$escaped%", q),
            )
            .use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        WordResult(c.getString(0), c.getString(1), c.getInt(2))
                    )
                }
            }
    }

    fun senses(word: String): List<Sense> =
        database()
            .rawQuery(
                "SELECT id,word,pos,context,translations,ipa,lexentry FROM sense WHERE search=? ORDER BY id",
                arrayOf(word.lowercase(Locale.ROOT)),
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
