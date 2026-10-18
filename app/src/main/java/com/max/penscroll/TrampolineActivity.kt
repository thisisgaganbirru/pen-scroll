package com.max.penscroll

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.widget.Toast

/**
 * Invisible activity launched by S Pen Air actions. It asks the accessibility service to
 * swipe and finishes straight away, so the app underneath stays in front.
 */
abstract class TrampolineActivity(private val direction: Direction) : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disableTransitions()

        val service = ScrollAccessibilityService.instance
        when {
            service == null ->
                Toast.makeText(this, R.string.service_off_toast, Toast.LENGTH_LONG).show()
            Prefs.isPaused(this) ->
                Toast.makeText(this, R.string.paused_toast, Toast.LENGTH_SHORT).show()
            else -> service.scroll(direction)
        }
        finish()
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun disableTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}

class ScrollDownActivity : TrampolineActivity(Direction.DOWN)

class ScrollUpActivity : TrampolineActivity(Direction.UP)
