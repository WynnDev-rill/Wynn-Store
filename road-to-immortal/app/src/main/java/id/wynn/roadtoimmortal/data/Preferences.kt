package id.wynn.roadtoimmortal.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import id.wynn.roadtoimmortal.domain.HeroPool
import id.wynn.roadtoimmortal.domain.RankPosition
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("immortal")

data class UserPreferences(
    val position: RankPosition = RankPosition(),
    val hasRank: Boolean = false,
    val theme: String = "system",
    val favorites: Set<Int> = emptySet(),
    val lastSeason: Int? = null,
    val pools: Map<String, List<Int>> = emptyMap(),
)

class Preferences(context: Context) {
    private val store = context.dataStore
    private val tier = stringPreferencesKey("rank_tier")
    private val division = intPreferencesKey("rank_division")
    private val stars = intPreferencesKey("rank_stars")
    private val selected = booleanPreferencesKey("has_rank")
    private val theme = stringPreferencesKey("theme")
    private val favorites = stringSetPreferencesKey("favorite_heroes")
    private val season = intPreferencesKey("rank_season")
    private val pools = stringPreferencesKey("hero_pools_v1")
    val flow =
        store.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { p ->
                UserPreferences(
                    RankPosition(p[tier] ?: "mythic", p[division] ?: 1, p[stars] ?: 0),
                    p[selected] ?: false,
                    p[theme] ?: "system",
                    p[favorites].orEmpty().mapNotNull(String::toIntOrNull).toSet(),
                    p[season],
                    HeroPool.decode(p[pools]),
                )
            }

    suspend fun setRank(value: RankPosition, number: Int?) {
        store.edit {
            it[tier] = value.tier
            it[division] = value.division
            it[stars] = value.stars
            it[selected] = true
            if (number != null) it[season] = number
        }
    }

    suspend fun setTheme(value: String) {
        require(value in listOf("system", "light", "dark"))
        store.edit { it[theme] = value }
    }

    suspend fun toggleFavorite(id: Int) {
        store.edit { p ->
            val old = p[favorites].orEmpty()
            p[favorites] = if (id.toString() in old) old - id.toString() else old + id.toString()
        }
    }

    suspend fun editPool(change: (Map<String, List<Int>>) -> Map<String, List<Int>>) {
        store.edit { p -> p[pools] = HeroPool.encode(change(HeroPool.decode(p[pools]))) }
    }
}
