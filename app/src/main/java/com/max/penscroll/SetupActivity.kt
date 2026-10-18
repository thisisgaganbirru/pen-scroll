package com.max.penscroll

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class SetupActivity : Activity() {

    private lateinit var serviceStatus: TextView
    private lateinit var batteryStatus: TextView
    private lateinit var batteryHint: TextView
    private lateinit var disableBattery: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serviceStatus = findViewById(R.id.serviceStatus)
        batteryStatus = findViewById(R.id.batteryStatus)
        batteryHint = findViewById(R.id.batteryHint)
        disableBattery = findViewById(R.id.disableBattery)

        findViewById<Button>(R.id.openAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        disableBattery.setOnClickListener { requestIgnoreBatteryOptimizations() }

        findViewById<View>(R.id.tuningRow).setOnClickListener {
            startActivity(Intent(this, TuningActivity::class.java))
        }

        Shortcuts.clearDynamic(this)

        addSetupSteps(findViewById<LinearLayout>(R.id.setupCard))
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        when {
            // "Enabled" in Settings isn't enough; swipes only work once the service is connected.
            !isServiceConnected() -> serviceStatus.setPill(R.string.pill_off, R.color.bad)
            Prefs.isPaused(this) -> serviceStatus.setPill(R.string.pill_paused, R.color.warn)
            else -> serviceStatus.setPill(R.string.pill_on, R.color.ok)
        }

        val pm = getSystemService(PowerManager::class.java)
        val ignoring = pm.isIgnoringBatteryOptimizations(packageName)
        if (ignoring) {
            batteryStatus.setPill(R.string.pill_unrestricted, R.color.ok)
        } else {
            batteryStatus.setPill(R.string.pill_optimized, R.color.warn)
        }
        // Nothing left to fix once it's unrestricted, so hide the hint and button.
        val batteryVisibility = if (ignoring) View.GONE else View.VISIBLE
        batteryHint.visibility = batteryVisibility
        disableBattery.visibility = batteryVisibility
    }

    private fun isServiceConnected(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val mine = ComponentName(this, ScrollAccessibilityService::class.java)
        val listed = enabled.split(':').any { ComponentName.unflattenFromString(it) == mine }
        return listed && ScrollAccessibilityService.instance != null
    }

    @SuppressLint("BatteryLife")
    private fun requestIgnoreBatteryOptimizations() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:$packageName"))
        try {
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
}
