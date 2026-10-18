package com.max.penscroll

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Quick Settings tile that pauses/resumes scrolling without turning the service off. */
class PauseTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        Prefs.setPaused(this, !Prefs.isPaused(this))
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val running = ScrollAccessibilityService.instance != null
        tile.state = when {
            !running -> Tile.STATE_UNAVAILABLE
            Prefs.isPaused(this) -> Tile.STATE_INACTIVE
            else -> Tile.STATE_ACTIVE
        }
        tile.updateTile()
    }
}
