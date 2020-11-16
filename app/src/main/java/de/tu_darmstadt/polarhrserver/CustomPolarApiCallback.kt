package de.tu_darmstadt.polarhrserver

import android.util.Log
import polar.com.sdk.api.PolarBleApiCallback
import polar.com.sdk.api.model.PolarDeviceInfo
import polar.com.sdk.api.model.PolarHrData
import java.util.*

class CustomPolarApiCallback(
    private val onEcgReady: (identifier: String) -> Unit,
    private val onAccReady: (identifier: String) -> Unit,
    private val onDisconnected: (polarDeviceInfo: PolarDeviceInfo) -> Unit,
    private val onConnected: (polarDeviceInfo: PolarDeviceInfo) -> Unit,
    private val onConnecting: (polarDeviceInfo: PolarDeviceInfo) -> Unit,
    private val onHrNotification: (identifier: String, data: PolarHrData) -> Unit
) : PolarBleApiCallback() {

    override fun blePowerStateChanged(powered: Boolean) {
        Log.d(LOG_TAG, "Power State Changed: $powered")
    }

    override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
        Log.d(LOG_TAG, "Device Connected: ${polarDeviceInfo.name} (${polarDeviceInfo.deviceId})")
        onConnected(polarDeviceInfo)
    }

    override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
        Log.d(LOG_TAG, "Connecting To: ${polarDeviceInfo.name} (${polarDeviceInfo.deviceId})")
        onConnecting(polarDeviceInfo)
    }

    override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
        Log.d(LOG_TAG, "Device Disconnected: ${polarDeviceInfo.name} (${polarDeviceInfo.deviceId})")
        onDisconnected(polarDeviceInfo)
    }

    override fun ecgFeatureReady(identifier: String) {
        Log.d(LOG_TAG, "Ready for ECG")
        onEcgReady(identifier)
    }

    override fun accelerometerFeatureReady(identifier: String) {
        Log.d(LOG_TAG, "Ready for ACC")
        onAccReady(identifier)
    }

    override fun ppgFeatureReady(identifier: String) {
        Log.d(LOG_TAG, "Ready for PPG")
    }

    override fun ppiFeatureReady(identifier: String) {
        Log.d(LOG_TAG, "Ready for PPI")
    }

    override fun biozFeatureReady(identifier: String) {
        Log.d(LOG_TAG, "Ready for BIOZ")
    }

    override fun hrFeatureReady(identifier: String) {
        Log.d(LOG_TAG, "Ready for HR")
    }

    override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
        Log.d(LOG_TAG, "DIS Information received: $value")
    }

    override fun batteryLevelReceived(identifier: String, level: Int) {
        Log.d(LOG_TAG, "Battery Level received: $level")
    }

    override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
        Log.d(LOG_TAG, "HR Notification received: ${data.hr}")
        onHrNotification(identifier, data)
    }

    override fun polarFtpFeatureReady(identifier: String) {
        Log.d(LOG_TAG, "FTP Ready")
    }
}