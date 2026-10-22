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
    val challengeError = MutableStateFlow<String?>(null)
    val remaining = MutableStateFlow(0)
    val unlocking = MutableStateFlow<String?>(null)
    private var pendingAction: (() -> Unit)? = null
    private var authorizedUntil = 0L
    private var waitDeadline = 0L
    private var countdown: Job? = null
    private var loading: Job? = null
    init { refresh() }
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
    fun freeze(app: InstalledApp, mode: FreezeMode) = secure { work {
        check(!app.protected) { "Aplikasi penting dilindungi" }
        check(status.value == id.wynn.screenguard.privilege.ShizukuStatus.READY) { "Hubungkan Shizuku terlebih dahulu" }
        graph.store.rule(app.packageName) { it.copy(freeze = mode, overrideUntil = 0) }; reconcile()
    } }
    fun saveRule(app: InstalledApp, minutes: Int, window: TimeWindow?, focus: Boolean) = secure { work {
        check(!app.protected)
        graph.store.rule(app.packageName) { it.copy(dailyLimitMinutes = minutes, windows = listOfNotNull(window), focus = focus, overrideUntil = 0) }
        reconcile()
    } }
    fun settings(change: (GuardSettings) -> GuardSettings) = secure { work { graph.store.settings(change); reconcile() } }
    fun monitor(enabled: Boolean) = secure { work {
        if (enabled) {
            check(device.value.usageAccess) { "Berikan akses penggunaan dahulu" }
            check(status.value == id.wynn.screenguard.privilege.ShizukuStatus.READY) { "Hubungkan Shizuku dahulu" }
        }
        graph.store.settings { it.copy(monitorEnabled = enabled) }
        if (enabled && !GuardMonitorService.start(graph)) { graph.store.settings { it.copy(monitorEnabled = false) }; error("Android menolak layanan. Buka aplikasi dan coba lagi.") }
        reconcile()
    } }
    fun focus(minutes: Int) = secure { work {
        check(state.value.rules.values.any { it.focus }) { "Pilih aplikasi fokus pada Batas Aplikasi dahulu" }
        check(device.value.usageAccess && status.value == id.wynn.screenguard.privilege.ShizukuStatus.READY) { "Aktifkan izin penggunaan dan Shizuku dahulu" }
        graph.store.settings { it.copy(focusUntil = if (minutes == 0) 0 else System.currentTimeMillis() + minutes * 60_000L, monitorEnabled = true) }
        check(GuardMonitorService.start(graph)) { "Layanan pemantauan gagal dimulai" }; reconcile()
    } }
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
    fun prepareUnlock(app: InstalledApp, navigate: () -> Unit) = secure {
        unlocking.value = app.packageName; challenge.value = MathChallenge.next(); challengeError.value = null
        waitDeadline = SystemClock.elapsedRealtime() + state.value.settings.waitSeconds * 1000L
        countdown?.cancel()
        countdown = viewModelScope.launch {
            while (isActive) { remaining.value = ((waitDeadline - SystemClock.elapsedRealtime()).coerceAtLeast(0) / 1000 + 1).toInt(); if (SystemClock.elapsedRealtime() >= waitDeadline) { remaining.value = 0; break }; delay(250) }
        }
        navigate()
    }
    fun newChallenge() { challenge.value = MathChallenge.next(); challengeError.value = null }
    fun unlock(answer: String, launch: (String) -> Unit) = secure {
        val pkg = unlocking.value ?: return@secure
        val settings = state.value.settings
        if ((settings.unlockMode == UnlockMode.MATH && !challenge.value.accepts(answer)) ||
            (settings.unlockMode == UnlockMode.WAIT && SystemClock.elapsedRealtime() < waitDeadline)) {
            challengeError.value = if (settings.unlockMode == UnlockMode.MATH) "Jawaban belum tepat. Dahulukan perkalian." else "Waktu tunggu belum selesai."
        } else work {
            check(status.value == id.wynn.screenguard.privilege.ShizukuStatus.READY) { "Hubungkan Shizuku untuk membuka aplikasi" }
            graph.store.rule(pkg) { it.copy(freeze = FreezeMode.NONE, overrideUntil = System.currentTimeMillis() + 5 * 60_000) }
            reconcile()
            val rule = state.value.rules[pkg]
            check(rule?.ownedDisable != true && rule?.ownedSuspension != true) { rule?.lastError ?: "Aplikasi belum berhasil dipulihkan" }
            val actual = withContext(Dispatchers.IO) { graph.catalog.managed(setOf(pkg)).firstOrNull() }
            check(actual?.enabled == true && !actual.suspended) { "Android belum membuka aplikasi; periksa statusnya kembali" }
            launch(pkg)
            if (rule?.lastError == null) message.value = "Aplikasi dipulihkan. Batas dan jadwal mendapat jeda 5 menit."
        }
    }
    fun reason(app: InstalledApp): String = state.value.rules[app.packageName]?.let {
        PolicyEngine.decide(it, state.value.settings, device.value.days.lastOrNull()?.apps?.get(app.packageName) ?: 0, ZonedDateTime.now()).reason
    } ?: "Tersedia"
}
