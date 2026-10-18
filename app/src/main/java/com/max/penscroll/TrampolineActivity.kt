package com.max.penscroll

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.widget.Toast

/**
 * Invisible activity launched by S Pen Air actions. It asks the accessibility service to
 * perform its action and finishes straight away, so the app underneath stays in front.
 */
abstract class TrampolineActivity : Activity() {

    /** Asks [service] to do this entry's work; only called when it is running and not paused. */
    protected abstract fun run(service: ScrollAccessibilityService)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disableTransitions()

        val service = ScrollAccessibilityService.instance
        when {
            service == null ->
                Toast.makeText(this, R.string.service_off_toast, Toast.LENGTH_LONG).show()
            Prefs.isPaused(this) ->
                Toast.makeText(this, R.string.paused_toast, Toast.LENGTH_SHORT).show()
            else -> run(service)
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

class ScrollDownActivity : TrampolineActivity() {
    override fun run(service: ScrollAccessibilityService) = service.perform(PenAction.SCROLL_DOWN)
}

class ScrollUpActivity : TrampolineActivity() {
    override fun run(service: ScrollAccessibilityService) = service.perform(PenAction.SCROLL_UP)
}

/** The press-and-hold entry: once scrolls up, twice quickly scrolls down. */
class SmartActivity : TrampolineActivity() {
    override fun run(service: ScrollAccessibilityService) = service.smartPress()
}

/** Target of the fixed per-action shortcuts; the action id travels in the intent extra. */
class PenActionActivity : TrampolineActivity() {
    override fun run(service: ScrollAccessibilityService) {
        PenAction.fromId(intent.getStringExtra(Shortcuts.EXTRA_ACTION))?.let(service::perform)
    }
}
