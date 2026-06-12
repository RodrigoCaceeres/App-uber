package com.rodrigocaceres.autopausa

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat

/**
 * Servicio de accesibilidad que automatiza el toque del botón
 * "pausar / detener nuevas solicitudes" en Uber o DiDi.
 *
 * Flujo:
 *  1. [TripNotificationListener] detecta un viaje y llama a [requestPause].
 *  2. Este servicio abre la app objetivo en primer plano.
 *  3. Cuando llega contenido de pantalla de esa app, busca un botón cuyo texto
 *     coincida con las palabras de pausa y lo pulsa.
 *  4. Si aparece un diálogo de confirmación, lo confirma.
 *  5. Vuelve a abrir la app del viaje para no estorbar al conductor.
 *
 * Si en 25 segundos no lo consigue (la app cambió sus pantallas, por ejemplo),
 * avisa con una notificación para que el conductor pause manualmente.
 */
class AutoPauseAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    /** App que estamos intentando pausar en este momento (null = inactivo). */
    private var pendingTarget: RideApp? = null

    /** Tras pulsar pausa, esperamos un posible diálogo de confirmación. */
    private var awaitingConfirmation = false

    private val timeoutRunnable = Runnable { onPauseTimeout() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Servicio de accesibilidad conectado")
    }

    override fun onDestroy() {
        if (instance == this) instance = null
        handler.removeCallbacks(timeoutRunnable)
        super.onDestroy()
    }

    override fun onInterrupt() = Unit

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val target = pendingTarget ?: return
        if (event.packageName?.toString() != target.packageName) return

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != target.packageName) return

        val texts = if (awaitingConfirmation) target.confirmButtonTexts else target.pauseButtonTexts
        val button = findClickableByText(root, texts) ?: run {
            // Aún no aparece el diálogo de confirmación: damos por hecha la pausa
            // cuando se agote el plazo corto programado al pulsar el botón.
            return
        }

        val clicked = button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        Log.i(TAG, "Click en '${button.text ?: button.contentDescription}' (confirmación=$awaitingConfirmation): $clicked")

        if (!clicked) return

        if (awaitingConfirmation) {
            onPauseSucceeded(target)
        } else {
            // Esperamos brevemente por un diálogo de confirmación; si no aparece,
            // consideramos la pausa completada.
            awaitingConfirmation = true
            handler.removeCallbacks(timeoutRunnable)
            handler.postDelayed({ pendingTarget?.let { onPauseSucceeded(it) } }, CONFIRMATION_WINDOW_MS)
        }
    }

    private fun startPauseFlow(target: RideApp) {
        pendingTarget = target
        awaitingConfirmation = false
        handler.removeCallbacks(timeoutRunnable)
        handler.postDelayed(timeoutRunnable, PAUSE_TIMEOUT_MS)

        val launch = packageManager.getLaunchIntentForPackage(target.packageName)
        if (launch == null) {
            Log.w(TAG, "${target.displayName} no está instalada")
            pendingTarget = null
            handler.removeCallbacks(timeoutRunnable)
            return
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launch)
        Log.i(TAG, "Abriendo ${target.displayName} para pausar solicitudes")
    }

    private fun onPauseSucceeded(target: RideApp) {
        handler.removeCallbacks(timeoutRunnable)
        pendingTarget = null
        awaitingConfirmation = false

        Prefs.setLastEvent(this, "${target.displayName} pausada automáticamente ✔")
        notify(
            getString(R.string.notif_paused_title, target.displayName),
            getString(R.string.notif_paused_text, target.other().displayName)
        )

        // Devolvemos al frente la app donde está el viaje activo.
        Prefs.getActiveTripApp(this)?.let { tripApp ->
            packageManager.getLaunchIntentForPackage(tripApp.packageName)?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(it)
            }
        }
    }

    private fun onPauseTimeout() {
        val target = pendingTarget ?: return
        pendingTarget = null
        awaitingConfirmation = false

        Prefs.setLastEvent(this, "No se pudo pausar ${target.displayName} automáticamente")
        Log.w(TAG, "Tiempo agotado intentando pausar ${target.displayName}")
        notify(
            getString(R.string.notif_failed_title, target.displayName),
            getString(R.string.notif_failed_text)
        )
    }

    /**
     * Busca en el árbol de la pantalla un nodo clicable cuyo texto o descripción
     * contenga alguna de las palabras dadas. Si el nodo con el texto no es
     * clicable, sube por sus padres hasta encontrar uno que sí lo sea.
     */
    private fun findClickableByText(root: AccessibilityNodeInfo, texts: List<String>): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val label = buildString {
                node.text?.let { append(it) }
                append(' ')
                node.contentDescription?.let { append(it) }
            }.lowercase()

            if (texts.any { label.contains(it) }) {
                var candidate: AccessibilityNodeInfo? = node
                while (candidate != null && !candidate.isClickable) {
                    candidate = candidate.parent
                }
                if (candidate != null) return candidate
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun notify(title: String, text: String) {
        notifyUser(this, title, text)
    }

    companion object {
        private const val TAG = "AutoPausa/Accesibilidad"
        private const val PAUSE_TIMEOUT_MS = 25_000L
        private const val CONFIRMATION_WINDOW_MS = 4_000L
        private const val CHANNEL_ID = "autopausa_eventos"

        private var instance: AutoPauseAccessibilityService? = null

        fun isRunning(): Boolean = instance != null

        /** Pide pausar las solicitudes en [target]. Si el servicio no está activo, avisa al usuario. */
        fun requestPause(context: Context, target: RideApp) {
            val service = instance
            if (service != null) {
                service.handler.post { service.startPauseFlow(target) }
            } else {
                notifyUser(
                    context,
                    context.getString(R.string.notif_service_off_title),
                    context.getString(R.string.notif_service_off_text, target.displayName)
                )
            }
        }

        /** Recuerda al conductor reactivar la app pausada cuando termina el viaje. */
        fun notifyTripEnded(context: Context, pausedApp: RideApp) {
            notifyUser(
                context,
                context.getString(R.string.notif_trip_ended_title),
                context.getString(R.string.notif_trip_ended_text, pausedApp.displayName)
            )
        }

        private fun notifyUser(context: Context, title: String, text: String) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )

            val contentIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_pause)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

            manager.notify(System.currentTimeMillis().toInt(), notification)
        }
    }
}
