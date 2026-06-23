package com.nexgenflows.limpiador

import android.content.Context
import java.util.Locale
import java.util.concurrent.TimeUnit

object Formatters {

    fun size(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024) String.format(Locale.getDefault(), "%.1f GB", mb / 1024.0)
        else String.format(Locale.getDefault(), "%.0f MB", mb)
    }

    fun lastUsed(context: Context, millis: Long?): String {
        if (millis == null || millis <= 0L) return context.getString(R.string.last_used_unknown)
        val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - millis)
        return when {
            days <= 0 -> context.getString(R.string.last_used_today)
            days == 1L -> context.getString(R.string.last_used_yesterday)
            else -> context.getString(R.string.last_used_days_ago, days)
        }
    }
}
