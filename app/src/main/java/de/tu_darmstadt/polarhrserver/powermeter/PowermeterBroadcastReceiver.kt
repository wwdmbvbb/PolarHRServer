package de.tu_darmstadt.polarhrserver.powermeter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Created by Marcel Zickler on 18.05.2021.
 */
/*class PowermeterBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        when (action){
            ACTION_GATT_CONNECTED -> {
                connected = true
                updateConnectionState(R.string.connected)
                (context as? Activity)?.invalidateOptionsMenu()
            }
            ACTION_GATT_DISCONNECTED -> {
                connected = false
                updateConnectionState(R.string.disconnected)
                (context as? Activity)?.invalidateOptionsMenu()
                clearUI()
            }
            ACTION_GATT_SERVICES_DISCOVERED -> {
                // Show all the supported services and characteristics on the
                // user interface.
                displayGattServices(bluetoothLeService.getSupportedGattServices())
            }
            ACTION_DATA_AVAILABLE -> {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
            }
        }
    }
}*/