package id.wynn.arithmemo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import id.wynn.arithmemo.core.*
import id.wynn.arithmemo.data.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class StoreTest {
    @Test
    fun answersAndSessionSurviveCloseAndDuplicateCommit() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("arithmemo.db")
        val store = MemoStore(context)
        val f = Fact(7, Operation.MULTIPLY, 8)
        val session = StudySession(mode = StudyMode.RECALL, cards = listOf(f))
        store.saveSession(session)
        val attempt = Attempt("once", f, true, 2100, Rating.GOOD, 1700000000000)
        store.record(attempt, session.copy(index = 1))
        store.record(attempt, session.copy(index = 1))
        store.close()
        val reopened = MemoStore(context)
        val snapshot = reopened.read()
        assertEquals(1, snapshot.attempts.size)
        assertEquals(1, snapshot.progress[f.key]?.streak)
        assertEquals(1, snapshot.session?.index)
        assertEquals(1, Stats.from(snapshot.attempts).total)
        reopened.close()
    }

    @Test
    fun invalidImportCannotOverwriteExistingData() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("arithmemo.db")
        val store = MemoStore(context)
        store.saveSettings(UserSettings(goal = 40))
        val exported = store.export()
        assertThrows(IllegalArgumentException::class.java) { store.import("{}") }
        assertEquals(40, store.read().settings.goal)
        assertThrows(IllegalArgumentException::class.java) {
            store.import("{\"metadata\":{\"schema\":1}}")
        }
        assertEquals(40, store.read().settings.goal)
        store.import(exported)
        assertEquals(40, store.read().settings.goal)
        store.close()
    }
}
