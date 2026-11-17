package id.wynn.arithmemo

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import id.wynn.arithmemo.core.*
import id.wynn.arithmemo.ui.MemoViewModel
import java.time.Duration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TimerTest {
    @Test
    fun advancingFeedbackDoesNotGiveTimeBackToSpeedSession() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val app = ApplicationProvider.getApplicationContext<Application>()
            app.deleteDatabase("arithmemo.db")
            val vm = MemoViewModel(app)
            withTimeout(10000) { vm.loaded.first { it } }
            val fact = Fact(2, Operation.ADD, 3)
            vm.snapshot.value =
                Snapshot(
                    session =
                        StudySession(
                            mode = StudyMode.SPEED,
                            cards = listOf(fact, fact),
                            feedback = true,
                            answered = 1,
                            correct = 1,
                        )
                )
            vm.foreground(true)
            ShadowSystemClock.advanceBy(Duration.ofMillis(400))
            vm.next()
            withTimeout(10000) { vm.snapshot.first { it.session?.index == 1 } }
            vm.tick()
            assertTrue(
                "Elapsed time must not be discarded",
                vm.snapshot.value.session!!.remainingMs <= 59600,
            )
            vm.foreground(false)
            val paused = vm.snapshot.value.session!!.remainingMs
            ShadowSystemClock.advanceBy(Duration.ofSeconds(5))
            vm.foreground(true)
            vm.tick()
            assertEquals(paused, vm.snapshot.value.session!!.remainingMs)
            vm.foreground(false)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
