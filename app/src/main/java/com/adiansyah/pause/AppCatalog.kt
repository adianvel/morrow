package com.adiansyah.pause

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class LaunchableApp(val packageName: String, val label: String)

fun installedLaunchableApps(context: Context): List<LaunchableApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val results = if (Build.VERSION.SDK_INT >= 33) {
        context.packageManager.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(0L),
        )
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.queryIntentActivities(intent, 0)
    }
    val excludedPackages = setOf(
        context.packageName,
        "com.android.settings",
        "com.android.systemui",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
    )

    return results.mapNotNull { result ->
        val packageName = result.activityInfo?.packageName ?: return@mapNotNull null
        if (packageName in excludedPackages) return@mapNotNull null
        val label = result.loadLabel(context.packageManager).toString().trim()
        if (label.isEmpty()) null else LaunchableApp(packageName, label)
    }.distinctBy(LaunchableApp::packageName).sortedBy { it.label.lowercase() }
}

fun appIcon(context: Context, packageName: String): ImageBitmap? = try {
    val drawable = context.packageManager.getApplicationIcon(packageName)
    val size = 96
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val oldBounds = Rect(drawable.bounds)
    drawable.setBounds(0, 0, size, size)
    drawable.draw(canvas)
    drawable.bounds = oldBounds
    bitmap.asImageBitmap()
} catch (_: PackageManager.NameNotFoundException) {
    null
}

fun isMonitorEnabled(context: Context): Boolean {
    val expected = ComponentName(context, AppMonitorService::class.java).flattenToString()
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ).orEmpty()
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}

fun openAccessibilitySettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
}
