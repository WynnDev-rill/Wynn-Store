package id.wynn.screenguard.domain

import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

@Serializable enum class FreezeMode { NONE, SUSPEND, DISABLE }
@Serializable enum class UnlockMode { MATH, WAIT }

@Serializable data class TimeWindow(
    val name: String = "Mode Malam",
    val startMinute: Int = 22 * 60,
    val endMinute: Int = 6 * 60,
    val days: Set<Int> = (1..7).toSet(),
    val enabled: Boolean = true
) {
    init { require(startMinute in 0..1439 && endMinute in 0..1439); require(days.all { it in 1..7 }) }
    fun contains(now: ZonedDateTime): Boolean {
        if (!enabled || startMinute == endMinute) return false
        val minute = now.hour * 60 + now.minute
        return if (startMinute < endMinute) now.dayOfWeek.value in days && minute in startMinute until endMinute
        else (minute >= startMinute && now.dayOfWeek.value in days) ||
            (minute < endMinute && now.minusDays(1).dayOfWeek.value in days)
    }
}

@Serializable data class AppRule(
    val packageName: String,
    val networkBlocked: Boolean = false,
    val freeze: FreezeMode = FreezeMode.NONE,
    val dailyLimitMinutes: Int = 0,
    val windows: List<TimeWindow> = emptyList(),
    val focus: Boolean = false,
    val overrideUntil: Long = 0,
    val ownedSuspension: Boolean = false,
    val ownedDisable: Boolean = false,
    val appliedUid: Int = -1,
    val appliedNetwork: Boolean = false,
    val pendingNetwork: Boolean = false,
    val lastError: String? = null
) {
    init { require(dailyLimitMinutes in 0..1440) }
}

@Serializable data class GuardSettings(
    val onboardingDone: Boolean = false,
    val monitorEnabled: Boolean = false,
    val focusUntil: Long = 0,
    val unlockMode: UnlockMode = UnlockMode.MATH,
    val waitSeconds: Int = 30,
    val dailyGoalMinutes: Int = 240,
    val darkMode: Boolean = false,
    val pinSalt: String = "",
    val pinHash: String = "",
    val failedPins: Int = 0,
    val pinLockUntil: Long = 0
) { val hasPin: Boolean get() = pinHash.isNotBlank() }

@Serializable data class GuardState(val settings: GuardSettings = GuardSettings(), val rules: Map<String, AppRule> = emptyMap())

data class PolicyDecision(val blocked: Boolean, val reason: String)

object PolicyEngine {
    fun decide(rule: AppRule, settings: GuardSettings, usedMs: Long, now: ZonedDateTime): PolicyDecision {
        if (rule.freeze != FreezeMode.NONE) return PolicyDecision(true, "Dibekukan manual")
        if (!settings.monitorEnabled) return PolicyDecision(false, "Pemantauan nonaktif")
        if (rule.overrideUntil > now.toInstant().toEpochMilli()) return PolicyDecision(false, "Jeda buka sementara")
        if (rule.focus && settings.focusUntil > now.toInstant().toEpochMilli()) return PolicyDecision(true, "Sesi fokus")
        if (rule.windows.any { it.contains(now) }) return PolicyDecision(true, "Jadwal kunci")
        if (rule.dailyLimitMinutes > 0 && usedMs >= rule.dailyLimitMinutes * 60_000L)
            return PolicyDecision(true, "Batas harian tercapai")
        return PolicyDecision(false, "Tersedia")
    }
}

data class InstalledApp(val packageName: String, val label: String, val uid: Int, val category: Int,
    val enabled: Boolean, val suspended: Boolean, val protected: Boolean, val sharedUid: Boolean)

object PackageSafety {
    private val protectedNames = setOf("android", "com.android.settings", "com.android.systemui", "com.android.phone",
        "com.android.shell", "com.android.providers.settings", "com.android.providers.telephony", "com.android.providers.media",
        "com.android.vending", "com.google.android.gms", "moe.shizuku.privileged.api", "com.miui.securitycenter",
        "com.miui.powerkeeper", "com.xiaomi.xmsf", "com.lbe.security.miui")
    fun protected(packageName: String, uid: Int, self: String, extra: Set<String> = emptySet()): Boolean =
        uid % 100000 < 10000 || packageName == self || packageName.startsWith("id.wynn.screenguard") ||
            packageName in protectedNames || packageName in extra ||
            listOf("permissioncontroller", "packageinstaller", "launcher", "inputmethod", "keyguard").any { packageName.contains(it) }
    fun validPackage(name: String): Boolean = name.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")) && name.length <= 255
}
