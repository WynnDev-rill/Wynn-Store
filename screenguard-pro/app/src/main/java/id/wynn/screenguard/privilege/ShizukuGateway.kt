package id.wynn.screenguard.privilege

import android.content.*
import android.content.pm.PackageManager
import android.os.IBinder
import id.wynn.screenguard.BuildConfig
import id.wynn.screenguard.domain.PrivilegedBackend
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import rikka.shizuku.Shizuku

enum class ShizukuStatus(val label: String) { STOPPED("Shizuku belum aktif"), DENIED("Izin Shizuku diperlukan"), CONNECTING("Menghubungkan Shizuku"), READY("Terhubung"), ERROR("Koneksi bermasalah") }

class ShizukuGateway(context: Context, private val scope: CoroutineScope) : PrivilegedBackend {
    private val mutableStatus = MutableStateFlow(ShizukuStatus.STOPPED)
    val status = mutableStatus.asStateFlow()
    val firewallSupported = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    @Volatile private var service: IGuardService? = null
    private val mutex = Mutex()
    private var pending = CompletableDeferred<IGuardService>()
    private val args = Shizuku.UserServiceArgs(ComponentName(context, GuardUserService::class.java))
        .daemon(false).processNameSuffix("guard").debuggable(BuildConfig.DEBUG).version(BuildConfig.VERSION_CODE).tag("screenguard-service")
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val value = IGuardService.Stub.asInterface(binder) ?: return
            service = value
            pending.complete(value)
            scope.launch(Dispatchers.IO) {
                val cap = runCatching { value.capabilities() }.getOrElse { "ERROR: ${it.message}" }
                firewallSupported.value = cap == "OK"
                error.value = cap.takeIf { it != "OK" }
                mutableStatus.value = ShizukuStatus.READY
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) { disconnected() }
    }
    private fun disconnected() { service = null; firewallSupported.value = false; pending.cancel(); pending = CompletableDeferred(); mutableStatus.value = ShizukuStatus.STOPPED }
    private val received = Shizuku.OnBinderReceivedListener { refresh() }
    private val dead = Shizuku.OnBinderDeadListener { disconnected() }
    private val permission = Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() }
    init {
        Shizuku.addBinderDeadListener(dead)
        Shizuku.addRequestPermissionResultListener(permission)
        Shizuku.addBinderReceivedListenerSticky(received)
    }
    fun refresh() {
        if (android.os.Process.myUid() / 100000 != 0) {
            error.value = "Kontrol Shizuku saat ini hanya mendukung profil utama Android (user 0)."
            mutableStatus.value = ShizukuStatus.ERROR
            return
        }
        runCatching {
            if (!Shizuku.pingBinder()) { disconnected(); return }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) { mutableStatus.value = ShizukuStatus.DENIED; return }
            if (service?.asBinder()?.pingBinder() == true) return
            mutableStatus.value = ShizukuStatus.CONNECTING
            Shizuku.bindUserService(args, connection)
        }.onFailure { error.value = it.message; mutableStatus.value = ShizukuStatus.ERROR }
    }
    fun requestPermission() {
        if (!Shizuku.pingBinder()) { mutableStatus.value = ShizukuStatus.STOPPED; return }
        runCatching { Shizuku.requestPermission(10) }.onFailure { error.value = it.message; mutableStatus.value = ShizukuStatus.ERROR }
    }
    override suspend fun network(pkg: String, uid: Int, blocked: Boolean) = execute { it.network(pkg, uid, blocked) }
    override suspend fun freeze(pkg: String, userId: Int, mode: Int, enabled: Boolean) = execute { it.freeze(pkg, userId, mode, enabled) }
    private suspend fun execute(action: (IGuardService) -> String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            check(Shizuku.pingBinder()) { "Aktifkan Shizuku terlebih dahulu" }
            check(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) { "Berikan izin Shizuku" }
            refresh()
            val svc = service ?: withTimeout(12_000) { pending.await() }
            val result = action(svc)
            check(result == "OK") { result.removePrefix("ERROR: ") }
        }
    }
}
