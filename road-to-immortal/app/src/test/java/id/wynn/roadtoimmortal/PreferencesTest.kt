package id.wynn.roadtoimmortal

import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import id.wynn.roadtoimmortal.data.Preferences
import id.wynn.roadtoimmortal.domain.HeroPool
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class PreferencesTest {
    @Test
    fun legacyRankThemeFavoritesSurvivePoolWrites() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val job = SupervisorJob()
        val legacy =
            PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) {
                context.preferencesDataStoreFile("immortal")
            }
        legacy.edit {
            it[stringPreferencesKey("rank_tier")] = "mythic"
            it[intPreferencesKey("rank_stars")] = 37
            it[booleanPreferencesKey("has_rank")] = true
            it[stringPreferencesKey("theme")] = "dark"
            it[stringSetPreferencesKey("favorite_heroes")] = setOf("1", "2")
        }
        job.cancelAndJoin()
        val prefs = Preferences(context)
        assertTrue(prefs.flow.first().pools.isEmpty())
        coroutineScope {
            (1..15)
                .map { id -> launch { prefs.editPool { HeroPool.add(it, "EXP", id) } } }
                .joinAll()
        }
        val now = prefs.flow.first()
        assertEquals(10, now.pools["EXP"]!!.size)
        assertEquals(37, now.position.stars)
        assertTrue(now.hasRank)
        assertEquals("dark", now.theme)
        assertEquals(setOf(1, 2), now.favorites)
        assertEquals(now, Preferences(context).flow.first())
    }
}
