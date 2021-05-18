package de.tu_darmstadt.polarhrserver

import android.util.Log
import com.androidcommunications.polar.api.ble.model.gatt.client.BlePMDClient
import de.tu_darmstadt.polarhrserver.powermeter.PowermeterData
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import polar.com.sdk.api.model.PolarAccelerometerData
import polar.com.sdk.api.model.PolarEcgData
import polar.com.sdk.api.model.PolarHrData
import java.net.InetSocketAddress

@KtorExperimentalAPI
class DataTransfer(private val serverAddress: String, private val serverPort: Int = 9090) {

    private val destAddr = InetSocketAddress(serverAddress, serverPort);
    private val socket =
        aSocket(ActorSelectorManager(Dispatchers.IO)).udp().bind(InetSocketAddress("0.0.0.0", 5678))

    init {
        Log.d(LOG_TAG, "Started UDP client at ${socket.localAddress}")
        GlobalScope.launch {
            sendTestData()
        }
    }

    private suspend fun sendTestData(){
        try {
            val buf = BytePacketBuilder();
            buf.writeText("Hello there, I'm using Polar HR Monitor");
            socket.outgoing.send(Datagram(buf.build(), destAddr))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    suspend fun sendAccData(accData: PolarAccelerometerData) {
        try {
            val buf = BytePacketBuilder();
            buf.writeText(accData.toUdpString());
            socket.outgoing.send(Datagram(buf.build(), destAddr))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    suspend fun sendEcgData(ecgData: PolarEcgData) {
        try {
            val buf = BytePacketBuilder();
            buf.writeText(ecgData.toUdpString());
            socket.outgoing.send(Datagram(buf.build(), destAddr))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    suspend fun sendHrData(hrData: PolarHrData) {
        try {
            val buf = BytePacketBuilder();
            buf.writeText(hrData.toUdpString());
            socket.outgoing.send(Datagram(buf.build(), destAddr))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    suspend fun sendPowermeterData(data: PowermeterData){
        try {
            val buf = BytePacketBuilder();
            buf.writeText(data.toUdpString());
            socket.outgoing.send(Datagram(buf.build(), destAddr))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    public fun dispose(){
        socket.dispose()
    }

}