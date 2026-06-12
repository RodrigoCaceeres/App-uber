package com.rodrigocaceres.autopausa

import android.content.Context
import android.content.SharedPreferences

/** Preferencias y estado compartido entre los servicios y la pantalla principal. */
object Prefs {

    private const val FILE = "autopausa_prefs"

    private const val KEY_ENABLED = "enabled"
    private const val KEY_ACTIVE_TRIP_APP = "active_trip_app"
    private const val KEY_LAST_EVENT = "last_event"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Interruptor general: si está apagado, no se hace nada automático. */
    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** App con la que hay un viaje activo en este momento (o null si ninguna). */
    fun getActiveTripApp(context: Context): RideApp? =
        prefs(context).getString(KEY_ACTIVE_TRIP_APP, null)?.let {
            runCatching { RideApp.valueOf(it) }.getOrNull()
        }

    fun setActiveTripApp(context: Context, app: RideApp?) {
        prefs(context).edit().putString(KEY_ACTIVE_TRIP_APP, app?.name).apply()
    }

    /** Último evento registrado, para mostrarlo en la pantalla principal. */
    fun getLastEvent(context: Context): String =
        prefs(context).getString(KEY_LAST_EVENT, "") ?: ""

    fun setLastEvent(context: Context, event: String) {
        prefs(context).edit().putString(KEY_LAST_EVENT, event).apply()
    }
}
