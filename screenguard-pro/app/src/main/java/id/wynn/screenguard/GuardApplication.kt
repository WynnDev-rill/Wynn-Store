package id.wynn.screenguard

import android.app.Application
import id.wynn.screenguard.data.*
import id.wynn.screenguard.privilege.*
import id.wynn.screenguard.service.MonitorScheduler
import kotlinx.coroutines.*

class GuardApplication : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var store: GuardStore; private set
    lateinit var usage: UsageRepository; private set
    lateinit var catalog: AppCatalog; private set
    lateinit var gateway: ShizukuGateway; private set
    lateinit var controller: PolicyController; private set
    override fun onCreate() {
        super.onCreate()
        store = GuardStore(this)
        usage = UsageRepository(this)
        catalog = AppCatalog(this)
        gateway = ShizukuGateway(this, scope)
        controller = PolicyController(store, catalog, usage, gateway)
        scope.launch {
            gateway.status.collect { status ->
                if (status == ShizukuStatus.READY) runCatching { controller.reconcile(forceNetwork = true) }
            }
        }
        MonitorScheduler.schedule(this)
    }
}
