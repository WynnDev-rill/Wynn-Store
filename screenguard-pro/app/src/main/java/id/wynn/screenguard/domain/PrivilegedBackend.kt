package id.wynn.screenguard.domain

/** Privileged operations remain outside policy and persistence orchestration. */
interface PrivilegedBackend {
    suspend fun network(pkg: String, uid: Int, blocked: Boolean)
    suspend fun scheduleRefreeze(pkg: String, userId: Int, deadlineElapsed: Long)
    suspend fun freeze(pkg: String, userId: Int, mode: Int, enabled: Boolean)
}
