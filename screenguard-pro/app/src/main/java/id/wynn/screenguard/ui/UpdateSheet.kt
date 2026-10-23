package id.wynn.screenguard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.wynn.screenguard.update.AppUpdater

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun UpdateSheet(state: AppUpdater.State, dismiss: () -> Unit, download: () -> Unit, install: () -> Unit, cancel: () -> Unit) {
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Outlined.SystemUpdate, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Ruang fokus yang lebih baik.", style = MaterialTheme.typography.headlineSmall)
            Text("ScreenGuard Pro ${state.available?.versionName.orEmpty()}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            if (state.available?.notes?.isNotBlank() == true) Text(state.available.notes, style = MaterialTheme.typography.bodyMedium)
            Text(state.message, style = MaterialTheme.typography.bodyMedium, color = if (state.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            AnimatedVisibility(state.progress != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(progress = { state.progress ?: 0f }, modifier = Modifier.fillMaxWidth())
                    Text("${((state.progress ?: 0f) * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                }
            }
            Button(if (state.ready) install else download, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = state.progress == null) {
                Text(if (state.ready) "Pasang update" else "Unduh update · ${state.available?.sizeBytes?.div(1024 * 1024f)?.let { "%.1f".format(it) } ?: "—"} MB")
            }
            if (state.progress != null) OutlinedButton(cancel, Modifier.fillMaxWidth()) { Text("Batalkan unduhan") }
            TextButton(dismiss, Modifier.fillMaxWidth()) { Text("Nanti saja") }
            Text("Hanya APK rilis resmi dengan checksum dan tanda tangan yang sesuai yang dapat dipasang. Android akan meminta konfirmasi pemasangan.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
