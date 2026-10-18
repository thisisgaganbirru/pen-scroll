package com.max.penscroll

import android.app.Activity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

/** Swipe sliders and the volume-key switch, opened from the Tuning row on the setup screen. */
class TuningActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tuning)

        findViewById<ImageButton>(R.id.back).setOnClickListener { finish() }

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
            isChecked = Prefs.volumeMode(this@TuningActivity)
            setOnCheckedChangeListener { _, checked ->
                Prefs.get(this@TuningActivity).edit()
                    .putBoolean(Prefs.KEY_VOLUME_MODE, checked).apply()
            }
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
                if (fromUser) Prefs.get(this@TuningActivity).edit().putInt(key, value).apply()
            }

            override fun onStartTrackingTouch(bar: SeekBar) = Unit
            override fun onStopTrackingTouch(bar: SeekBar) = Unit
        })
    }
}
