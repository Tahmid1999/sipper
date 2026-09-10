package io.github.tahmid1999.sipper.collect

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import io.github.tahmid1999.sipper.audit.DefaultCause
import io.github.tahmid1999.sipper.audit.Grant
import io.github.tahmid1999.sipper.audit.ParsedProfile
import io.github.tahmid1999.sipper.audit.Reading
import io.github.tahmid1999.sipper.audit.Route
import io.github.tahmid1999.sipper.audit.RouteKind

public fun readViaSystemResources(): Reading<ParsedProfile> {
    val resources = Resources.getSystem()
    val id = resources.getIdentifier("power_profile", "xml", "android")
    val route = Route(RouteKind.SYSTEM_RESOURCES, "getSystem().getXml()")

    if (id == 0) {
        return Reading.SilentDefault(
            ParsedProfile(emptyMap(), emptyMap()),
            DefaultCause.KEY_ABSENT_FROM_PROFILE,
            route
        )
    }

    return try {
        val parser = resources.getXml(id)
        val profile = readProfile(parser)
        parser.close()
        Reading.Value(profile, route)
    } catch (e: Resources.NotFoundException) {
        Reading.SilentDefault(
            ParsedProfile(emptyMap(), emptyMap()),
            DefaultCause.KEY_ABSENT_FROM_PROFILE,
            route
        )
    }
}

public fun readViaAndroidPackage(context: Context): Reading<ParsedProfile> {
    val route = Route(RouteKind.ANDROID_PACKAGE_RESOURCES, "packageManager.getResourcesForApplication()")

    return try {
        val resources = context.packageManager.getResourcesForApplication("android")
        val id = resources.getIdentifier("power_profile", "xml", "android")

        if (id == 0) {
            return Reading.SilentDefault(
                ParsedProfile(emptyMap(), emptyMap()),
                DefaultCause.KEY_ABSENT_FROM_PROFILE,
                route
            )
        }

        val parser = resources.getXml(id)
        val profile = readProfile(parser)
        parser.close()
        Reading.Value(profile, route)
    } catch (e: PackageManager.NameNotFoundException) {
        Reading.SilentDefault(
            ParsedProfile(emptyMap(), emptyMap()),
            DefaultCause.KEY_ABSENT_FROM_PROFILE,
            route
        )
    } catch (e: Resources.NotFoundException) {
        Reading.SilentDefault(
            ParsedProfile(emptyMap(), emptyMap()),
            DefaultCause.KEY_ABSENT_FROM_PROFILE,
            route
        )
    }
}

public fun reflectAveragePower(context: Context, key: String): Reading<Double> {
    val route = Route(RouteKind.POWER_PROFILE_REFLECTION, key)

    return try {
        val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
        val constructor = powerProfileClass.getConstructor(Context::class.java)
        val powerProfileInstance = constructor.newInstance(context)
        val method = powerProfileClass.getMethod("getAveragePower", String::class.java)
        val result = method.invoke(powerProfileInstance, key)
        Reading.Value(result as Double, route)
    } catch (e: ClassNotFoundException) {
        // Exception class and message belong in probe_result.detail in :data, not in Reading,
        // because Reading has only four constructors and none model "the platform threw something unexpected".
        Reading.Denied("com.android.internal.os.PowerProfile", Grant.Unreachable)
    } catch (e: NoSuchMethodException) {
        // Exception class and message belong in probe_result.detail in :data, not in Reading,
        // because Reading has only four constructors and none model "the platform threw something unexpected".
        Reading.Denied("com.android.internal.os.PowerProfile.getAveragePower", Grant.Unreachable)
    } catch (e: SecurityException) {
        // Exception class and message belong in probe_result.detail in :data, not in Reading,
        // because Reading has only four constructors and none model "the platform threw something unexpected".
        Reading.Denied("com.android.internal.os.PowerProfile.getAveragePower", Grant.Unreachable)
    }
}
