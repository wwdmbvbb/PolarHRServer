package de.tu_darmstadt.polarhrserver

import java.net.DatagramSocket
import kotlinx.coroutines.*

class DataTransfer(private val port: Int = 6789) {

    private val socket: DatagramSocket = DatagramSocket(port)

    suspend fun sendData() {

    }

}