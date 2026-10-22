package id.wynn.screenguard.data

import id.wynn.screenguard.domain.*
import id.wynn.screenguard.privilege.ShizukuGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime

/** Shared by UI, foreground service and WorkManager: only one reconciliation at a time. */
class PolicyController(private val store: GuardStore, private val catalog: AppCatalog,
    private val usage: UsageRepository, private val gateway: ShizukuGateway) {
    private val mutex = Mutex()
    suspend fun reconcile(forceNetwork: Boolean = false): List<String> = withContext(Dispatchers.IO) { mutex.withLock {
        val snapshot = store.state.value
        if (snapshot.rules.isEmpty()) return@withLock emptyList<String>()
        val apps = catalog.managed(snapshot.rules.keys).associateBy { it.packageName }
        val used = if (snapshot.settings.monitorEnabled && usage.hasAccess()) usage.day().apps else emptyMap()
        val errors = mutableListOf<String>()
        for ((pkg, _) in snapshot.rules) {
            val app = apps[pkg] ?: continue
            var rule = store.state.value.rules[pkg] ?: continue
            try {
                check(!app.protected) { "Aplikasi penting dilindungi" }
                if (rule.networkBlocked || rule.appliedNetwork || rule.pendingNetwork) {
                    check(!app.sharedUid) { "UID dipakai bersama; blokir jaringan tidak aman" }
                    if (forceNetwork || rule.pendingNetwork || rule.networkBlocked != rule.appliedNetwork || rule.appliedUid != app.uid) {
                        gateway.network(pkg, app.uid, rule.networkBlocked)
                        store.rule(pkg) { it.copy(appliedNetwork = it.networkBlocked, appliedUid = app.uid, pendingNetwork = false) }
                    }
                }
                val decision = PolicyEngine.decide(rule, snapshot.settings, used.getOrDefault(pkg, 0), ZonedDateTime.now())
                val disable = decision.blocked && rule.freeze == FreezeMode.DISABLE
                val suspend = decision.blocked && !disable
                // Write-ahead ownership: a crash after a successful system call can be recovered.
                // Refuse to adopt apps already disabled/suspended by another tool.
                if (!disable && rule.ownedDisable) {
                    gateway.freeze(pkg, app.uid / 100000, 2, false)
                    store.rule(pkg) { it.copy(ownedDisable = false) }
                    rule = store.state.value.rules.getValue(pkg)
                }
                if (!suspend && rule.ownedSuspension) {
                    gateway.freeze(pkg, app.uid / 100000, 1, false)
                    store.rule(pkg) { it.copy(ownedSuspension = false) }
                    rule = store.state.value.rules.getValue(pkg)
                }
                if (disable) {
                    check(app.enabled || rule.ownedDisable) { "Aplikasi sudah dinonaktifkan alat lain" }
                    if (!rule.ownedDisable) store.rule(pkg) { it.copy(ownedDisable = true) }
                    if (app.enabled) gateway.freeze(pkg, app.uid / 100000, 2, true)
                } else if (suspend) {
                    check(!app.suspended || rule.ownedSuspension) { "Aplikasi sudah ditangguhkan alat lain" }
                    if (!rule.ownedSuspension) store.rule(pkg) { it.copy(ownedSuspension = true) }
                    if (!app.suspended) gateway.freeze(pkg, app.uid / 100000, 1, true)
                }
                if (rule.lastError != null) store.rule(pkg) { it.copy(lastError = null) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val message = e.message ?: "Operasi gagal"
                errors += "${app.label}: $message"
                if (rule.lastError != message) store.rule(pkg) { it.copy(lastError = message) }
            }
        }
        errors
    } }

    suspend fun restoreAll(): List<String> {
        store.update { s -> s.copy(settings = s.settings.copy(monitorEnabled = false, focusUntil = 0),
            rules = s.rules.mapValues { (_, r) -> r.copy(networkBlocked = false, freeze = FreezeMode.NONE, dailyLimitMinutes = 0, windows = emptyList(), focus = false, overrideUntil = 0) }) }
        return reconcile(forceNetwork = true)
    }
}
