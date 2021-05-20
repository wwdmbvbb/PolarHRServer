package de.tu_darmstadt.polarhrserver.powermeter

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile.*
import android.util.Log
import java.util.*


private fun convertFromInteger(i: Int): UUID {
    val MSB = 0x0000000000001000L
    val LSB = -0x7fffff7fa064cb05L
    val value = (i and -0x1).toLong()
    return UUID(MSB or (value shl 32), LSB)
}

data class PowermeterData(
    val instantaneousPower: Int,
    val accumulatedTorque: Float,
    val crankRevolutions: Int,
    val lastCrankEventTime: Float
) {
    fun toUdpString(): String {
        return "POW:$instantaneousPower;$accumulatedTorque;$crankRevolutions;$lastCrankEventTime"
    }

    override fun toString(): String {
        return "PowermeterData(instantaneousPower=$instantaneousPower, accumulatedTorque=$accumulatedTorque, crankRevolutions=$crankRevolutions, lastCrankEventTime=$lastCrankEventTime)"
    }
}

/**
 * Created by Marcel Zickler on 18.05.2021.
 */
class PowermeterGattCallback(val onPowermeterData: (data: PowermeterData) -> Unit) :
    BluetoothGattCallback() {
    private val TAG = "PowermeterGattCallback";

    private val GENERIC_ATTRIBUTE_SERVICE_UUID = convertFromInteger(0x1801)
    private val DEVICE_INFO_SERVICE_UUID = convertFromInteger(0x180a)
    private val BATTERY_SERVICE_UUID = convertFromInteger(0x180F)
    private val CYCLING_POWER_SERVICE_UUID = convertFromInteger(0x1818)
    private val CYCLING_POWER_MEASUREMENT_CHAR = convertFromInteger(0x2a63)
    private val CYCLING_POWER_VECTOR_CHAR = convertFromInteger(0x2a64)
    private val CYCLING_POWER_FEATURE_CHAR = convertFromInteger(0x2a65)
    private val CYCLING_POWER_CONTROL_POINT_CHAR = convertFromInteger(0x2a66)
    private val CLIENT_CHARACTERISTIC_CONFIG_UUID = convertFromInteger(0x2902)


    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        if (newState == STATE_CONNECTED) {
            gatt?.discoverServices()
            Log.d(TAG, "connected")
        } else if (newState == STATE_DISCONNECTED) {
            Log.d(TAG, "disconnected")
        } else if (newState == STATE_CONNECTING) {
            Log.d(TAG, "connecting...")
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        if (gatt == null) return
        val services = gatt.services
        for (s in services) {
            if (s.uuid == CYCLING_POWER_SERVICE_UUID) {
                Log.d(TAG, "Found Cycling Power Service")
                for (c in s.characteristics) {
                    if (c.uuid == CYCLING_POWER_MEASUREMENT_CHAR) {
                        Log.d(TAG, "Found cycling power measurement characteristics")

                        gatt.setCharacteristicNotification(c, true)

                        val descriptor =
                            c.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                        Log.d(TAG, "descriptor: ${descriptor.uuid}")
                        val sucessSetValue =
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        val succesWrite = gatt.writeDescriptor(descriptor);

                        Log.d(TAG, "sucess setValue = $sucessSetValue, sucessWrite = $succesWrite")
                        //Log.d(TAG, "Descriptors: ${c.descriptors.map { it.uuid }}")
                        //gatt.setCharacteristicNotification(c, true)
                    } else if (c.uuid == CYCLING_POWER_VECTOR_CHAR) {
                        Log.d(TAG, "found cycling power vector characteristics")
                        Log.d(TAG, "Descriptors: ${c.descriptors.map { it.uuid }}")
                    } else if (c.uuid == CYCLING_POWER_CONTROL_POINT_CHAR) {
                        Log.d(TAG, "Found cycling power control point characteristics")
                        Log.d(TAG, "Descriptors: ${c.descriptors.map { it.uuid }}")
                    } else if (c.uuid == CYCLING_POWER_FEATURE_CHAR) {
                        Log.d(TAG, "Found cycling power feature characteristics")
                        Log.d(TAG, "Descriptors: ${c.descriptors.map { it.uuid }}")
                    } else if (c.uuid == CLIENT_CHARACTERISTIC_CONFIG_UUID) {
                        Log.d(TAG, "Found client characteristic config characteristics")
                        Log.d(TAG, "Descriptors: ${c.descriptors.map { it.uuid }}")
                    } else {
                        Log.d(TAG, "Found unknown/irrelevant characteristic with UUID ${s.uuid}")
                    }
                }
            } else if (s.uuid == GENERIC_ATTRIBUTE_SERVICE_UUID) {
                Log.d(TAG, "Found generic info service")
            } else if (s.uuid == BATTERY_SERVICE_UUID) {
                Log.d(TAG, "Found battery service")
            } else if (s.uuid == DEVICE_INFO_SERVICE_UUID) {
                Log.d(TAG, "Found battery service")
            } else {
                Log.d(TAG, "Found unknown/irrelevant service with UUID ${s.uuid}")
            }
        }

    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        c: BluetoothGattCharacteristic?
    ) {

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (c == null) return
        when (c.uuid) {
            CYCLING_POWER_MEASUREMENT_CHAR -> {
                //Log.d(TAG, "cycling power measurement characteristics changed: ${c.value}")
                onCyclingPowerMeasurementCharacteristicsChanged(c)
            }
            CYCLING_POWER_VECTOR_CHAR -> {
                Log.d(TAG, "cycling power vector characteristics changed: ${c.value}")
            }
            CYCLING_POWER_CONTROL_POINT_CHAR -> {
                Log.d(TAG, "cycling power control point characteristics changed: ${c.value}")
            }
            CYCLING_POWER_FEATURE_CHAR -> {
                Log.d(TAG, "cycling power feature characteristics changed: ${c.value}")
            }
            else -> {
                Log.d(TAG, "characteristic ${c.uuid} changed: ${c.value}")
            }
        }
    }

    private fun onCyclingPowerMeasurementCharacteristicsChanged(c: BluetoothGattCharacteristic) {
        // For all other profiles, writes the data formatted in HEX.
        val data: ByteArray = c.value
        if (data.size < 11) return;

        /*TODO: one could read this, but for the stages powermeter we have 00101111 00000000, which
        means we have Accumulated Torque Present and Crank Revolution Data Present. The rest is not
        interesting for this use case*/
        val flags = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)
        val instantaneousPower = c.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 2) //Watt
        val accumulatedTorque =
            c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 5) //(1/32) Nm
        val cumulativeCrankRevs = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 7) //#
        val lastCrankEventTime =
            c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 9) // (1/1024) seconds

        onPowermeterData(
            PowermeterData(
                instantaneousPower,
                accumulatedTorque / 32f,
                cumulativeCrankRevs,
                lastCrankEventTime / 1024f
            )
        )
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "successfully wrote char")
        } else {
            Log.d(TAG, "failed writing char")
        }
    }

}
