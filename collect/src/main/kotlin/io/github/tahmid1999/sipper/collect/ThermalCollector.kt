package io.github.tahmid1999.sipper.collect

import io.github.tahmid1999.sipper.audit.DefaultCause
import io.github.tahmid1999.sipper.audit.Reading
import io.github.tahmid1999.sipper.audit.Route
import io.github.tahmid1999.sipper.audit.RouteKind
import java.io.File

public enum class TempUnit { MILLI, DEGREE }

public sealed interface Temp {
    public data class Celsius(val c: Double, val unit: TempUnit) : Temp
    public data object Sentinel : Temp
    public data class Unclassified(val raw: Long) : Temp
}

public fun classifyTemp(raw: Long): Temp {
    if (raw == -40L || raw == -40000L) return Temp.Sentinel
    if (raw >= 1000L) return Temp.Celsius(raw / 1000.0, TempUnit.MILLI)
    if (raw > -40L && raw < 200L) return Temp.Celsius(raw.toDouble(), TempUnit.DEGREE)
    return Temp.Unclassified(raw)
}

public data class ThermalZone(
    val index: Int,
    val type: String,
    val rawValue: Long,
    val temp: Temp,
)

public fun readThermalZones(): Reading<List<ThermalZone>> {
    val route = Route(RouteKind.SYSTEM_RESOURCES, "/sys/class/thermal/thermal_zone*")
    val baseDir = File("/sys/class/thermal")
    val altDir = File("/sys/devices/virtual/thermal")
    val targetDir = if (baseDir.exists() && baseDir.canRead()) baseDir else altDir

    if (!targetDir.exists()) {
        return Reading.SilentDefault(emptyList(), DefaultCause.KEY_ABSENT_FROM_PROFILE, route)
    }

    val zones = mutableListOf<ThermalZone>()
    val dirs = targetDir.listFiles { file -> file.isDirectory && file.name.startsWith("thermal_zone") }
        ?: return Reading.SilentDefault(emptyList(), DefaultCause.KEY_ABSENT_FROM_PROFILE, route)

    for (dir in dirs.sortedBy { it.name }) {
        val index = dir.name.removePrefix("thermal_zone").toIntOrNull() ?: continue
        val typeFile = File(dir, "type")
        val tempFile = File(dir, "temp")

        if (!typeFile.canRead() || !tempFile.canRead()) continue

        val type = typeFile.readText().trim()
        val rawValue = tempFile.readText().trim().toLongOrNull() ?: continue
        val temp = classifyTemp(rawValue)
        zones.add(ThermalZone(index, type, rawValue, temp))
    }

    return if (zones.isEmpty()) {
        Reading.SilentDefault(emptyList(), DefaultCause.KEY_ABSENT_FROM_PROFILE, route)
    } else {
        Reading.Value(zones, route)
    }
}
