package com.max.penscroll

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.util.Log

/** Publishes the enabled non-scroll actions as dynamic shortcuts, which Air actions can pick. */
object Shortcuts {
    private const val TAG = "PenScroll"
    const val EXTRA_ACTION = "pen_action"

    fun publish(context: Context) {
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return
        val icon = Icon.createWithResource(context, R.drawable.ic_action)

        val shortcuts = PenAction.entries
            .filter { !it.isStatic && Prefs.isActionEnabled(context, it) }
            .map { action ->
                val label = context.getString(action.label)
                ShortcutInfo.Builder(context, action.id)
                    .setShortLabel(label)
                    .setLongLabel(label)
                    .setIcon(icon)
                    .setIntent(
                        Intent(context, PenActionActivity::class.java)
                            .setAction(Intent.ACTION_VIEW)
                            .putExtra(EXTRA_ACTION, action.id)
                    )
                    .build()
            }
        try {
            manager.setDynamicShortcuts(shortcuts)
        } catch (e: Exception) {
            Log.w(TAG, "Could not publish shortcuts", e)
        }
    }
}
