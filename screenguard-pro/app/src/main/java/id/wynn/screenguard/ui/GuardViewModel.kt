package id.wynn.screenguard.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.wynn.screenguard.GuardApplication
import id.wynn.screenguard.data.*
import id.wynn.screenguard.domain.*
import id.wynn.screenguard.service.GuardMonitorService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.ZonedDateTime

data class DeviceData(val apps: List<InstalledApp> = emptyList(), val days: List<UsageDay> = emptyList(), val usageAccess: Boolean = false, val loading: Boolean = true)

class GuardViewModel(application: Application) : AndroidViewModel(application) {
    val graph = application as GuardApplication
    val updater = id.wynn.screenguard.update.AppUpdater(application)
    private var updateJob: Job? = null
    private var lastUpdateAttempt = 0L
    fun checkUpdates(manual: Boolean = false) {
        if (!manual && (!updater.automatic.value || lastUpdateAttempt > 0 && SystemClock.elapsedRealtime() - lastUpdateAttempt < 120_000)) return
        lastUpdateAttempt = SystemClock.elapsedRealtime()
        viewModelScope.launch { updater.check() }
    }
    fun downloadUpdate() { if (updateJob?.isActive != true) updateJob = viewModelScope.launch { updater.download() } }
    fun cancelUpdate() { updateJob?.cancel() }
    fun installUpdate(launch: (android.content.Intent) -> Unit) { secure { viewModelScope.launch {
        updater.install()?.let { intent -> runCatching { launch(intent) }.onFailure { message.value = "Pemasang tidak dapat dibuka: ${it.message}" } }
    } } }
    val state = graph.store.state
    val status = graph.gateway.status
    val firewall = graph.gateway.firewallSupported.asStateFlow()
    val device = MutableStateFlow(DeviceData())
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(graph.store.loadError)
    val pinPrompt = MutableStateFlow(false)
    val pinError = MutableStateFlow<String?>(null)
    val pinWorking = MutableStateFlow(false)
    val challenge = MutableStateFlow(MathChallenge.next())
    val completedQuestions = MutableStateFlow(0)
    val challengeGate = MutableStateFlow(AccessGate())
    val challengeError = MutableStateFlow<String?>(null)
    val remaining = MutableStateFlow(0)
    val unlocking = MutableStateFlow<String?>(null)
    private var pendingAction: (() -> Unit)? = null
    private var authorizedUntil = 0L
    private var waitDeadline = 0L
    private var countdown: Job? = null
    private var loading: Job? = null
    init { refresh() }
    fun recover() { viewModelScope.launch { runCatching { graph.controller.reconcile() }; loading?.join(); refresh() } }
    fun refresh() {
        graph.gateway.refresh()
        if (loading?.isActive == true) return
        loading = viewModelScope.launch {
            runCatching {
                val value = withContext(Dispatchers.IO) { DeviceData(graph.catalog.list(state.value.rules.keys), graph.usage.range(30), graph.usage.hasAccess(), false) }
                device.value = value
            }.onFailure { device.value = device.value.copy(loading = false); message.value = it.message ?: "Gagal memuat data" }
        }
    }
    fun secure(action: () -> Unit) {
        if (state.value.settings.hasPin && SystemClock.elapsedRealtime() >= authorizedUntil) {
            pendingAction = action; pinError.value = null; pinPrompt.value = true
        } else action()
    }
    fun invalidateSession() { authorizedUntil = 0; pendingAction = null; pinPrompt.value = false }
    fun cancelPin() { pendingAction = null; pinPrompt.value = false; pinError.value = null }
    fun verifyPin(pin: String) {
        if (pinWorking.value) return
        pinWorking.value = true
        viewModelScope.launch {
            try {
                val s = state.value.settings
                if (System.currentTimeMillis() < s.pinLockUntil) { pinError.value = "Terlalu banyak percobaan. Tunggu ${(s.pinLockUntil - System.currentTimeMillis()) / 1000 + 1} detik."; return@launch }
                val valid = withContext(Dispatchers.Default) { PinCrypto.verify(pin, s.pinSalt, s.pinHash) }
                if (valid) {
                    graph.store.settings { it.copy(failedPins = 0, pinLockUntil = 0) }
                    authorizedUntil = SystemClock.elapsedRealtime() + 120_000
                    pinPrompt.value = false
                    val action = pendingAction; pendingAction = null; action?.invoke()
                } else {
                    val failures = s.failedPins + 1
                    graph.store.settings { it.copy(failedPins = failures,
                        pinLockUntil = if (failures >= 5) System.currentTimeMillis() + (30_000L * (failures - 4)).coerceAtMost(900_000) else 0) }
                    pinError.value = "PIN salah. ${if (failures >= 5) "Percobaan dikunci sementara." else "Coba lagi."}"
                }
            } catch (e: Exception) { pinError.value = e.message }
            finally { pinWorking.value = false }
        }
    }
    private fun work(action: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try { action() } catch (e: Exception) {
                if (e is CancellationException) throw e
                message.value = e.message ?: "Perubahan gagal"
            } finally { busy.value = false; refresh() }
        }
    }
    private suspend fun reconcile() {
        val errors = graph.controller.reconcile()
        if (errors.isNotEmpty()) message.value = errors.joinToString("\n").take(700)
    }
    fun grantUsageThroughShizuku() = secure { work {
        graph.gateway.grantOwnUsageAccess()
        check(graph.usage.hasAccess()) { "Perangkat belum mengizinkan akses. Gunakan Info aplikasi → Izinkan setelan terbatas, lalu coba kembali." }
        message.value = "Akses penggunaan aktif. Statistik dan batas harian siap digunakan."
    } }
    fun setNetwork(app: InstalledApp, blocked: Boolean) = secure { work {
        check(!app.protected && !app.sharedUid) { "Aplikasi ini dilindungi atau memakai UID bersama" }
        check(firewall.value) { "Firewall tidak tersedia. Periksa Shizuku dan dukungan Android." }
        // Journal intent before side effects; UI uses applied state, not requested state.
        graph.store.rule(app.packageName) { it.copy(networkBlocked = blocked, pendingNetwork = true) }
        try {
            graph.gateway.network(app.packageName, app.uid, blocked)
            graph.store.rule(app.packageName) { it.copy(appliedNetwork = blocked, appliedUid = app.uid, pendingNetwork = false, lastError = null) }
        } catch (e: Exception) {
            graph.store.rule(app.packageName) { it.copy(lastError = e.message ?: "Perubahan jaringan gagal") }
            throw e
        }
    } }
    /** Add an app to the curated freeze list and immediately disable it through Shizuku. */
    fun addFrozen(app: InstalledApp) = secure { work {
        check(FreezeCatalog.canAdd(app, state.value.rules[app.packageName])) { "Aplikasi ini tidak dapat ditambahkan atau sudah dikelola." }
        check(status.value == id.wynn.screenguard.privilege.ShizukuStatus.READY) { "Hubungkan Shizuku terlebih dahulu" }
        graph.store.rule(app.packageName) {
            it.copy(gate = AccessGate(accessMinutes = 5), freeze = FreezeMode.DISABLE, overrideUntil = 0, lastError = null)
        }
        reconcile()
    } }

    /** Opening from the curated list has no challenge unless the user opts in. */
    fun openTemporarily(app: InstalledApp, launch: (String) -> Unit) {
        val rule = state.value.rules[app.packageName] ?: return
        if (rule.gate == null || rule.gate.automaticOnly) return
        if (app.enabled && !app.suspended && sessionOpen(rule)) { launch(app.packageName); return }
        work { grantTemporaryAccess(app.packageName, state.value.settings.freezeGate.accessMinutes, launch) }
    }

    fun configureGate(app: InstalledApp, gate: AccessGate) = secure { work {
        check(!app.protected && app.launchable) { "Aplikasi penting atau tanpa peluncur dilindungi" }
        check(status.value == id.wynn.screenguard.privilege.ShizukuStatus.READY) { "Hubungkan Shizuku terlebih dahulu" }
        val existing = state.value.rules[app.packageName]
        check(app.enabled && !app.suspended || existing?.ownedDisable == true || existing?.ownedSuspension == true) { "Aplikasi dikunci alat lain; pulihkan melalui alat tersebut dahulu" }
        graph.store.rule(app.packageName) { it.copy(gate = gate, freeze = if (gate.automaticOnly) FreezeMode.NONE else FreezeMode.DISABLE, overrideUntil = 0) }
        syncMonitor(); reconcile()
    } }
    private suspend fun clearGate(pkg: String) {
        graph.store.rule(pkg) { it.copy(gate = null, freeze = FreezeMode.NONE, dailyLimitMinutes = 0,
            windows = emptyList(), focus = false, overrideUntil = 0) }
        syncMonitor()
        reconcile()
    }
    fun removeGate(app: InstalledApp) = secure { work { clearGate(app.packageName) } }

    fun closeAccess(app: InstalledApp) = work {
        graph.store.rule(app.packageName) { it.copy(overrideUntil = 0) }; reconcile()
    }
    fun saveRule(app: InstalledApp, minutes: Int, window: TimeWindow?) = secure { work {
        check(!app.protected)
        check(minutes == 0 || device.value.usageAccess) { "Berikan akses penggunaan untuk batas harian" }
        check(status.value == id.wynn.screenguard.privilege.ShizukuStatus.READY) { "Hubungkan Shizuku terlebih dahulu" }
        graph.store.rule(app.packageName) { it.copy(dailyLimitMinutes = minutes, windows = listOfNotNull(window), focus = false, overrideUntil = 0,
            freeze = FreezeMode.NONE, gate = if (minutes > 0 || window != null) (it.gate ?: AccessGate()).copy(automaticOnly = true) else null) }
        syncMonitor()
        reconcile()
    } }
    private suspend fun syncMonitor() {
        val enabled = state.value.rules.values.any { it.gate?.automaticOnly == true && (it.dailyLimitMinutes > 0 || it.windows.isNotEmpty()) }
        graph.store.settings { it.copy(monitorEnabled = enabled) }
        if (enabled) check(GuardMonitorService.start(graph)) { "Android menolak pemantauan; buka aplikasi lalu simpan aturan kembali" }
    }
    fun canLaunchWithoutChallenge(app: InstalledApp): Boolean {
        val rule = state.value.rules[app.packageName] ?: return true
        return sessionOpen(rule) || rule.gate?.automaticOnly == true && !PolicyEngine.decide(rule, state.value.settings,
            device.value.days.lastOrNull()?.apps?.get(app.packageName) ?: 0, ZonedDateTime.now(), SystemClock.elapsedRealtime(), graph.usage.bootCount()).blocked
    }
    fun settings(change: (GuardSettings) -> GuardSettings) = secure { work { graph.store.settings(change); reconcile() } }
    fun savePin(pin: String, confirm: String) = secure { work {
        check(pin == confirm) { "Konfirmasi PIN berbeda" }; check(PinCrypto.valid(pin)) { "Gunakan tepat 6 angka" }
        val (salt, hash) = withContext(Dispatchers.Default) { PinCrypto.create(pin) }
        graph.store.settings { it.copy(pinSalt = salt, pinHash = hash, failedPins = 0, pinLockUntil = 0) }
        authorizedUntil = 0; message.value = "PIN orang tua disimpan"
    } }
    fun removePin() = secure { work { graph.store.settings { it.copy(pinSalt = "", pinHash = "", failedPins = 0, pinLockUntil = 0) }; authorizedUntil = 0 } }
    fun restore() = secure { work {
        val errors = graph.controller.restoreAll()
        message.value = if (errors.isEmpty()) "Semua aturan ScreenGuard sudah dipulihkan" else "Belum sepenuhnya pulih:\n${errors.joinToString("\n")}" 
    } }
    fun onboarded() = work { graph.store.settings { it.copy(onboardingDone = true) } }
    fun prepareUnlock(app: InstalledApp, navigate: () -> Unit) {
        val rule = state.value.rules[app.packageName] ?: return
        val gate = state.value.settings.freezeGate
        challengeGate.value = gate
        unlocking.value = app.packageName; challenge.value = MathChallenge.next(gate.level)
        completedQuestions.value = 0; challengeError.value = null
        waitDeadline = SystemClock.elapsedRealtime() + if (gate.hasWait) gate.waitSeconds * 1000L else 0
        remaining.value = if (gate.hasWait) gate.waitSeconds else 0
        countdown?.cancel()
        countdown = viewModelScope.launch {
            while (isActive) {
                remaining.value = ((waitDeadline - SystemClock.elapsedRealtime()).coerceAtLeast(0) + 999).div(1000).toInt()
                if (remaining.value == 0) break
                delay(200)
            }
        }
        navigate()
    }
    fun cancelChallenge() { countdown?.cancel(); unlocking.value = null; completedQuestions.value = 0 }
    fun newChallenge() { challenge.value = MathChallenge.next(challengeGate.value.level); challengeError.value = null }
    /** Returns true only after the configured wait and every math question have passed. */
    private fun challengePassed(answer: String): Boolean {
        val gate = challengeGate.value
        if (SystemClock.elapsedRealtime() < waitDeadline) {
            challengeError.value = "Tunggu hingga jeda selesai."
            return false
        }
        if (gate.hasMath && completedQuestions.value < gate.questions) {
            if (!challenge.value.accepts(answer)) {
                challengeError.value = "Jawaban belum tepat."
                return false
            }
            completedQuestions.value += 1
            challengeError.value = null
            if (completedQuestions.value < gate.questions) { newChallenge(); return false }
        }
        return true
    }
    fun unlock(answer: String, launch: (String) -> Unit) {
        val pkg = unlocking.value ?: return
        if (!challengePassed(answer)) return
        work { grantTemporaryAccess(pkg, challengeGate.value.accessMinutes, launch) }
    }
    fun releaseAfterChallenge(answer: String, done: () -> Unit) {
        val pkg = unlocking.value ?: return
        if (!challengePassed(answer)) return
        secure { work {
            check(status.value == id.wynn.screenguard.privilege.ShizukuStatus.READY) { "Hubungkan Shizuku untuk memulihkan aplikasi" }
            clearGate(pkg)
            cancelChallenge()
            done()
        } }
    }

    private suspend fun grantTemporaryAccess(pkg: String, minutes: Int, launch: (String) -> Unit) {
        check(status.value == id.wynn.screenguard.privilege.ShizukuStatus.READY) { "Hubungkan Shizuku untuk membuka aplikasi" }
        val rule = state.value.rules[pkg] ?: error("Aplikasi sudah dikeluarkan dari Bekukan")
        check(rule.gate != null && !rule.gate.automaticOnly) { "Aplikasi tidak ada dalam daftar Bekukan" }
        val wall = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        graph.store.rule(pkg) {
            it.copy(freeze = FreezeMode.DISABLE,
                overrideUntil = wall + minutes * 60_000L,
                sessionStartedWall = wall, sessionStartedElapsed = elapsed,
                sessionUntilElapsed = elapsed + minutes * 60_000L, sessionBoot = graph.usage.bootCount())
        }
        reconcile()
        val actual = withContext(Dispatchers.IO) { graph.catalog.managed(setOf(pkg)).firstOrNull() }
        check(actual?.enabled == true && !actual.suspended) { state.value.rules[pkg]?.lastError ?: "Android belum mengaktifkan aplikasi" }
        check(GuardMonitorService.start(graph)) { "Layanan penguncian ulang gagal dijalankan. Akses sementara perlu dipantau." }
        launch(pkg)
    }
    fun sessionOpen(rule: AppRule): Boolean = AccessSession.isOpen(rule, System.currentTimeMillis(), SystemClock.elapsedRealtime(), graph.usage.bootCount())
}
