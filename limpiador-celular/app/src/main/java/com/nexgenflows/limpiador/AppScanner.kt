package com.nexgenflows.limpiador

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import java.io.File
import java.util.Calendar

object AppScanner {

    fun hasUsageAccess(context: Context): Boolean {
        return buildLastUsedMap(context).isNotEmpty()
    }

    fun scan(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val lastUsedMap = buildLastUsedMap(context)

        @Suppress("DEPRECATION")
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return installed
            .filter { isRelevant(it, context.packageName) }
            .map { app ->
                AppInfo(
                    packageName = app.packageName,
                    label = pm.getApplicationLabel(app).toString(),
                    icon = runCatching { pm.getApplicationIcon(app) }.getOrNull(),
                    sizeBytes = runCatching { File(app.sourceDir).length() }.getOrDefault(0L),
                    lastUsedMillis = lastUsedMap[app.packageName],
                    isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .sortedByDescending { it.sizeBytes }
    }

    private fun isRelevant(app: ApplicationInfo, ownPackage: String): Boolean {
        if (app.packageName == ownPackage) return false
        val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val wasUpdated = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        // Apps de sistema "puras" quedan afuera: desinstalarlas puede dejar el celular inestable.
        return !isSystem || wasUpdated
    }

    private fun buildLastUsedMap(context: Context): Map<String, Long> {
        return runCatching {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val cal = Calendar.getInstance()
            cal.add(Calendar.YEAR, -2)
            usm.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, cal.timeInMillis, end)
                .associate { it.packageName to it.lastTimeUsed }
        }.getOrDefault(emptyMap())
    }
}
