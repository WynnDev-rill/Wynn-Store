package id.wynn.vocamemo

import android.content.Context
import androidx.room.*
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "memos", indices = [Index(value = ["memoKey"], unique = true)])
data class Memo(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val word: String,
    val meaning: String,
    val context: String = "",
    val pos: String = "",
    val ipa: String = "",
    val groupName: String = "Umum",
    val favourite: Boolean = false,
    val mastered: Boolean = false,
    val note: String = "",
    val due: Long = 0,
    val interval: Int = 0,
    val ease: Double = 2.5,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val created: Long = System.currentTimeMillis(),
    val sourceSense: String = "",
    val memoKey: String = memoKey(word, meaning, context, sourceSense),
)

@Entity(tableName = "reviews", indices = [Index("time")])
data class Review(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val memoId: String,
    val word: String,
    val time: Long,
    val rating: Int,
    val correct: Boolean,
    val mode: String,
    val direction: String,
    val durationMs: Long,
)

@Dao
interface VocaDao {
    @Query("SELECT * FROM memos ORDER BY created DESC") fun observeMemos(): Flow<List<Memo>>

    @Query("SELECT * FROM reviews ORDER BY time DESC") fun observeReviews(): Flow<List<Review>>

    @Query("SELECT * FROM memos ORDER BY created DESC") suspend fun allMemos(): List<Memo>

    @Query("SELECT * FROM reviews ORDER BY time DESC") suspend fun allReviews(): List<Review>

    @Query("SELECT * FROM memos WHERE id=:id") suspend fun memo(id: String): Memo?

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(m: Memo): Long

    @Update suspend fun update(m: Memo)

    @Query("DELETE FROM memos WHERE id=:id") suspend fun delete(id: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertReview(r: Review): Long
}

@Database(entities = [Memo::class, Review::class], version = 1, exportSchema = true)
abstract class VocaDatabase : RoomDatabase() {
    abstract fun dao(): VocaDao

    companion object {
        fun open(c: Context) =
            Room.databaseBuilder(c, VocaDatabase::class.java, "vocamemo-personal.db").build()
    }
}

class PersonalStore(val db: VocaDatabase) {
    val dao = db.dao()

    suspend fun grade(
        memoId: String,
        rating: Int,
        mode: String,
        direction: String,
        duration: Long,
        attemptId: String,
    ): Boolean =
        db.withTransaction {
            val current = dao.memo(memoId) ?: return@withTransaction false
            val now = System.currentTimeMillis()
            val added =
                dao.insertReview(
                    Review(
                        attemptId,
                        current.id,
                        current.word,
                        now,
                        rating,
                        rating >= 2,
                        mode,
                        direction,
                        duration.coerceAtLeast(0),
                    )
                )
            if (added == -1L) return@withTransaction false
            dao.update(Scheduler.grade(current, rating, now))
            true
        }

    suspend fun export(goal: Int): String =
        db.withTransaction {
            JSONObject()
                .put("format", "VocaMemo")
                .put("version", 1)
                .put("goal", goal)
                .put("memos", JSONArray().apply { dao.allMemos().forEach { put(it.json()) } })
                .put(
                    "reviews",
                    JSONArray().apply {
                        dao.allReviews().forEach { r ->
                            put(
                                JSONObject()
                                    .put("id", r.id)
                                    .put("memoId", r.memoId)
                                    .put("word", r.word)
                                    .put("time", r.time)
                                    .put("rating", r.rating)
                                    .put("correct", r.correct)
                                    .put("mode", r.mode)
                                    .put("direction", r.direction)
                                    .put("durationMs", r.durationMs)
                            )
                        }
                    },
                )
                .toString(2)
                .also { backup ->
                    require(backup.toByteArray().size <= BACKUP_LIMIT) {
                        "Cadangan melebihi batas 50 MB. Hubungi pengembang untuk ekspor arsip besar."
                    }
                    require(dao.allMemos().size <= 50000 && dao.allReviews().size <= 100000) {
                        "Cadangan melebihi batas jumlah catatan yang didukung."
                    }
                }
        }

    /** Validate the entire backup before any mutation. Merge keeps existing records. */
    suspend fun import(text: String): ImportResult {
        require(text.toByteArray().size <= BACKUP_LIMIT) {
            "Cadangan terlalu besar (maksimal 50 MB)"
        }
        val root = JSONObject(text)
        require(root.getString("format") == "VocaMemo" && root.getInt("version") == 1) {
            "Format cadangan tidak didukung"
        }
        require(root.getInt("goal") in 1..500) { "Target harian cadangan tidak valid" }
        val memos = root.getJSONArray("memos")
        val reviews = root.getJSONArray("reviews")
        require(memos.length() <= 50000 && reviews.length() <= 100000) { "Terlalu banyak catatan" }
        val parsed = (0 until memos.length()).map { memoFrom(memos.getJSONObject(it)) }
        val events =
            (0 until reviews.length()).map { i ->
                val j = reviews.getJSONObject(i)
                val rating = j.getInt("rating")
                require(rating in 1..4)
                val id = j.getString("id")
                UUID.fromString(id)
                val memoId = j.getString("memoId")
                UUID.fromString(memoId)
                val time = j.getLong("time")
                val duration = j.getLong("durationMs")
                require(time >= 0 && duration in 0..86_400_000L)
                Review(
                    id,
                    memoId,
                    j.getString("word").take(300),
                    time,
                    rating,
                    rating >= 2,
                    j.getString("mode").take(80),
                    j.getString("direction").take(80),
                    duration,
                )
            }
        val added =
            db.withTransaction {
                val existing = dao.allMemos().associateBy { it.memoKey }.toMutableMap()
                val idMap = mutableMapOf<String, String>()
                var added = 0
                parsed.forEach { m ->
                    val old = existing[m.memoKey]
                    if (old != null) idMap[m.id] = old.id
                    else {
                        val actual =
                            if (dao.memo(m.id) != null) m.copy(id = UUID.randomUUID().toString())
                            else m
                        if (dao.insert(actual) != -1L) {
                            added++
                            existing[actual.memoKey] = actual
                        }
                        idMap[m.id] = actual.id
                    }
                }
                events.forEach { r ->
                    dao.insertReview(r.copy(memoId = idMap[r.memoId] ?: r.memoId))
                }
                added
            }
        return ImportResult(added, root.getInt("goal"))
    }
}

data class ImportResult(val added: Int, val goal: Int)

private fun Memo.json() =
    JSONObject()
        .put("id", id)
        .put("word", word)
        .put("meaning", meaning)
        .put("context", context)
        .put("pos", pos)
        .put("ipa", ipa)
        .put("groupName", groupName)
        .put("favourite", favourite)
        .put("mastered", mastered)
        .put("note", note)
        .put("due", due)
        .put("interval", interval)
        .put("ease", ease)
        .put("repetitions", repetitions)
        .put("lapses", lapses)
        .put("created", created)
        .put("sourceSense", sourceSense)

private fun memoFrom(j: JSONObject): Memo {
    val id = j.getString("id")
    UUID.fromString(id)
    val w = j.getString("word").trim()
    val m = j.getString("meaning").trim()
    val group = j.optString("groupName", "Umum").trim()
    require(w.length in 1..300 && m.length in 1..1000 && group.length in 1..80) {
        "Isi memo tidak valid"
    }
    val due = j.getLong("due")
    val interval = j.getInt("interval")
    val ease = j.getDouble("ease")
    val reps = j.getInt("repetitions")
    val lapses = j.getInt("lapses")
    val created = j.getLong("created")
    require(
        due >= 0 &&
            interval in 0..3650 &&
            ease.isFinite() &&
            ease in 1.3..3.0 &&
            reps in 0..1000000 &&
            lapses in 0..1000000 &&
            created >= 0
    ) {
        "Progres cadangan tidak valid"
    }
    return Memo(
        id,
        w,
        m,
        j.optString("context").take(3000),
        j.optString("pos").take(80),
        j.optString("ipa").take(3000),
        group,
        j.getBoolean("favourite"),
        j.getBoolean("mastered"),
        j.optString("note").take(5000),
        due,
        interval,
        ease,
        reps,
        lapses,
        created,
        j.optString("sourceSense").take(4000),
    )
}

internal fun memoKey(word: String, meaning: String, context: String, sourceSense: String) =
    Answers.normalize(word) +
        "\n" +
        Answers.normalize(meaning) +
        "\n" +
        sourceSense.ifBlank { context }

internal const val BACKUP_LIMIT = 50 * 1024 * 1024
