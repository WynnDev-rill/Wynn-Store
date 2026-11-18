package id.wynn.vocamemo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

internal data class GoogleProfile(val name: String, val email: String)

@Composable
internal fun WelcomeScreen(
    firstRun: Boolean,
    onProfile: (GoogleProfile) -> Unit,
    onGuest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val colors = MaterialTheme.colorScheme
    val configured = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotEmpty()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(colors.primaryContainer, colors.secondaryContainer,
                    colors.tertiaryContainer)),
                MaterialTheme.shapes.extraLarge,
            ).padding(vertical = 26.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.app_icon), null, Modifier.size(108.dp))
        }
        Text(
            if (firstRun) "Kenalan dengan kata baru." else "Masuk ke VocaMemo",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            if (firstRun) "Jelajahi kamus, simpan kata, lalu latih ingatan Anda."
            else "Gunakan akun Google untuk menampilkan identitas Anda di profil.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(
            onClick = {
                if (!configured) {
                    message = "Login Google belum aktif di build ini. Lanjutkan sebagai tamu."
                } else {
                    scope.launch {
                        working = true
                        message = null
                        try {
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(
                                    GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                                        .build()
                                ).build()
                            val credential = CredentialManager.create(context)
                                .getCredential(context, request).credential
                            if (credential !is CustomCredential ||
                                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                            ) {
                                message = "Google tidak mengembalikan akun yang didukung."
                            } else {
                                val google = GoogleIdTokenCredential.createFrom(credential.data)
                                onProfile(GoogleProfile(google.displayName ?: google.id, google.id))
                            }
                        } catch (_: GetCredentialCancellationException) {
                            // The account picker can be closed while staying on this screen.
                        } catch (_: NoCredentialException) {
                            message = "Tidak ada akun Google yang tersedia di perangkat ini."
                        } catch (_: GetCredentialException) {
                            message = "Gagal masuk. Periksa koneksi dan akun Google, lalu coba lagi."
                        } catch (_: GoogleIdTokenParsingException) {
                            message = "Profil Google tidak dapat dibaca. Coba lagi."
                        } finally {
                            working = false
                        }
                    }
                }
            },
            enabled = !working,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Image(painterResource(R.drawable.google_g), null, Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("Masuk dengan Google", fontWeight = FontWeight.Bold)
        }
        if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
        message?.let { Text(it, color = colors.onSurfaceVariant, textAlign = TextAlign.Center) }
        TextButton(onClick = onGuest, enabled = !working) {
            Text(if (firstRun) "Lanjut sebagai tamu" else "Kembali ke Profil")
        }
        Text(
            "Memo dan progres tetap tersimpan di perangkat ini.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

internal suspend fun clearGoogleAccount(context: android.content.Context) {
    try {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    } catch (_: androidx.credentials.exceptions.ClearCredentialException) {
        // Local profile information is already removed. The provider might be unavailable offline.
    }
}
