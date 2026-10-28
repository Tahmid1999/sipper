package io.github.tahmid1999.sipper.collect

import io.github.tahmid1999.sipper.audit.DefaultCause
import io.github.tahmid1999.sipper.audit.Reading
import io.github.tahmid1999.sipper.audit.Route
import io.github.tahmid1999.sipper.audit.RouteKind
import java.io.File

public data class MemInfo(
    val memTotalBytes: Long,
    val memAvailableBytes: Long,
    val memFreeBytes: Long,
    val cachedBytes: Long,
    val swapTotalBytes: Long,
    val swapFreeBytes: Long,
    val rawLines: Map<String, Long>,
)

public fun readMemInfo(): Reading<MemInfo> {
    val route = Route(RouteKind.SYSTEM_RESOURCES, "/proc/meminfo")
    val file = File("/proc/meminfo")

    if (!file.canRead()) {
        return Reading.SilentDefault(
            MemInfo(0, 0, 0, 0, 0, 0, emptyMap()),
            DefaultCause.KEY_ABSENT_FROM_PROFILE,
            route
        )
    }

    val lines = mutableMapOf<String, Long>()
    file.forEachLine { line ->
        val parts = line.split(":")
        if (parts.size == 2) {
            val key = parts[0].trim()
            val valStr = parts[1].trim().split(" ")[0].trim()
            val kb = valStr.toLongOrNull() ?: 0L
            lines[key] = kb * 1024L
        }
    }

    val info = MemInfo(
        memTotalBytes = lines["MemTotal"] ?: 0L,
        memAvailableBytes = lines["MemAvailable"] ?: 0L,
        memFreeBytes = lines["MemFree"] ?: 0L,
        cachedBytes = lines["Cached"] ?: 0L,
        swapTotalBytes = lines["SwapTotal"] ?: 0L,
        swapFreeBytes = lines["SwapFree"] ?: 0L,
        rawLines = lines,
    )
    return Reading.Value(info, route)
}

public data class CpuClusterInfo(
    val clusterId: Int,
    val coreCount: Int,
    val curKHz: Long,
    val minKHz: Long,
    val maxKHz: Long,
    val governor: String,
)

public fun readCpuClusters(): Reading<List<CpuClusterInfo>> {
    val route = Route(RouteKind.SYSTEM_RESOURCES, "/sys/devices/system/cpu/cpufreq")
    val cpuDir = File("/sys/devices/system/cpu")

    if (!cpuDir.exists()) {
        return Reading.SilentDefault(emptyList(), DefaultCause.KEY_ABSENT_FROM_PROFILE, route)
    }

    val clusters = mutableListOf<CpuClusterInfo>()

    // listFiles has no order, and on the ANE-LX2 it returned policy4 first. Sort by the number.
    val policyDirs = (File("/sys/devices/system/cpu/cpufreq").listFiles { f ->
        f.isDirectory && f.name.startsWith("policy")
    } ?: emptyArray()).sortedBy { it.name.removePrefix("policy").toIntOrNull() ?: Int.MAX_VALUE }

    for ((idx, pDir) in policyDirs.withIndex()) {
        // Each policy owns only the cores in its related_cpus, not every cpuN directory.
        val coreCount = File(pDir, "related_cpus").let { if (it.canRead()) countCpus(it.readText()) else 0 }
        val curFreq = File(pDir, "scaling_cur_freq").let { if (it.canRead()) it.readText().trim().toLongOrNull() else null } ?: 0L
        val minFreq = File(pDir, "scaling_min_freq").let { if (it.canRead()) it.readText().trim().toLongOrNull() else null } ?: 0L
        val maxFreq = File(pDir, "scaling_max_freq").let { if (it.canRead()) it.readText().trim().toLongOrNull() else null } ?: 0L
        val gov = File(pDir, "scaling_governor").let { if (it.canRead()) it.readText().trim() else "" }

        clusters.add(
            CpuClusterInfo(
                clusterId = idx,
                coreCount = coreCount,
                curKHz = curFreq,
                minKHz = minFreq,
                maxKHz = maxFreq,
                governor = gov,
            )
        )
    }

    return if (clusters.isEmpty()) {
        Reading.SilentDefault(emptyList(), DefaultCause.KEY_ABSENT_FROM_PROFILE, route)
    } else {
        Reading.Value(clusters, route)
    }
}

// related_cpus is "0 1 2 3" on this kernel; some kernels write ranges like "0-3" or "0-3,6".
internal fun countCpus(list: String): Int =
    list.trim().split(' ', ',').filter { it.isNotEmpty() }.sumOf { part ->
        val bounds = part.split('-')
        val lo = bounds[0].toIntOrNull() ?: return@sumOf 0
        val hi = bounds.getOrNull(1)?.toIntOrNull() ?: lo
        hi - lo + 1
    }
