import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresApi

class PowermeterSearch(
    private val bluetoothManager: BluetoothManager?,
    private val name: String,
    private val onPowermeterFound: (device: BluetoothDevice) -> Unit,
    private val onScanStopped: () -> Unit
) {

    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter;
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private var scanning = false
    private val handler = Handler()
    private val SCAN_PERIOD: Long = 10000

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (result.isConnectable && result.device.name == name) {
                bluetoothLeScanner!!.stopScan(this)
                scanning = false;
                Log.d("Powermeter", "Connected!");
                onPowermeterFound(result.device);
            }
            Log.d(
                "Powermeter",
                "Found device with name ${result.device.name}, connectable: ${result.isConnectable}, rssi: ${result.rssi}, advertisingSid: ${result.advertisingSid}"
            );
        }
    }

    public fun scanDevices() {
        bluetoothLeScanner?.let { scanner ->
            if (!scanning) { // Stops scanning after a pre-defined scan period.
                handler.postDelayed({
                    scanning = false
                    scanner.stopScan(leScanCallback)
                    onScanStopped();
                }, SCAN_PERIOD)
                scanning = true
                scanner.startScan(leScanCallback)
            } else {
                scanning = false
                scanner.stopScan(leScanCallback)
                onScanStopped()
            }
        }
    }

}