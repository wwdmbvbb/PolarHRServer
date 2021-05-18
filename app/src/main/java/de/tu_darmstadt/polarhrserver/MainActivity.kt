package de.tu_darmstadt.polarhrserver

import PowermeterSearch
import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import de.tu_darmstadt.polarhrserver.powermeter.PowermeterData
import de.tu_darmstadt.polarhrserver.powermeter.PowermeterGattCallback
import de.tu_darmstadt.polarhrserver.powermeter.PowermeterGattManager
import io.ktor.util.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import polar.com.sdk.api.model.PolarAccelerometerData
import polar.com.sdk.api.model.PolarEcgData
import polar.com.sdk.api.model.PolarHrData
import polar.com.sdk.api.model.PolarSensorSetting


private const val PREVIOUS_CONNECTIONS_KEY = "previous_connections";
private const val CLEAN_PREFS = false;
private const val POWERMETER_NAME = "Stages 21218";

class MainActivity : AppCompatActivity() {

    @KtorExperimentalAPI
    var dataTransfer: DataTransfer? = null

    private var previousConnectionsList = listOf<ConnectionData>();

    private lateinit var powermeterSearch: PowermeterSearch

    @KtorExperimentalAPI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (CLEAN_PREFS) {
            val settings = PreferenceManager.getDefaultSharedPreferences(this);
            val editor = settings.edit()
            editor.clear()
            editor.apply()
        }


        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ), 1
        )

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        PolarDatReceiver.init(
            this::onEcgData,
            this::onAccData,
            this::onConnected,
            this::onDisconnected,
            this::onConnecting,
            this::onHrData,
            this::onInitAccSettings,
            this::onInitEcgSettings,
        )

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fillPreviousConnections(init = true)

        powermeterSearch = PowermeterSearch(getSystemService(BluetoothManager::class.java), POWERMETER_NAME, this::onPowermeterFound)
    }

    private fun onInitAccSettings(settingsChoice: PolarSensorSetting): PolarSensorSetting =
        onInitPolarSettings(settingsChoice, "ACC")


    private fun onInitEcgSettings(settingsChoice: PolarSensorSetting): PolarSensorSetting =
        onInitPolarSettings(settingsChoice, "ECG")


    private fun onInitPolarSettings(
        settingsChoice: PolarSensorSetting,
        name: String
    ): PolarSensorSetting {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this);
        val editor = prefs.edit()
        val key = "${name}_AVAILABLE_TYPES"
        val value: Set<String> = settingsChoice.settings.keys.map { it.toString() }.toSet()

        val availableTypes = prefs.getStringSet(key, setOf())
        if (availableTypes != value) {
            editor.putStringSet(key, value)
            settingsChoice.settings.forEach { type, choices ->
                editor.putStringSet(
                    "${name}_AVAILABLE_$type",
                    choices.map { it.toString() }.toSet()
                )
            }
            editor.apply()
        } else {
            val settings = mutableMapOf<PolarSensorSetting.SettingType, Int>()

            settingsChoice.settings.forEach { type, choices ->
                settings[type] = prefs.getString("${name}_${type}", null)?.toIntOrNull() ?: -1
            }

            if (!settings.values.contains(-1)) {
                return PolarSensorSetting(settings)
            }
        }

        return settingsChoice.maxSettings()
    }


    @KtorExperimentalAPI
    private fun fillPreviousConnections(init: Boolean = false) {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val previousConnections = settings.getString(PREVIOUS_CONNECTIONS_KEY, null)?.split(";");

        if (previousConnections.isNullOrEmpty()) return;

        previousConnectionsList = previousConnections.map {
            val parts = it.split(":")
            if (parts.size < 2) throw IllegalArgumentException("IP Address has to have Format address:port")

            ConnectionData(
                it
            ) { view ->
                et_server.setText(parts[0])
                et_server_port.setText(parts[1])
                onSendDataClicked(view)
            }
        }

        if (init) {
            list_previous_connections.adapter = PreviousConnectionsListAdapter(
                previousConnectionsList,
                layoutInflater
            )
        } else {
            val adapter = list_previous_connections.adapter as PreviousConnectionsListAdapter
            adapter.data = previousConnectionsList;
        }

    }

    private fun saveConnection(ip: String, port: Int) {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val previousConnections =
            settings.getString(PREVIOUS_CONNECTIONS_KEY, null)?.split(";") ?: listOf()

        val newConnectionList = previousConnections.toMutableList()
        val newElement = "$ip:$port";
        newConnectionList.remove(newElement)
        newConnectionList.add(0, newElement);
        while (newConnectionList.size > 5) {
            newConnectionList.removeLast()
        }

        val editor = settings.edit()
        editor.putString(PREVIOUS_CONNECTIONS_KEY, newConnectionList.joinToString(";"))
        editor.apply()
    }

    fun onConnectClicked(view: View) {
        PolarDatReceiver.connect(this)
    }

    fun onPowermeterConnectClicked(view: View) {
        powermeterSearch.scanDevices();
    }

    private fun onPowermeterFound(device: BluetoothDevice){
        val gatt = device.connectGatt(applicationContext, true, PowermeterGattCallback(this::onPowermeterData))
        val gattManager = PowermeterGattManager(gatt)
    }

    @KtorExperimentalAPI
    private fun onPowermeterData(data: PowermeterData){
        //TODO UI
        /*Log.d(
            LOG_TAG,
            "AccData: ${polarAccData.timeStamp} - (${polarAccData.samples.map { "${it.x}, ${it.y}, ${it.z} " }})"
        )
        runOnUiThread {
            tv_acc_value.text = getString(
                R.string.current_acc_value,
                "x: ${polarAccData.samples.last().x}, y: ${polarAccData.samples.last().y}, z: ${polarAccData.samples.last().z}"
            )
        }*/

        GlobalScope.launch {
            dataTransfer?.sendPowermeterData(data)
        }
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

            saveConnection(ip, port)
            fillPreviousConnections()

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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean = when (item?.itemId) {
        R.id.action_settings -> {
            val intent = Intent(this, SettingsActivity::class.java).apply {
                // putExtra(EXTRA_MESSAGE, message)
                // flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

}
