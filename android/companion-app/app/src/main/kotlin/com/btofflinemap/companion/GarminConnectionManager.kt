package com.btofflinemap.companion

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import com.garmin.android.connectiq.exception.InvalidStateException
import com.garmin.android.connectiq.exception.ServiceUnavailableException

/**
 * Phase 1: HELLO / HELLO_ACK / GPS_UPDATE only. Wire format: shared/protocol/protocol.md.
 *
 * The watch never talks BLE to us directly — it talks to the Garmin Connect Mobile (GCM)
 * app, and this class talks to GCM through the Connect IQ Mobile SDK
 * (com.garmin.android.connectiq, vendored as app/libs/connectiq-mobile-sdk-android-1.5.aar).
 * GCM must be installed and the watch paired through it for any of this to work.
 */
class GarminConnectionManager(private val context: Context) {

    companion object {
        // Must match garmin/fr255-map-app/manifest.xml's iq:application id — see
        // shared/protocol/protocol.md "Application identity".
        private const val WATCH_APP_ID = "c68fb880-5658-43be-8abf-14eb037cab7a"
    }

    private val connectIQ: ConnectIQ = ConnectIQ.getInstance(context, ConnectIQ.IQConnectType.WIRELESS)
    private val watchApp = IQApp(WATCH_APP_ID)

    private var connectedDevice: IQDevice? = null

    private val _status = mutableStateOf("initializing")
    val status: State<String> get() = _status

    private val _lastGps = mutableStateOf<GpsUpdate?>(null)
    val lastGps: State<GpsUpdate?> get() = _lastGps

    fun start() {
        connectIQ.initialize(
            context,
            false,
            object : ConnectIQ.ConnectIQListener {
                override fun onSdkReady() {
                    _status.value = "SDK ready, looking for a paired device"
                    findDevice()
                }

                override fun onInitializeError(errStatus: ConnectIQ.IQSdkErrorStatus) {
                    _status.value = "init error: $errStatus"
                }

                override fun onSdkShutDown() {
                    _status.value = "SDK shut down"
                }
            },
        )
    }

    private fun findDevice() {
        val devices = try {
            connectIQ.knownDevices
        } catch (e: InvalidStateException) {
            _status.value = "SDK not ready (${e.message})"
            return
        } catch (e: ServiceUnavailableException) {
            _status.value = "Garmin Connect Mobile unavailable"
            return
        }

        val device = devices?.firstOrNull()
        if (device == null) {
            _status.value = "no paired Garmin device found"
            return
        }

        connectedDevice = device
        _status.value = "found ${device.friendlyName}, waiting for connection"

        try {
            connectIQ.registerForDeviceEvents(device) { _, deviceStatus ->
                when (deviceStatus) {
                    IQDevice.IQDeviceStatus.CONNECTED -> {
                        _status.value = "connected: ${device.friendlyName}"
                        registerForAppMessages(device)
                    }
                    else -> _status.value = "${device.friendlyName}: $deviceStatus"
                }
            }
        } catch (e: InvalidStateException) {
            _status.value = "device event registration failed (${e.message})"
        }
    }

    private fun registerForAppMessages(device: IQDevice) {
        try {
            connectIQ.registerForAppEvents(device, watchApp) { _, _, message, _ ->
                handleMessage(device, message)
            }
        } catch (e: InvalidStateException) {
            _status.value = "app event registration failed (${e.message})"
        }
    }

    private fun handleMessage(device: IQDevice, message: List<Any>?) {
        val payload = message?.firstOrNull() as? Map<*, *> ?: return
        when (payload["type"]) {
            "HELLO" -> sendHelloAck(device)
            "GPS_UPDATE" -> {
                val lat = (payload["lat"] as? Number)?.toDouble() ?: return
                val lon = (payload["lon"] as? Number)?.toDouble() ?: return
                val alt = (payload["alt"] as? Number)?.toDouble()
                val seq = (payload["seq"] as? Number)?.toLong() ?: 0L
                _lastGps.value = GpsUpdate(lat = lat, lon = lon, alt = alt, seq = seq)
            }
        }
    }

    private fun sendHelloAck(device: IQDevice) {
        val ack = mapOf("v" to 1, "type" to "HELLO_ACK", "seq" to 0)
        try {
            connectIQ.sendMessage(device, watchApp, ack) { _, _, _ -> }
        } catch (e: InvalidStateException) {
            _status.value = "HELLO_ACK send failed (${e.message})"
        } catch (e: ServiceUnavailableException) {
            _status.value = "Garmin Connect Mobile unavailable"
        }
    }
}

data class GpsUpdate(val lat: Double, val lon: Double, val alt: Double?, val seq: Long)
