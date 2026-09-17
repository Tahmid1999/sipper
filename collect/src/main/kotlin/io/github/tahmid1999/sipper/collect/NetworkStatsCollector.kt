package io.github.tahmid1999.sipper.collect

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.RemoteException
import io.github.tahmid1999.sipper.audit.DefaultCause
import io.github.tahmid1999.sipper.audit.Reading
import io.github.tahmid1999.sipper.audit.Route
import io.github.tahmid1999.sipper.audit.RouteKind

public data class NetworkUidBytes(
    val uid: Int,
    val rxFgBytes: Long,
    val txFgBytes: Long,
    val rxBgBytes: Long,
    val txBgBytes: Long,
)

public fun readNetworkStatsSummary(
    context: Context,
    beginTimeMs: Long,
    endTimeMs: Long,
): Reading<List<NetworkUidBytes>> {
    val route = Route(RouteKind.APP_OPS, "NetworkStatsManager.querySummary")
    val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
        ?: return Reading.SilentDefault(emptyList(), DefaultCause.KEY_ABSENT_FROM_PROFILE, route)

    return try {
        val map = mutableMapOf<Int, LongArray>() // uid -> [rxFg, txFg, rxBg, txBg]

        // 1. Wifi stats
        val wifiStats = nsm.querySummary(ConnectivityManager.TYPE_WIFI, null, beginTimeMs, endTimeMs)
        drainStatsIntoMap(wifiStats, map)

        // 2. Mobile stats (Only API 29+ without subscriberId required)
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                val mobileStats = nsm.querySummary(ConnectivityManager.TYPE_MOBILE, null, beginTimeMs, endTimeMs)
                drainStatsIntoMap(mobileStats, map)
            } catch (_: Exception) {
                // Ignore mobile stats failure if subscriberId permission issues occur
            }
        }

        val result = map.map { (uid, bytes) ->
            NetworkUidBytes(
                uid = uid,
                rxFgBytes = bytes[0],
                txFgBytes = bytes[1],
                rxBgBytes = bytes[2],
                txBgBytes = bytes[3],
            )
        }
        Reading.Value(result, route)
    } catch (e: SecurityException) {
        Reading.Denied("android.permission.PACKAGE_USAGE_STATS", io.github.tahmid1999.sipper.audit.Grant.SettingsToggle("android.settings.USAGE_ACCESS_SETTINGS", null, "OPSTR_GET_USAGE_STATS"))
    } catch (e: RemoteException) {
        Reading.SilentDefault(emptyList(), DefaultCause.KEY_ABSENT_FROM_PROFILE, route)
    }
}

private fun drainStatsIntoMap(stats: NetworkStats?, map: MutableMap<Int, LongArray>) {
    if (stats == null) return
    val bucket = NetworkStats.Bucket()
    while (stats.hasNextBucket()) {
        stats.getNextBucket(bucket)
        val arr = map.getOrPut(bucket.uid) { LongArray(4) }
        if (bucket.state == NetworkStats.Bucket.STATE_FOREGROUND) {
            arr[0] += bucket.rxBytes
            arr[1] += bucket.txBytes
        } else {
            arr[2] += bucket.rxBytes
            arr[3] += bucket.txBytes
        }
    }
    stats.close()
}
