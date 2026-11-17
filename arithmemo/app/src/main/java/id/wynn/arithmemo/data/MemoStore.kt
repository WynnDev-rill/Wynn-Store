package id.wynn.arithmemo.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import id.wynn.arithmemo.core.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** One transaction commits both the answer and the next recoverable session state. */
class MemoStore(context: Context) : SQLiteOpenHelper(context, "arithmemo.db", null, 1) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE state (name TEXT PRIMARY KEY, value TEXT NOT NULL)")
        db.execSQL("CREATE TABLE progress (fact TEXT PRIMARY KEY, value TEXT NOT NULL)")
        db.execSQL("CREATE TABLE attempts (id TEXT PRIMARY KEY, value TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Migrasi database belum tersedia; data lama dipertahankan")
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    private fun state(name: String): String? =
        readableDatabase.rawQuery("SELECT value FROM state WHERE name=?", arrayOf(name)).use {
            if (it.moveToFirst()) it.getString(0) else null
        }

    private fun put(db: SQLiteDatabase, name: String, value: String?) {
        if (value == null) db.delete("state", "name=?", arrayOf(name))
        else
            db.insertWithOnConflict(
                    "state",
                    null,
                    ContentValues().apply {
                        put("name", name)
                        put("value", value)
                    },
                    SQLiteDatabase.CONFLICT_REPLACE,
                )
                .also { check(it != -1L) }
    }

    @Synchronized
    fun read(): Snapshot {
        val progress = linkedMapOf<String, Progress>()
        readableDatabase.rawQuery("SELECT fact,value FROM progress", null).use { c ->
            while (c.moveToNext()) progress[c.getString(0)] = json.decodeFromString(c.getString(1))
        }
        val attempts = mutableListOf<Attempt>()
        readableDatabase.rawQuery("SELECT value FROM attempts ORDER BY rowid", null).use { c ->
            while (c.moveToNext()) attempts += json.decodeFromString<Attempt>(c.getString(0))
        }
        return Snapshot(
            state("settings")?.let { json.decodeFromString<UserSettings>(it) } ?: UserSettings(),
            progress,
            attempts,
            state("session")?.let { json.decodeFromString<StudySession>(it) },
        )
    }

    @Synchronized
    fun saveSettings(value: UserSettings) {
        value.validate()
        put(writableDatabase, "settings", json.encodeToString(value))
    }

    @Synchronized
    fun saveSession(value: StudySession?) {
        value?.validate()
        put(writableDatabase, "session", value?.let { json.encodeToString(it) })
    }

    @Synchronized
    fun record(attempt: Attempt, next: StudySession) {
        attempt.validate()
        next.validate()
        val db = writableDatabase
        db.beginTransaction()
        try {
            val inserted =
                db.insertWithOnConflict(
                    "attempts",
                    null,
                    ContentValues().apply {
                        put("id", attempt.id)
                        put("value", json.encodeToString(attempt))
                    },
                    SQLiteDatabase.CONFLICT_IGNORE,
                )
            if (inserted != -1L) {
                val old =
                    db.rawQuery(
                            "SELECT value FROM progress WHERE fact=?",
                            arrayOf(attempt.fact.key),
                        )
                        .use {
                            if (it.moveToFirst()) json.decodeFromString<Progress>(it.getString(0))
                            else Progress()
                        }
                val reviewed = Scheduler.review(old, attempt.rating, attempt.at)
                check(
                    db.insertWithOnConflict(
                        "progress",
                        null,
                        ContentValues().apply {
                            put("fact", attempt.fact.key)
                            put("value", json.encodeToString(reviewed))
                        },
                        SQLiteDatabase.CONFLICT_REPLACE,
                    ) != -1L
                )
                put(db, "session", json.encodeToString(next))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun export(): String {
        val s = read()
        return json.encodeToString(
            Backup(settings = s.settings, progress = s.progress, attempts = s.attempts)
        )
    }

    @Synchronized
    fun import(text: String) {
        require(text.toByteArray().size <= MAX_BACKUP) { "Cadangan melebihi 32 MB" }
        val data =
            try {
                json.decodeFromString<Backup>(text).also {
                    require(
                        json
                            .parseToJsonElement(text)
                            .jsonObject["schema"]
                            ?.jsonPrimitive
                            ?.intOrNull == 1
                    )
                    it.validate()
                }
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    "Cadangan tidak valid atau versinya tidak didukung",
                    e,
                )
            }
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("progress", null, null)
            db.delete("attempts", null, null)
            db.delete("state", null, null)
            put(db, "settings", json.encodeToString(data.settings))
            data.progress.forEach { (key, p) ->
                check(
                    db.insertOrThrow(
                        "progress",
                        null,
                        ContentValues().apply {
                            put("fact", key)
                            put("value", json.encodeToString(p))
                        },
                    ) != -1L
                )
            }
            data.attempts.forEach { a ->
                check(
                    db.insertOrThrow(
                        "attempts",
                        null,
                        ContentValues().apply {
                            put("id", a.id)
                            put("value", json.encodeToString(a))
                        },
                    ) != -1L
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        const val MAX_BACKUP = 32 * 1024 * 1024

        fun readBackup(input: java.io.InputStream): String {
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= MAX_BACKUP) { "Cadangan melebihi 32 MB" }
                output.write(buffer, 0, count)
            }
            return output.toString("UTF-8")
        }
    }
}
