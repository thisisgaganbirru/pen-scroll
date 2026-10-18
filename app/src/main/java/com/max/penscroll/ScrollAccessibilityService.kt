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

        private const val TAP_MS = 40L
        private const val TAP_GAP_MS = 80L

        private val TRAMPOLINE_CLASSES = setOf(
            ScrollDownActivity::class.java.name,
            ScrollUpActivity::class.java.name,
            PenActionActivity::class.java.name,
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
        if (pkg in IGNORED_PACKAGES) return
        // Our own trampolines flash in front of the real app, so they must not count; any
        // other Pen Scroll window (the setup screen) does, so volume keys stop scrolling.
        if (pkg == packageName && event.className?.toString().orEmpty() in TRAMPOLINE_CLASSES) return
        foregroundPackage = pkg
    }

    override fun onInterrupt() = Unit

    /** Scroll once in [direction]; see [perform] for the delay. */
    fun scroll(direction: Direction, delayMs: Long = Prefs.delayMs(this).toLong()) =
        perform(if (direction == Direction.DOWN) PenAction.SCROLL_DOWN else PenAction.SCROLL_UP, delayMs)

    /**
     * Run [action] after [delayMs]. The default delay gives the trampoline activity time to
     * finish so the app underneath (e.g. Instagram) is in front when the action lands.
     */
    fun perform(action: PenAction, delayMs: Long = Prefs.delayMs(this).toLong()) {
        if (Prefs.isPaused(this)) return
        handler.postDelayed({ execute(action) }, delayMs)
    }

    private fun execute(action: PenAction) {
        // Re-check: the user may have paused while this action was waiting out its delay.
        if (Prefs.isPaused(this)) return
        when (action) {
            PenAction.SCROLL_DOWN -> swipeVertical(Direction.DOWN)
            PenAction.SCROLL_UP -> swipeVertical(Direction.UP)
            PenAction.SWIPE_LEFT -> swipeHorizontal(toLeft = true)
            PenAction.SWIPE_RIGHT -> swipeHorizontal(toLeft = false)
            PenAction.DOUBLE_TAP -> doubleTap()
            PenAction.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            PenAction.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            PenAction.RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            PenAction.NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            PenAction.QUICK_SETTINGS -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            PenAction.SCREENSHOT ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                }
            PenAction.LOCK_SCREEN ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                }
        }
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

    /** [toLeft]: the finger moves right to left, which is "next" in Stories and carousels. */
    private fun swipeHorizontal(toLeft: Boolean) {
        val (width, height) = screenSize()
        val half = Prefs.distancePercent(this) / 200f
        val y = height * 0.5f
        val right = width * (0.5f + half)
        val left = width * (0.5f - half)

        val path = Path().apply {
            if (toLeft) { moveTo(right, y); lineTo(left, y) } else { moveTo(left, y); lineTo(right, y) }
        }
        dispatch(GestureDescription.StrokeDescription(path, 0L, Prefs.durationMs(this).toLong()))
    }

    /** Two quick taps in the middle of the screen, which likes a reel in Instagram. */
    private fun doubleTap() {
        val (width, height) = screenSize()
        fun tap(startMs: Long) = GestureDescription.StrokeDescription(
            Path().apply { moveTo(width * 0.5f, height * 0.5f) }, startMs, TAP_MS
        )
        dispatch(tap(0L), tap(TAP_MS + TAP_GAP_MS))
    }

    private fun dispatch(vararg strokes: GestureDescription.StrokeDescription) {
        val builder = GestureDescription.Builder()
        strokes.forEach(builder::addStroke)
        val accepted = dispatchGesture(builder.build(), null, null)
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
