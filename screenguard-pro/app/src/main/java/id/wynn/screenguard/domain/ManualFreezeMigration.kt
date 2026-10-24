package id.wynn.screenguard.domain

/** Only the manual freeze collection changes; network, statistics and settings remain intact. */
object ManualFreezeMigration {
    fun migrate(state: GuardState, wall: Long, elapsed: Long, boot: Int): GuardState {
        if (state.schemaVersion >= 4) return state
        return state.copy(schemaVersion = 4, rules = state.rules.mapValues { (_, r) ->
            val manual = r.managedFreeze || r.gate?.automaticOnly == false || r.gate == null && r.freeze != FreezeMode.NONE
            if (!manual) r else {
                val frozen = !AccessSession.isOpen(r, wall, elapsed, boot) &&
                    (r.freeze != FreezeMode.NONE || r.gate != null || r.ownedDisable || r.ownedSuspension)
                r.copy(managedFreeze = true, gate = null, freeze = if (frozen) FreezeMode.DISABLE else FreezeMode.NONE,
                    dailyLimitMinutes = 0, windows = emptyList(), focus = false, overrideUntil = 0,
                    sessionUntilElapsed = 0, sessionStartedElapsed = 0, sessionStartedWall = 0, sessionBoot = -1)
            }
        })
    }
}
