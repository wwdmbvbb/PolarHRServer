package de.tu_darmstadt.polarhrserver

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.core.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.*
import polar.com.sdk.api.model.PolarAccelerometerData
import polar.com.sdk.api.model.PolarEcgData
import polar.com.sdk.api.model.PolarHrData
import java.net.InetSocketAddress


class MainActivity : AppCompatActivity() {

    @KtorExperimentalAPI
    var dataTransfer: DataTransfer? = null

    @KtorExperimentalAPI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), 1
            )
        }

        PolarDatReceiver.init(
            this::onEcgData,
            this::onAccData,
            this::onConnected,
            this::onDisconnected,
            this::onConnecting,
            this::onHrData
        )

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
    }

    fun onConnectClicked(view: View) {
        PolarDatReceiver.connect(this)
    }

    @KtorExperimentalAPI
    fun onSendDataClicked(view: View) = GlobalScope.launch {
        try {
            val ip = et_server.text.toString()
            val port = et_server_port.text.toString().toInt()

            if (dataTransfer == null) {
                dataTransfer = DataTransfer(ip, port)
                runOnUiThread {
                    btn_send_data.text = getString(R.string.stop_sending_data)
                }
            } else {
                dataTransfer?.dispose()
                dataTransfer = null
                runOnUiThread {
                    btn_send_data.text = getString(R.string.start_sending_data)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun onConnected(id: String) {
        tv_status.text = getString(R.string.status, getString(R.string.connected))
        btn_connect.text = getString(R.string.disconnect)
        btn_connect.isEnabled = true
    }

    private fun onDisconnected(id: String) {
        tv_status.text = getString(R.string.status, getString(R.string.disconnect))
        btn_connect.text = getString(R.string.connect)
        btn_connect.isEnabled = true
    }

    private fun onConnecting(id: String) {
        tv_status.text = getString(R.string.connecting)
        btn_connect.isEnabled = false
    }

    @KtorExperimentalAPI
    private fun onAccData(polarAccData: PolarAccelerometerData) {
        Log.d(
            LOG_TAG,
            "AccData: ${polarAccData.timeStamp} - (${polarAccData.samples.map { "${it.x}, ${it.y}, ${it.z} " }})"
        )
        runOnUiThread {
            tv_acc_value.text = getString(
                R.string.current_acc_value,
                "x: ${polarAccData.samples.last().x}, y: ${polarAccData.samples.last().y}, z: ${polarAccData.samples.last().z}"
            )
        }

        GlobalScope.launch {
            dataTransfer?.sendAccData(polarAccData)
        }
    }

    @KtorExperimentalAPI
    private fun onEcgData(ecgData: PolarEcgData) {
        Log.d(LOG_TAG, "ECG Data: ${ecgData.timeStamp} - (${ecgData.samples})")
        runOnUiThread {
            tv_ecg_value.text = getString(
                R.string.current_acc_value,
                "x: ${ecgData.samples.last()}"
            )
        }

        GlobalScope.launch {
            dataTransfer?.sendEcgData(ecgData)
        }
    }

    @KtorExperimentalAPI
    private fun onHrData(hrData: PolarHrData) {
        tv_hr.text = getString(R.string.hr, hrData.hr.toString());
        GlobalScope.launch {
            dataTransfer?.sendHrData(hrData)
        }
    }

    override fun onPause() {
        PolarDatReceiver.onPause()
        super.onPause()
    }

    override fun onResume() {
        PolarDatReceiver.onResume()
        super.onResume()
    }

    override fun onStop() {
        PolarDatReceiver.dispose()
        super.onStop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(LOG_TAG, "permission result: $grantResults")
    }

}
