package id.wynn.roadtoimmortal
import android.app.Application
import android.content.Context
import androidx.work.*
import coil3.*
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.svg.SvgDecoder
import id.wynn.roadtoimmortal.data.*
import okio.Path.Companion.toPath
import java.io.File
import java.util.concurrent.TimeUnit
class ImmortalApp:Application(),SingletonImageLoader.Factory {
 val repository by lazy {CatalogRepository(this)};val preferences by lazy {Preferences(this)}
 override fun onCreate(){super.onCreate();WorkManager.getInstance(this).enqueueUniquePeriodicWork("catalog-refresh",ExistingPeriodicWorkPolicy.KEEP,PeriodicWorkRequestBuilder<DataRefreshWorker>(6,TimeUnit.HOURS).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.MINUTES).build())}
 override fun newImageLoader(context:PlatformContext)=ImageLoader.Builder(context).components {add(SvgDecoder.Factory())}.memoryCache {MemoryCache.Builder().maxSizePercent(context,.18).build()}.diskCache {DiskCache.Builder().directory(File(cacheDir,"artwork").absolutePath.toPath()).maxSizeBytes(120L*1024*1024).build()}.build()
}
class DataRefreshWorker(context:Context,parameters:WorkerParameters):CoroutineWorker(context,parameters){
 override suspend fun doWork():Result {val repo=(applicationContext as ImmortalApp).repository;repo.initialize();return if(repo.refresh()) Result.success() else if(runAttemptCount<2) Result.retry() else Result.failure()}
}
