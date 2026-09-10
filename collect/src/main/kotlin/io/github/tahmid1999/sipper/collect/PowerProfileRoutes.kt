package io.github.tahmid1999.sipper.collect

import android.content.Context
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
    } catch (e: Exception) {
        Reading.Denied("getXml-exception", Grant.Unreachable)
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
    } catch (e: Exception) {
        Reading.Denied("android-package-exception", Grant.Unreachable)
    }
}

public fun readViaReflection(context: Context): Reading<ParsedProfile> {
    val route = Route(RouteKind.POWER_PROFILE_REFLECTION, "Class.forName()")

    return try {
        val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
        Reading.Denied("power-profile-no-full-dump", Grant.Unreachable)
    } catch (e: NoSuchMethodException) {
        Reading.Denied("hidden-api", Grant.Unreachable)
    } catch (e: ClassNotFoundException) {
        Reading.Denied("class-not-found", Grant.Unreachable)
    } catch (e: SecurityException) {
        Reading.Denied("hidden-api", Grant.Unreachable)
    }
}
