package io.github.tahmid1999.sipper.audit

import kotlin.annotation.AnnotationRetention
import kotlin.annotation.Retention

public sealed interface Reading<out T> {

    /** The platform answered, and the answer came from the route named. */
    public data class Value<out T>(val value: T, val route: Route) : Reading<T>

    /**
     * The platform answered, the answer is not an answer, and `shown` is what a caller that did
     * not know would have printed. Not a data class: `copy()` and `component1()` would hand out
     * `shown` without the opt-in.
     */
    public class SilentDefault<out T>(
        @property:AuditOnly public val shown: T,
        public val cause: DefaultCause,
        public val route: Route,
    ) : Reading<T> {
        override fun toString(): String = "SilentDefault(cause=$cause, route=$route)"
        override fun equals(other: Any?): Boolean =
            other is SilentDefault<*> &&
                @OptIn(AuditOnly::class) (shown == other.shown) &&
                cause == other.cause && route == other.route
        override fun hashCode(): Int =
            31 * (31 * @OptIn(AuditOnly::class) shown.hashCode() + cause.hashCode()) + route.hashCode()
    }

    /** A permission or appop is missing, and `howToGrant` says whether that is fixable. */
    public data class Denied(val permission: String, val howToGrant: Grant) : Reading<Nothing>

    /** The API does not exist below `minApi`; this device is below it. */
    public data class Absent(val minApi: Int) : Reading<Nothing>
}

@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Reads a manufactured value.")
@Retention(AnnotationRetention.BINARY)
public annotation class AuditOnly

public data class Route(val kind: RouteKind, val detail: String)

public enum class RouteKind {
    SYSTEM_RESOURCES, ANDROID_PACKAGE_RESOURCES, POWER_PROFILE_REFLECTION, BACK_FILL,
    BATTERY_BROADCAST, BATTERY_PROPERTY, PROC_FILE,
    USAGE_EVENTS, USAGE_INTERVALS, NETWORK_STATS, APP_OPS, PACKAGE_MANAGER,
}

public enum class DefaultCause {
    KEY_ABSENT_FROM_PROFILE,     // getAveragePower fell through to `return defaultValue`
    NO_USAGE_ACCESS,             // isAppInactive without the appop, API 30+
    PACKAGE_NOT_VISIBLE,         // isIgnoringBatteryOptimizations outside <queries>, API 31+
    APPOP_DEFAULT_MODE,          // AppOpsService returned opToDefaultMode(code)
    UNCLIPPED_INTERVAL_BUCKET,   // queryUsageStats summed buckets wider than the window
}

public sealed interface Grant {
    public data class SettingsToggle(val action: String, val uri: String?, val appOp: String) : Grant
    public data class ManifestQueries(val entry: String) : Grant
    public data object Unreachable : Grant
}
