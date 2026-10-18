package com.max.penscroll

/** Everything a pen shortcut can do. [id] is stable: it is the shortcut id and the pref key. */
enum class PenAction(val id: String, val label: Int) {
    SCROLL_DOWN("scroll_down", R.string.scroll_down),
    SCROLL_UP("scroll_up", R.string.scroll_up),
    SWIPE_LEFT("swipe_left", R.string.swipe_left),
    SWIPE_RIGHT("swipe_right", R.string.swipe_right),
    DOUBLE_TAP("double_tap", R.string.double_tap),
    BACK("back", R.string.action_back),
    HOME("home", R.string.action_home),
    RECENTS("recents", R.string.action_recents),
    NOTIFICATIONS("notifications", R.string.action_notifications),
    QUICK_SETTINGS("quick_settings", R.string.action_quick_settings),
    SCREENSHOT("screenshot", R.string.action_screenshot),
    LOCK_SCREEN("lock_screen", R.string.action_lock_screen);

    /** The two scroll actions are static shortcuts (res/xml/shortcuts.xml); the rest are dynamic. */
    val isStatic: Boolean get() = this == SCROLL_DOWN || this == SCROLL_UP

    companion object {
        fun fromId(id: String?): PenAction? = entries.firstOrNull { it.id == id }
    }
}
