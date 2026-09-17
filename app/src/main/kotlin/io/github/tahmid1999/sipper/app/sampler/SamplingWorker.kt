package io.github.tahmid1999.sipper.app.sampler

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.tahmid1999.sipper.collect.readRawEvents
import java.util.concurrent.TimeUnit

public class SamplingWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val sixHoursAgo = now - TimeUnit.HOURS.toMillis(6)

        val reading = readRawEvents(applicationContext, sixHoursAgo, now)
        val eventsCount = (reading as? io.github.tahmid1999.sipper.audit.Reading.Value)?.value?.size?.toLong() ?: 0L

        val usm = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val bucketInt = if (Build.VERSION.SDK_INT >= 28 && usm != null) usm.appStandbyBucket else 0
        val bucketName = when (bucketInt) {
            UsageStatsManager.STANDBY_BUCKET_ACTIVE -> "10 active"
            UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> "20 working_set"
            UsageStatsManager.STANDBY_BUCKET_FREQUENT -> "30 frequent"
            UsageStatsManager.STANDBY_BUCKET_RARE -> "40 rare"
            45 -> "45 restricted"
            else -> "$bucketInt"
        }

        // Write to log/preferences or data repository
        val prefs = applicationContext.getSharedPreferences("sipper_sampler", Context.MODE_PRIVATE)
        val lastRun = prefs.getLong("last_run", now)
        val interval = if (lastRun == now) 0L else (now - lastRun)
        prefs.edit()
            .putLong("last_run", now)
            .putString("last_bucket", bucketName)
            .putLong("last_count", eventsCount)
            .apply()

        return Result.success()
    }

    public companion object {
        public const val WORK_NAME: String = "sipper_sampler_worker"

        public fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<SamplingWorker>(6, TimeUnit.HOURS, 1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
