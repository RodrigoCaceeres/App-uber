package com.rodrigocaceres.autopausa

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Escucha las notificaciones del sistema y detecta cuándo empieza o termina
 * un viaje en Uber o DiDi, según las palabras clave de la notificación
 * persistente que cada app muestra durante un viaje.
 *
 * Cuando detecta un viaje activo en una app, le pide al servicio de
 * accesibilidad que pause las solicitudes en la otra.
 */
class TripNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val app = RideApp.fromPackage(sbn.packageName) ?: return
        if (!Prefs.isEnabled(this)) return

        val text = extractText(sbn)
        Log.d(TAG, "Notificación de ${app.displayName}: $text")

        val looksLikeTrip = app.tripKeywords.any { text.contains(it) }
        if (!looksLikeTrip) return

        val current = Prefs.getActiveTripApp(this)
        if (current == app) return // ya sabíamos que está en viaje

        Prefs.setActiveTripApp(this, app)
        Prefs.setLastEvent(this, "Viaje detectado en ${app.displayName}; pausando ${app.other().displayName}…")
        Log.i(TAG, "Viaje activo en ${app.displayName}: solicitando pausa de ${app.other().displayName}")

        AutoPauseAccessibilityService.requestPause(this, target = app.other())
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val app = RideApp.fromPackage(sbn.packageName) ?: return

        // Si desaparece la notificación de viaje de la app activa, asumimos que
        // el viaje terminó. Solo se notifica al conductor; volver a conectarse
        // queda como decisión manual (es más seguro que reconectar solo).
        if (Prefs.getActiveTripApp(this) == app) {
            val text = extractText(sbn)
            val wasTripNotification = app.tripKeywords.any { text.contains(it) }
            if (wasTripNotification) {
                Prefs.setActiveTripApp(this, null)
                Prefs.setLastEvent(this, "Viaje en ${app.displayName} terminado. Recuerda reactivar ${app.other().displayName}.")
                Log.i(TAG, "Viaje en ${app.displayName} terminado")
                AutoPauseAccessibilityService.notifyTripEnded(this, pausedApp = app.other())
            }
        }
    }

    /** Junta título y texto de la notificación en minúsculas para buscar palabras clave. */
    private fun extractText(sbn: StatusBarNotification): String {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        return "$title $text $big".lowercase()
    }

    companion object {
        private const val TAG = "AutoPausa/Listener"
    }
}
