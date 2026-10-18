package com.max.penscroll

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null

    private lateinit var serviceStatus: TextView
    private lateinit var batteryStatus: TextView
    private lateinit var batteryHint: TextView
    private lateinit var disableBattery: Button
    private lateinit var countdown: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serviceStatus = findViewById(R.id.serviceStatus)
        batteryStatus = findViewById(R.id.batteryStatus)
        batteryHint = findViewById(R.id.batteryHint)
        disableBattery = findViewById(R.id.disableBattery)
        countdown = findViewById(R.id.countdown)

        findViewById<Button>(R.id.openAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        disableBattery.setOnClickListener { requestIgnoreBatteryOptimizations() }

        bindSlider(
            R.id.distanceSeek, R.id.distanceValue, R.string.distance_value, Prefs.KEY_DISTANCE,
            Prefs.MIN_DISTANCE, Prefs.MAX_DISTANCE, Prefs.distancePercent(this), step = 1,
        )
        bindSlider(
            R.id.durationSeek, R.id.durationValue, R.string.duration_value, Prefs.KEY_DURATION,
            Prefs.MIN_DURATION, Prefs.MAX_DURATION, Prefs.durationMs(this), step = 10,
        )
        bindSlider(
            R.id.delaySeek, R.id.delayValue, R.string.delay_value, Prefs.KEY_DELAY,
            Prefs.MIN_DELAY, Prefs.MAX_DELAY, Prefs.delayMs(this), step = 10,
        )

        findViewById<Switch>(R.id.volumeSwitch).apply {
            isChecked = Prefs.volumeMode(this@MainActivity)
            setOnCheckedChangeListener { _, checked ->
                Prefs.get(this@MainActivity).edit()
                    .putBoolean(Prefs.KEY_VOLUME_MODE, checked).apply()
            }
        }

        findViewById<Button>(R.id.testDown).setOnClickListener { startTest(Direction.DOWN) }
        findViewById<Button>(R.id.testUp).setOnClickListener { startTest(Direction.UP) }

        addActionToggles(findViewById<LinearLayout>(R.id.actionsCard))
        Shortcuts.clearDynamic(this)

        addSetupSteps(findViewById<LinearLayout>(R.id.setupCard))
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    override fun onDestroy() {
        // The countdown keeps running while you switch to Instagram (onStop), but not past destroy.
        countdownRunnable?.let(handler::removeCallbacks)
        super.onDestroy()
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

    /** SeekBar on API 26 has no reliable min, so progress is an offset from [min] in [step]s. */
    private fun bindSlider(
        seekId: Int, valueId: Int, valueRes: Int, key: String,
        min: Int, max: Int, current: Int, step: Int,
    ) {
        val label = findViewById<TextView>(valueId)
        val seek = findViewById<SeekBar>(seekId)
        seek.max = (max - min) / step
        seek.progress = (current - min) / step
        label.text = getString(valueRes, current)

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                val value = min + progress * step
                label.text = getString(valueRes, value)
                if (fromUser) Prefs.get(this@MainActivity).edit().putInt(key, value).apply()
            }

            override fun onStartTrackingTouch(bar: SeekBar) = Unit
            override fun onStopTrackingTouch(bar: SeekBar) = Unit
        })
    }

    private fun startTest(direction: Direction) {
        if (ScrollAccessibilityService.instance == null) {
            Toast.makeText(this, R.string.service_off_toast, Toast.LENGTH_LONG).show()
            return
        }
        countdownRunnable?.let(handler::removeCallbacks)
        val name = if (direction == Direction.DOWN) "down" else "up"
        var remaining = 3
        countdown.visibility = View.VISIBLE

        val tick = object : Runnable {
            override fun run() {
                if (remaining > 0) {
                    countdown.text = getString(R.string.countdown, name, remaining)
                    remaining--
                    handler.postDelayed(this, 1000)
                } else {
                    // The countdown already gave you time to switch apps, so no extra delay.
                    ScrollAccessibilityService.instance?.scroll(direction, delayMs = 0L)
                    countdown.setText(R.string.test_done)
                    countdownRunnable = null
                }
            }
        }
        countdownRunnable = tick
        handler.post(tick)
    }
}
