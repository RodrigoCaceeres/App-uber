package com.nexgenflows.limpiador

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val sizeBytes: Long,
    val lastUsedMillis: Long?,
    val isSystemApp: Boolean
)
