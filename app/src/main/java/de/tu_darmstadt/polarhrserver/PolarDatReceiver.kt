package de.tu_darmstadt.polarhrserver

import android.content.Context
import android.util.Log
import io.reactivex.rxjava3.disposables.Disposable
import polar.com.sdk.api.PolarBleApi
import polar.com.sdk.api.PolarBleApiCallbackProvider
import polar.com.sdk.api.PolarBleApiDefaultImpl
import polar.com.sdk.api.model.*
import java.lang.Exception
import kotlin.reflect.KFunction0

object PolarDatReceiver {
    private var api: PolarBleApi? = null

    //TODO: figure out settings
    private var ecgSettings: PolarSensorSetting = PolarSensorSetting(
        mapOf(
            PolarSensorSetting.SettingType.SAMPLE_RATE to 100,
            PolarSensorSetting.SettingType.RANGE to 2,
            PolarSensorSetting.SettingType.RESOLUTION to 1
        )
    );
    private var accSettings: PolarSensorSetting = PolarSensorSetting(
        mapOf(
            PolarSensorSetting.SettingType.SAMPLE_RATE to 100,
            PolarSensorSetting.SettingType.RANGE to 2,
            PolarSensorSetting.SettingType.RESOLUTION to 1
        )
    );

    private var ecgSubscription: Disposable? = null
    private var accSubscription: Disposable? = null

    private var onEcgData: ((PolarEcgData) -> Unit)? = null
    private var onAccData: ((PolarAccelerometerData) -> Unit)? = null

    private var onConnected: ((String) -> Unit)? = null
    private var onDisconnected: ((String) -> Unit)? = null
    private var onConnecting: ((String) -> Unit)? = null

    fun init(
        onEcgData: ((PolarEcgData) -> Unit),
        onAccData: ((PolarAccelerometerData) -> Unit),
        onConnected: ((String) -> Unit),
        onDisconnected: ((String) -> Unit)?,
        onConnecting: ((String) -> Unit)
    ) {
        this.onEcgData = onEcgData
        this.onAccData = onAccData
        this.onConnected = onConnected
        this.onDisconnected = onDisconnected
        this.onConnecting = onConnecting
    }

    fun connect(context: Context) {
        api = PolarBleApiDefaultImpl.defaultImplementation(
            context,
            PolarBleApi.FEATURE_HR or PolarBleApi.FEATURE_POLAR_SENSOR_STREAMING
        )
        api?.setApiCallback(
            CustomPolarApiCallback(
                this::onEcgReady,
                this::onAccReady,
                this::onDisconnected,
                this::onConnected,
                this::onConnecting,
                this::onHrReceived
            )
        )
        try {
            api?.setAutomaticReconnection(true)
            Log.d(LOG_TAG, "Connecting to ");
            api?.connectToDevice(DEVICE_ID)
        } catch (e: Exception) {
            Log.e(LOG_TAG, e.toString());
        }

    }

    private fun onEcgReady(identifier: String) = api?.let {
        val ecgFlowable = it.startEcgStreaming(identifier, ecgSettings)
        if (onEcgData != null) {
            ecgSubscription = ecgFlowable.subscribe(
                onEcgData,
                { Log.e(LOG_TAG, "Error while receiving ECG: ${it.message}") }) //TODO: onError
        }
    }

    private fun onAccReady(identifier: String) = api?.let {
        val accFlowable = it.startAccStreaming(identifier, accSettings)
        if (onAccData != null) {
            accSubscription = accFlowable.subscribe(
                onAccData,
                { Log.e(LOG_TAG, "Error while receiving ACC: ${it.message}") }) //TODO: onError
        }
    }

    private fun onHrReceived(identifier: String, data: PolarHrData) {
        Log.d(
            LOG_TAG,
            "HR Data received, HR: ${data.hr}, Connect Status (supported): ${data.contactStatus}(${data.contactStatusSupported}), RRS: ${if (data.rrAvailable) data.rrs else null}"
        )
    }

    private fun onConnected(polarDeviceInfo: PolarDeviceInfo) {
        onConnected?.let { it(polarDeviceInfo.deviceId) };
    }

    private fun onDisconnected(polarDeviceInfo: PolarDeviceInfo) {
        onDisconnected?.let { it(polarDeviceInfo.deviceId) }
    }

    private fun onConnecting(polarDeviceInfo: PolarDeviceInfo) {
        onConnecting?.let { it(polarDeviceInfo.deviceId) }
    }

    fun dispose() {
        accSubscription?.dispose()
        ecgSubscription?.dispose()
        api?.shutDown()
    }

    fun onResume() {
        api?.foregroundEntered()
    }

    fun onPause() {
        api?.backgroundEntered()
    }
}