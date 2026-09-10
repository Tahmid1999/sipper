package io.github.tahmid1999.sipper.collect

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import io.github.tahmid1999.sipper.audit.Reading
import io.github.tahmid1999.sipper.audit.Route
import io.github.tahmid1999.sipper.audit.RouteKind

public fun usageAccessGranted(context: Context): Reading<Boolean> {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val uid = Process.myUid()
    val packageName = context.packageName

    // unsafeCheckOpNoThrow is API 29+; checkOpNoThrow is deprecated but works on API 28.
    // The branching is required because this function runs on every resume and must not crash on
    // API 28 devices with a NoSuchMethodError.
    val mode = if (Build.VERSION.SDK_INT >= 29) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, packageName)
    }
    val granted = mode == AppOpsManager.MODE_ALLOWED   // MODE_DEFAULT is denied for this op

    // checkSelfPermission(PACKAGE_USAGE_STATS) reports denied forever even when the call works,
    // so it is not used here. MODE_DEFAULT counts as denied for this operation.
    return Reading.Value(granted, Route(RouteKind.APP_OPS, "OPSTR_GET_USAGE_STATS"))
}
