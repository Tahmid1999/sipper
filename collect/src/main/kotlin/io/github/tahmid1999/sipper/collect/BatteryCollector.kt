package io.github.tahmid1999.sipper.collect

import android.os.BatteryManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import io.github.tahmid1999.sipper.audit.DefaultCause
import io.github.tahmid1999.sipper.audit.Reading
import io.github.tahmid1999.sipper.audit.Route
import io.github.tahmid1999.sipper.audit.RouteKind

public data class BatteryState(
    val level: Int,
    val scale: Int,
    val status: Int,
    val health: Int,
    val plugged: Int,
    val present: Boolean,
    val technology: String,
    val voltage: Int,
    val temperature: Int,
)

public fun readBatteryState(context: Context): Reading<BatteryState> {
    val route = Route(RouteKind.SYSTEM_RESOURCES, "ACTION_BATTERY_CHANGED")
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        ?: return Reading.SilentDefault(
            BatteryState(0, 100, 0, 0, 0, false, "", 0, 0),
            DefaultCause.KEY_ABSENT_FROM_PROFILE,
            route
        )

    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
    val present = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false)
    val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: ""
    val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
    val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)

    val state = BatteryState(
        level = level,
        scale = scale,
        status = status,
        health = health,
        plugged = plugged,
        present = present,
        technology = technology,
        voltage = voltage,
        temperature = temperature,
    )
    return Reading.Value(state, route)
}

public fun readChargeCounter(context: Context): Reading<Long> {
    val route = Route(RouteKind.SYSTEM_RESOURCES, "BATTERY_PROPERTY_CHARGE_COUNTER")
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        ?: return Reading.SilentDefault(0L, DefaultCause.KEY_ABSENT_FROM_PROFILE, route)

    val counter = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
    return if (counter <= 0) {
        Reading.SilentDefault(0L, DefaultCause.KEY_ABSENT_FROM_PROFILE, route)
    } else {
        Reading.Value(counter, route)
    }
}
