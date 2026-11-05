package com.baff.launcher.apps

import android.content.Context
import android.content.Intent

class AppRepository(private val context: Context) {
    @Suppress("DEPRECATION")
    fun getLaunchableApps(): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager.queryIntentActivities(intent, 0)
            .asSequence()
            .mapNotNull { info ->
                val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
                val label = info.loadLabel(context.packageManager)?.toString()
                    ?.takeIf { it.isNotBlank() } ?: packageName
                AppInfo(label, packageName)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
