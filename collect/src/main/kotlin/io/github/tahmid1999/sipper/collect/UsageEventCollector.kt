package io.github.tahmid1999.sipper.collect

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import io.github.tahmid1999.sipper.audit.DefaultCause
import io.github.tahmid1999.sipper.audit.Reading
import io.github.tahmid1999.sipper.audit.Route
import io.github.tahmid1999.sipper.audit.RouteKind
import io.github.tahmid1999.sipper.usage.RawEvent

public fun readRawEvents(
    context: Context,
    beginTimeMs: Long,
    endTimeMs: Long,
): Reading<List<RawEvent>> {
    val route = Route(RouteKind.APP_OPS, "UsageStatsManager.queryEvents")
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        ?: return Reading.SilentDefault(emptyList(), DefaultCause.KEY_ABSENT_FROM_PROFILE, route)

    return try {
        val events = usm.queryEvents(beginTimeMs, endTimeMs)
        val list = mutableListOf<RawEvent>()
        val eventObj = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(eventObj)
            list.add(
                RawEvent(
                    packageName = eventObj.packageName ?: "",
                    className = eventObj.className ?: "",
                    eventType = eventObj.eventType,
                    timestampMs = eventObj.timeStamp,
                )
            )
        }
        Reading.Value(list, route)
    } catch (e: SecurityException) {
        Reading.Denied("android.permission.PACKAGE_USAGE_STATS", io.github.tahmid1999.sipper.audit.Grant.SettingsToggle("android.settings.USAGE_ACCESS_SETTINGS", null, "OPSTR_GET_USAGE_STATS"))
    }
}
