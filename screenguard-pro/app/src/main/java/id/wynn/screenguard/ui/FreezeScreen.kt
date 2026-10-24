package id.wynn.screenguard.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.wynn.screenguard.domain.*

@OptIn(ExperimentalFoundationApi::class,ExperimentalMaterial3Api::class)
@Composable fun FreezeScreen(state: GuardState, device: DeviceData, ready: Boolean, busy: Boolean,
    add: (InstalledApp)->Unit,select: (InstalledApp)->Unit,remove: (InstalledApp)->Unit,saveOptions: (Boolean,AccessGate)->Unit,
    permissions: ()->Unit = {},close: (InstalledApp)->Unit = {},freezeAll: ()->Unit = {},
    addMany: ((List<InstalledApp>)->Unit)? = null,editRule: ((InstalledApp,Int,TimeWindow?)->Unit)? = null,initialTarget: String? = null) {
    var picker by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf<InstalledApp?>(null) }
    var overflow by remember { mutableStateOf(false) }
    val selected = FreezeCatalog.selected(state,device.apps)
    val visible = selected.filter { it.label.contains(search,true) || it.packageName.contains(search,true) }
    val context = LocalContext.current
    val large = LocalDensity.current.fontScale > 1.3f
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            PageHeader("Dibekukan",null,action = {
                Box {
                    IconButton({overflow=true}) {Icon(Icons.Outlined.MoreVert,"Menu Bekukan")}
                    DropdownMenu(overflow,{overflow=false}) {
                        DropdownMenuItem(text={Text("Bekukan semua")},onClick={overflow=false;freezeAll()},enabled=ready && !busy && selected.any {it.enabled && !it.suspended})
                        DropdownMenuItem(text={Text("Shizuku")},onClick={overflow=false;permissions()})
                    }
                }
            })
            if(selected.isNotEmpty()) {
                if(!ready) TextButton(permissions,Modifier.padding(horizontal=16.dp)) {Icon(Icons.Outlined.Bolt,null);Spacer(Modifier.width(8.dp));Text("Hubungkan Shizuku")}
                OutlinedTextField(search,{search=it},Modifier.fillMaxWidth().padding(horizontal=20.dp),label={Text("Cari aplikasi")},singleLine=true,
                    leadingIcon={Icon(Icons.Outlined.Search,null)},shape=RoundedCornerShape(18.dp))
                LazyVerticalGrid(GridCells.Adaptive(if(large) 148.dp else 100.dp),Modifier.fillMaxSize(),
                    contentPadding=PaddingValues(16.dp,24.dp,16.dp,96.dp),verticalArrangement=Arrangement.spacedBy(24.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    items(visible,key={it.packageName}) { app ->
                        val frozen = !app.enabled || app.suspended
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).combinedClickable(enabled=!busy,onClick={select(app)},onLongClick={menu=app})
                            .semantics(mergeDescendants=true) { stateDescription=if(frozen) "Dibekukan" else "Aktif"; contentDescription="${app.label}, ${if(frozen) "dibekukan" else "aktif"}" },
                            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            AppIcon(app,60.dp,frozen)
                            Text(app.label,style=MaterialTheme.typography.labelLarge,maxLines=2,overflow=TextOverflow.Ellipsis,textAlign=TextAlign.Center)
                            state.rules[app.packageName]?.lastError?.let { Text("Perlu diperiksa",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error) }
                            IconButton({menu=app},Modifier.size(48.dp)) {Icon(Icons.Outlined.MoreHoriz,"Kelola ${app.label}")}
                        }
                    }
                }
            }
        }
        FloatingActionButton({picker=true},Modifier.align(Alignment.BottomEnd).padding(24.dp),containerColor=MaterialTheme.colorScheme.primary,
            contentColor=MaterialTheme.colorScheme.onPrimary,shape=RoundedCornerShape(18.dp)) {Icon(Icons.Outlined.Add,"Tambah aplikasi",Modifier.size(28.dp))}
    }
    if(picker) {
        var query by rememberSaveable {mutableStateOf("")}
        var chosen by rememberSaveable {mutableStateOf(listOf<String>())}
        val apps = device.apps.filter {FreezeCatalog.canAdd(it,state.rules[it.packageName]) && (it.label.contains(query,true)||it.packageName.contains(query,true))}
        ModalBottomSheet(onDismissRequest={picker=false},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
            Column(Modifier.fillMaxWidth().imePadding().padding(horizontal=20.dp).heightIn(max=640.dp)) {
                Text("Tambah aplikasi",style=MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                if(!ready) FilledTonalButton(permissions,Modifier.fillMaxWidth()) {Text("Hubungkan Shizuku")}
                OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),label={Text("Cari aplikasi")},singleLine=true,leadingIcon={Icon(Icons.Outlined.Search,null)},shape=RoundedCornerShape(18.dp))
                LazyColumn(Modifier.weight(1f,fill=false),contentPadding=PaddingValues(vertical=8.dp)) {
                    items(apps,key={it.packageName}) {app ->
                        Row(Modifier.fillMaxWidth().clickable(enabled=ready&&!busy) {
                            if(addMany==null) {add(app);picker=false} else chosen=if(app.packageName in chosen) chosen-app.packageName else chosen+app.packageName
                        }.padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            AppIcon(app,44.dp)
                            Text(app.label,Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge)
                            Checkbox(app.packageName in chosen,null)
                        }
                    }
                }
                if(addMany!=null) Button({addMany(device.apps.filter {it.packageName in chosen});picker=false},Modifier.fillMaxWidth().padding(vertical=16.dp),enabled=ready&&!busy&&chosen.isNotEmpty()) {Text("Bekukan (${chosen.size})")}
            }
        }
    }
    menu?.let {app ->
        val frozen=!app.enabled||app.suspended
        ModalBottomSheet(onDismissRequest={menu=null},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
            Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {AppIcon(app,48.dp,frozen);Text(app.label,style=MaterialTheme.typography.titleLarge)}
                state.rules[app.packageName]?.lastError?.let {Text(it,color=MaterialTheme.colorScheme.error)}
                Button({menu=null;if(frozen)select(app)else close(app)},Modifier.fillMaxWidth(),enabled=ready&&!busy) {Text(if(frozen) "Buka aplikasi" else "Bekukan sekarang")}
                TextButton({context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${app.packageName}")));menu=null},Modifier.fillMaxWidth()) {Text("Info aplikasi")}
                TextButton({menu=null;remove(app)},Modifier.fillMaxWidth(),enabled=ready&&!busy) {Text("Keluarkan dari daftar")}
            }
        }
    }
}

// Legacy rule editors are intentionally absent from the product's navigation.
fun gateLabel(gate: AccessGate): String = "Bekukan"
