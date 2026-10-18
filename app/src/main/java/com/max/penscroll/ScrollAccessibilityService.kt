package com.max.penscroll

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

class ScrollAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "PenScroll"
        private const val INSTAGRAM = "com.instagram.android"

        // Windows that pop over the real foreground app and shouldn't count as "foreground".
        private val IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "com.samsung.android.honeyboard",
            "com.samsung.android.service.aircommand",
        )

        @Volatile
        var instance: ScrollAccessibilityService? = null
            private set
    }

    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var foregroundPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Service connected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        handler.removeCallbacksAndMessages(null)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg in IGNORED_PACKAGES) return
        foregroundPackage = pkg
    }

    override fun onInterrupt() = Unit

    /**
     * Swipe once in [direction]. The default delay gives the trampoline activity time to
     * finish so the app underneath (e.g. Instagram) is in front when the finger lands.
     */
    fun scroll(direction: Direction, delayMs: Long = Prefs.delayMs(this).toLong()) {
        if (Prefs.isPaused(this)) return
        handler.postDelayed({ dispatchSwipe(direction) }, delayMs)
    }

    private fun dispatchSwipe(direction: Direction) {
        val (width, height) = screenSize()
        val half = Prefs.distancePercent(this) / 200f
        val x = width * 0.5f
        val low = height * (0.5f + half) // nearer the bottom
        val high = height * (0.5f - half) // nearer the top

        val path = Path().apply {
            when (direction) {
                Direction.DOWN -> { moveTo(x, low); lineTo(x, high) }
                Direction.UP -> { moveTo(x, high); lineTo(x, low) }
            }
        }
        val stroke = GestureDescription.StrokeDescription(
            path, 0L, Prefs.durationMs(this).toLong()
        )
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val accepted = dispatchGesture(gesture, null, null)
        if (!accepted) Log.w(TAG, "dispatchGesture was rejected")
    }

    private fun screenSize(): Pair<Int, Int> {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels to metrics.heightPixels
        }
    }

    /** Optional fallback: volume keys scroll Instagram when the toggle is on. */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!Prefs.volumeMode(this) || Prefs.isPaused(this)) return false
        if (foregroundPackage != INSTAGRAM) return false

        val direction = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> Direction.DOWN
            KeyEvent.KEYCODE_VOLUME_UP -> Direction.UP
            else -> return false
        }
        // Consume both down and up so the volume doesn't change; swipe once per press.
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            scroll(direction, delayMs = 0L)
        }
        return true
    }
}
