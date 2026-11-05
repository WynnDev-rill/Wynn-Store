package id.wynn.iwatchyou

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** At most one running and one latest pending read, never a backlog of old windows. */
internal class LatestWork {
 private val executor=ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,ArrayBlockingQueue<Runnable>(1),ThreadPoolExecutor.DiscardOldestPolicy())
 fun execute(work:()->Unit) { executor.execute(work) }
 fun shutdownNow() { executor.shutdownNow() }
}
