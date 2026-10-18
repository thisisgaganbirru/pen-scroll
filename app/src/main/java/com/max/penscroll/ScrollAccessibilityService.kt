package com.max.penscroll

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

class ScrollAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "PenScroll"
        private const val INSTAGRAM = "com.instagram.android"
        const val AIR_COMMAND = "com.samsung.android.service.aircommand"

        // Windows that pop over the real foreground app and shouldn't count as "foreground".
        private val IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "com.samsung.android.honeyboard",
            AIR_COMMAND,
        )

        // Samsung needs ~0.8 s of pressing per hold, so two quick holds start 1.1-1.3 s apart.
        private const val MULTI_PRESS_WINDOW_MS = 2000L
        private const val SETTLE_MS = 300L

        private val TRAMPOLINE_CLASS = LaunchActivity::class.java.name

        @Volatile
        var instance: ScrollAccessibilityService? = null
            private set
    }

    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var foregroundPackage: String? = null

    // When the last single press-and-hold arrived, so a quick second one can turn it around.
    private var lastHoldAt = 0L

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
        if (pkg in IGNORED_PACKAGES) return
        // Our trampoline flashes in front of the real app, so it must not count; any other
        // Pen Scroll window (the setup screen) does, so volume keys stop scrolling.
        if (pkg == packageName && event.className?.toString() == TRAMPOLINE_CLASS) return
        foregroundPackage = pkg
    }

    override fun onInterrupt() = Unit

    /**
     * One press-and-hold scrolls down (next reel) straight away. A second within
     * [MULTI_PRESS_WINDOW_MS] scrolls up twice: once to undo the first, once more to go back.
     * Pen Scroll only sees these trampoline launches, never the pen clicks themselves.
     */
    fun smartPress() {
        if (Prefs.isPaused(this)) return
        val now = SystemClock.uptimeMillis()
        if (now - lastHoldAt <= MULTI_PRESS_WINDOW_MS) {
            lastHoldAt = 0L
            val delay = Prefs.delayMs(this).toLong()
            scroll(Direction.UP, delay)
            // Let the first swipe finish and the feed settle before the second.
            scroll(Direction.UP, delay + Prefs.durationMs(this) + SETTLE_MS)
        } else {
            lastHoldAt = now
            scroll(Direction.DOWN)
        }
    }

    /**
     * Scroll once in [direction] after [delayMs]. The default delay gives the trampoline time to
     * finish so the app underneath (e.g. Instagram) is in front when the swipe lands.
     */
    fun scroll(direction: Direction, delayMs: Long = Prefs.delayMs(this).toLong()) {
        if (Prefs.isPaused(this)) return
        handler.postDelayed({
            // Re-check: the user may have paused while this swipe was waiting out its delay.
            if (!Prefs.isPaused(this)) swipeVertical(direction)
        }, delayMs)
    }

    private fun swipeVertical(direction: Direction) {
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
        dispatch(GestureDescription.StrokeDescription(path, 0L, Prefs.durationMs(this).toLong()))
    }

    private fun dispatch(stroke: GestureDescription.StrokeDescription) {
        val accepted = dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
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
