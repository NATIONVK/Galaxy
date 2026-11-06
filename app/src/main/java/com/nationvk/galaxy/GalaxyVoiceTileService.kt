package com.nationvk.galaxy

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat

class GalaxyVoiceTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val stop = Intent(this, GalaxyVoiceWakeService::class.java).setAction(GalaxyVoiceWakeService.ACTION_STOP)
        val start = Intent(this, GalaxyVoiceWakeService::class.java)
        if (qsTile?.state == Tile.STATE_ACTIVE) {
            startService(stop)
            qsTile?.state = Tile.STATE_INACTIVE
            qsTile?.label = "Galaxy Voice"
        } else {
            try {
                ContextCompat.startForegroundService(this, start)
                qsTile?.state = Tile.STATE_ACTIVE
                qsTile?.label = "Galaxy Listening"
            } catch (_: Exception) {
                qsTile?.state = Tile.STATE_INACTIVE
                qsTile?.label = "Open Galaxy to enable"
            }
        }
        qsTile?.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.label = "Galaxy Voice"
        qsTile?.state = Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }
}
