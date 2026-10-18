package com.max.penscroll

import android.content.Context
import android.content.pm.ShortcutManager
import android.util.Log

/**
 * All shortcuts are static (res/xml/shortcuts.xml): Samsung's Air actions picker lists those
 * but not dynamic ones. The intent extra names which [PenAction] [PenActionActivity] runs.
 */
object Shortcuts {
    private const val TAG = "PenScroll"
    const val EXTRA_ACTION = "pen_action"

    /** Earlier builds published dynamic shortcuts; their ids would clash with the static ones. */
    fun clearDynamic(context: Context) {
        try {
            context.getSystemService(ShortcutManager::class.java)?.removeAllDynamicShortcuts()
        } catch (e: Exception) {
            Log.w(TAG, "Could not clear dynamic shortcuts", e)
        }
    }
}
