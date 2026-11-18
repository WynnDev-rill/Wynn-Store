package id.wynn.vocamemo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight

internal data class WordPhoto(val drawable: Int, val label: String, val context: String)

internal val wordPhotos = mapOf(
    "apple" to WordPhoto(R.drawable.apple, "Apel merah", "fruit of Malus domestica"),
    "banana" to WordPhoto(R.drawable.banana, "Beberapa jenis pisang", "fruit"),
    "butterfly" to WordPhoto(R.drawable.butterfly, "Kupu-kupu", "insect"),
    "cat" to WordPhoto(R.drawable.cat, "Kucing peliharaan", "domestic species"),
    "mountain" to WordPhoto(R.drawable.mountain, "Gunung Everest", ""),
    "penguin" to WordPhoto(R.drawable.penguin, "Penguin kaisar", "flightless sea bird"),
    "sunflower" to WordPhoto(R.drawable.sunflower, "Bunga matahari", "flower"),
    "tiger" to WordPhoto(R.drawable.tiger, "Harimau", "The mammal Panthera tigris"),
    "waterfall" to WordPhoto(R.drawable.waterfall, "Air terjun", ""),
)

@Composable
internal fun WordTile(word: String, photo: WordPhoto?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val tones = listOf(
        colors.primaryContainer to colors.onPrimaryContainer,
        colors.tertiaryContainer to colors.onTertiaryContainer,
        colors.secondaryContainer to colors.onSecondaryContainer,
    )
    val (background, ink) = tones[Math.floorMod(word.hashCode(), tones.size)]
    Surface(modifier, shape = MaterialTheme.shapes.small, color = background, contentColor = ink) {
        if (photo != null) {
            Image(painterResource(photo.drawable), null, Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop)
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(word.firstOrNull()?.uppercase().orEmpty(),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
