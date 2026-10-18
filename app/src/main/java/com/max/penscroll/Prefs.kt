package com.max.penscroll

import android.content.Context
import android.content.SharedPreferences

/** All tunable values, so they can be changed from MainActivity without rebuilding. */
object Prefs {
    private const val NAME = "pen_scroll"

    const val KEY_DISTANCE = "distance_percent"
    const val KEY_DURATION = "duration_ms"
    const val KEY_DELAY = "delay_ms"
    const val KEY_PAUSED = "paused"
    const val KEY_VOLUME_MODE = "volume_mode"

    const val DEFAULT_DISTANCE = 50
    const val DEFAULT_DURATION = 200
    const val DEFAULT_DELAY = 200

    const val MIN_DISTANCE = 30
    const val MAX_DISTANCE = 80
    const val MIN_DURATION = 100
    const val MAX_DURATION = 600
    const val MIN_DELAY = 50
    const val MAX_DELAY = 500

    fun get(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun distancePercent(context: Context) =
        get(context).getInt(KEY_DISTANCE, DEFAULT_DISTANCE).coerceIn(MIN_DISTANCE, MAX_DISTANCE)

    fun durationMs(context: Context) =
        get(context).getInt(KEY_DURATION, DEFAULT_DURATION).coerceIn(MIN_DURATION, MAX_DURATION)

    fun delayMs(context: Context) =
        get(context).getInt(KEY_DELAY, DEFAULT_DELAY).coerceIn(MIN_DELAY, MAX_DELAY)

    fun isPaused(context: Context) = get(context).getBoolean(KEY_PAUSED, false)

    fun setPaused(context: Context, paused: Boolean) =
        get(context).edit().putBoolean(KEY_PAUSED, paused).apply()

    fun volumeMode(context: Context) = get(context).getBoolean(KEY_VOLUME_MODE, false)

    private const val KEY_DEFAULT_ACTION = "default_action"
    private const val KEY_MAPPED_APPS = "mapped_apps"

    /** What "Pen Scroll Smart" does in any app that has no mapping of its own. */
    fun defaultAction(context: Context): PenAction =
        PenAction.fromId(get(context).getString(KEY_DEFAULT_ACTION, null)) ?: PenAction.SCROLL_DOWN

    fun setDefaultAction(context: Context, action: PenAction) =
        get(context).edit().putString(KEY_DEFAULT_ACTION, action.id).apply()

    fun mappedApps(context: Context): Set<String> =
        get(context).getStringSet(KEY_MAPPED_APPS, emptySet()) ?: emptySet()

    fun appAction(context: Context, packageName: String): PenAction? =
        PenAction.fromId(get(context).getString("app_$packageName", null))

    fun setAppAction(context: Context, packageName: String, action: PenAction) {
        get(context).edit()
            .putString("app_$packageName", action.id)
            .putStringSet(KEY_MAPPED_APPS, mappedApps(context) + packageName)
            .apply()
    }

    fun removeApp(context: Context, packageName: String) {
        get(context).edit()
            .remove("app_$packageName")
            .putStringSet(KEY_MAPPED_APPS, mappedApps(context) - packageName)
            .apply()
    }

    /** The action for the app in front, or the default when it is unknown or unmapped. */
    fun actionFor(context: Context, packageName: String?): PenAction =
        packageName?.let { appAction(context, it) } ?: defaultAction(context)
}
