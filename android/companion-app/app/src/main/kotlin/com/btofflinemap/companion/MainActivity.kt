package com.btofflinemap.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Phase 1: shows the Garmin link status and the last GPS_UPDATE received. No map/activity
 * UI yet — those arrive in later phases (docs/PROJECT_PLAN.md §39).
 */
class MainActivity : ComponentActivity() {

    private lateinit var garminConnectionManager: GarminConnectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        garminConnectionManager = GarminConnectionManager(applicationContext)
        garminConnectionManager.start()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GarminLinkScreen(
                        status = garminConnectionManager.status.value,
                        lastGps = garminConnectionManager.lastGps.value,
                    )
                }
            }
        }
    }
}

@Composable
private fun GarminLinkScreen(status: String, lastGps: GpsUpdate?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Garmin link: $status")
        if (lastGps != null) {
            Text("lat: ${lastGps.lat}")
            Text("lon: ${lastGps.lon}")
            Text("alt: ${lastGps.alt ?: "n/a"}")
            Text("seq: ${lastGps.seq}")
        } else {
            Text("no GPS fix received yet")
        }
    }
}
