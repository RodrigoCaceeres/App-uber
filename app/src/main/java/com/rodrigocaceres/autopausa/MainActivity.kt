package com.rodrigocaceres.autopausa

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * Pantalla principal: muestra el estado de los dos permisos que la app
 * necesita y permite activarlos, además del interruptor general.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var enabledSwitch: SwitchMaterial
    private lateinit var notificationStatus: TextView
    private lateinit var accessibilityStatus: TextView
    private lateinit var lastEvent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        enabledSwitch = findViewById(R.id.switch_enabled)
        notificationStatus = findViewById(R.id.text_notification_status)
        accessibilityStatus = findViewById(R.id.text_accessibility_status)
        lastEvent = findViewById(R.id.text_last_event)

        enabledSwitch.setOnCheckedChangeListener { _, checked ->
            Prefs.setEnabled(this, checked)
        }

        findViewById<Button>(R.id.button_notification_access).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.button_accessibility_access).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        enabledSwitch.isChecked = Prefs.isEnabled(this)

        val hasNotificationAccess = NotificationManagerCompat
            .getEnabledListenerPackages(this)
            .contains(packageName)
        notificationStatus.text = getString(
            if (hasNotificationAccess) R.string.status_granted else R.string.status_missing
        )

        accessibilityStatus.text = getString(
            if (AutoPauseAccessibilityService.isRunning()) R.string.status_granted
            else R.string.status_missing
        )

        val event = Prefs.getLastEvent(this)
        lastEvent.text = if (event.isBlank()) getString(R.string.no_events_yet) else event
    }
}
