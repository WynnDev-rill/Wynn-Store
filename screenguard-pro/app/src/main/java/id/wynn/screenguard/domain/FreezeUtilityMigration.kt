package id.wynn.screenguard.domain

/** Retire screen-time enforcement without losing the ownership needed to restore old locks. */
object FreezeUtilityMigration {
    fun migrate(state: GuardState, now: Long = System.currentTimeMillis()): GuardState {
        if (state.schemaVersion >= 3) return state
        return state.copy(schemaVersion = 3, settings = state.settings.copy(freezeUtility = true,
            monitorEnabled = false, focusUntil = 0, freezeChallengeEnabled = false,
            pinHash = "", pinSalt = "", failedPins = 0, pinLockUntil = 0),
            rules = state.rules.mapValues { (_, r) ->
                val manual = r.managedFreeze || r.gate?.automaticOnly == false || r.gate == null && r.freeze != FreezeMode.NONE
                val temporary = r.overrideUntil > now && r.sessionUntilElapsed > 0
                r.copy(managedFreeze = manual, gate = null,
                    freeze = if (manual && !temporary) FreezeMode.DISABLE else FreezeMode.NONE,
                    networkBlocked = false, pendingNetwork = r.pendingNetwork || r.appliedNetwork,
                    dailyLimitMinutes = 0, windows = emptyList(), focus = false, overrideUntil = 0,
                    sessionUntilElapsed = 0, sessionStartedElapsed = 0, sessionStartedWall = 0, sessionBoot = -1)
            })
    }
}
