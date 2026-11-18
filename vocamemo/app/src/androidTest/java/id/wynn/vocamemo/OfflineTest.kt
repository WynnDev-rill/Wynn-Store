package id.wynn.vocamemo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineTest {
    private val context
        get() = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun dictionarySearchSensesExamples() {
        val d = Dictionary(context)
        assertEquals(15725, d.count())
        assertEquals("apple", d.search(" APPLE ").first().word)
        assertTrue(d.search("%_\\\" OR 1=1").isEmpty())
        assertTrue(d.search("zzzxxyyunknown").isEmpty())
        assertTrue(d.search("").isEmpty())
        val book = d.senses("book")
        assertTrue(book.any { it.pos == "noun" && "buku" in it.translations })
        assertTrue(book.any { it.pos == "verb" && "pesan" in it.translations })
        assertTrue(d.senses("light").any { "ringan" in it.translations })
        assertTrue(d.senses("apple").any { it.ipa.isNotBlank() })
        val examples = d.examples("book")
        assertTrue(examples.isNotEmpty())
        assertTrue(examples.all { it.credit.contains("tatoeba.org") && it.indonesian.isNotBlank() })
    }

    @Test
    fun browsePagingAndSearchDirections() {
        val d = Dictionary(context)
        val first = d.browse(null, "")
        val second = d.browse(first.last().cursor, "")
        assertEquals(64, first.size)
        assertTrue(first.last().cursor < second.first().cursor)
        assertTrue(d.browse(null, "z").first().cursor.startsWith("z"))
        assertTrue(d.search("buku", "id").any { it.word == "book" && it.summary == "buku" })
        assertTrue(d.search("aple").any { it.word == "apple" && it.suggestion != null })
        assertTrue(d.search("bkuu", "id").any { it.word == "book" && it.suggestion != null })
        assertTrue(d.search("running").any { it.word == "run" && it.suggestion != null })
        val discovered = d.discover(first.mapTo(hashSetOf()) { it.key }, second.first().key)!!
        assertFalse(first.any { it.key == discovered.key })
        assertNotEquals(second.first().key, discovered.key)
    }

    @Test
    fun progressIsAtomicBackupIsValidated() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, VocaDatabase::class.java).build()
        try {
            val s = PersonalStore(db)
            val m = Memo(word = "apple", meaning = "apel")
            assertTrue(s.dao.insert(m) != -1L)
            assertEquals(-1L, s.dao.insert(m.copy(id = java.util.UUID.randomUUID().toString())))
            val attempt = java.util.UUID.randomUUID().toString()
            assertTrue(s.grade(m.id, 3, "Flashcard", "EN → ID", 2000, attempt))
            assertFalse(s.grade(m.id, 3, "Flashcard", "EN → ID", 2000, attempt))
            assertEquals(1, s.dao.allReviews().size)
            assertEquals(1, s.dao.memo(m.id)!!.repetitions)
            val other =
                m.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    context = "a different sense",
                    sourceSense = "eng/apple|different",
                    memoKey = memoKey(m.word, m.meaning, "a different sense", "eng/apple|different"),
                )
            assertTrue(s.dao.insert(other) != -1L)
            s.dao.delete(other.id)
            val backup = s.export(20)
            s.dao.delete(m.id)
            assertEquals(1, s.import(backup).added)
            assertEquals(1, s.dao.allMemos().size)
            assertEquals(1, s.dao.allReviews().size)
            assertEquals(0, s.import(backup).added)
            try {
                s.import(backup.replace("\"ease\": 2.5", "\"ease\": -100"))
                fail("Invalid scheduling data accepted")
            } catch (_: IllegalArgumentException) {}
            assertEquals(1, s.dao.allMemos().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun persistentRoomReopens() = runBlocking {
        val name = "persistence-${System.nanoTime()}.db"
        var db = Room.databaseBuilder(context, VocaDatabase::class.java, name).build()
        val m = Memo(word = "light", meaning = "ringan")
        try {
            db.dao().insert(m)
            db.close()
            db = Room.databaseBuilder(context, VocaDatabase::class.java, name).build()
            assertEquals("ringan", db.dao().memo(m.id)?.meaning)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun malformedUpdateManifestRejected() {
        val base =
            """{"versionCode":2,"versionName":"1.0.1","packageName":"id.wynn.vocamemo","minSdk":26,"apkUrl":"https://github.com/WynnDev-rill/Wynn-Store/releases/download/vocamemo-v1.0.1/VocaMemo.apk","sha256":"${"a".repeat(64)}","signerSha256":"${"b".repeat(64)}","sizeBytes":1000}"""
        Release.parse(base).validate()
        listOf(
                base.replace("id.wynn.vocamemo", "evil.app"),
                base.replace("1000", "0"),
                base.replace("https://github.com/", "https://evil.org/"),
                base.replace("\"minSdk\":26", "\"minSdk\":1"),
            )
            .forEach {
                try {
                    Release.parse(it)
                    fail("Invalid manifest accepted")
                } catch (_: IllegalArgumentException) {}
            }
    }
}
